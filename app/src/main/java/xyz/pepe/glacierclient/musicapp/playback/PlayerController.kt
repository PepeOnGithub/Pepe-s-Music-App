package xyz.pepe.glacierclient.musicapp.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.launch
import xyz.pepe.glacierclient.musicapp.data.repository.CatalogRepository
import xyz.pepe.glacierclient.musicapp.data.repository.PlaybackStateRepository
import xyz.pepe.glacierclient.musicapp.domain.model.RepeatMode
import xyz.pepe.glacierclient.musicapp.domain.model.Track
import xyz.pepe.glacierclient.musicapp.domain.repository.SettingsRepository
import xyz.pepe.glacierclient.musicapp.domain.util.QueueOps
import xyz.pepe.glacierclient.musicapp.domain.util.SleepTimer

/** App-side wrapper around a [MediaController], exposing playback as a [StateFlow]. */
class PlayerController(
  private val context: Context,
  private val catalog: CatalogRepository,
  private val resumeStore: PlaybackStateRepository,
  private val settings: SettingsRepository,
  private val history: xyz.pepe.glacierclient.musicapp.data.repository.HistoryRepository? = null,
  private val streaks: xyz.pepe.glacierclient.musicapp.data.repository.StreakRepository? = null,
  private val stats: xyz.pepe.glacierclient.musicapp.data.repository.ListeningStatsRepository? = null,
) {
  private var pendingListenMs = 0L
  private val scope = CoroutineScope(Dispatchers.Main.immediate)
  private var controller: MediaController? = null
  private var positionJob: Job? = null
  private var sleepTimerJob: Job? = null
  private var saveTickCounter = 0
  // Bumped on every play(); the background "up next" resolver stops as soon as it's stale. It used
  // to compare the queue's first key instead, so reordering/removing the first item killed it.
  private var playSession = 0
  // Pre-shuffle order, so turning shuffle off actually restores it (it used to just flip the flag).
  private var unshuffledOrder: List<String>? = null
  // MediaController connects asynchronously; a tap during app start used to hit `controller?.…`
  // while still null and silently do nothing.
  private val controllerReady = kotlinx.coroutines.CompletableDeferred<MediaController>()
  private var retriedKey: String? = null
  private var fadeJob: Job? = null

  private val _state = MutableStateFlow(PlayerState())
  val state: StateFlow<PlayerState> = _state

  // Whether there's a track to show a mini player for. Screens that only need to know this — not
  // the live position, which changes 4x/second — should collect this instead of [state], so they
  // don't recompose on every position tick just because they're rendered while something plays.
  val hasTrack: StateFlow<Boolean> = _state
    .map { it.current != null }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, false)

  // One-off error messages (couldn't play, playback error) — a SharedFlow rather than part of
  // PlayerState so showing a snackbar for one doesn't require collecting (and recomposing on)
  // the fast-changing position state too.
  private val _errorEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
  val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

  // The android.media.audiofx.Equalizer itself lives in PlaybackService (it has to be attached to
  // the player's own audio session, in that process) — this is the UI-side mirror of its state,
  // kept in sync via session custom commands since MediaController has no built-in EQ support.
  private val _eq = MutableStateFlow(EqState())
  val eq: StateFlow<EqState> = _eq

  init {
    val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
    val future = MediaController.Builder(context, token).buildAsync()
    future.addListener({
      val c = runCatching { future.get() }.getOrNull() ?: return@addListener
      controller = c
      c.addListener(playerListener)
      controllerReady.complete(c)
      refreshEqDescribe()
    }, MoreExecutors.directExecutor())

    // Show the configured default speed from the start (it was always 1.0x in the UI until the
    // first play(), even with a different default).
    _state.value = _state.value.copy(speed = settings.settings.value.defaultSpeed)

    // Keep the Equalizer page's mirror in step with the saved settings (reset / import included).
    scope.launch {
      settings.settings.map { it.eqEnabled to it.eqPreset }.distinctUntilChanged().collect { (on, preset) ->
        _eq.value = _eq.value.copy(enabled = on, preset = preset)
      }
    }

    if (settings.settings.value.resumePlayback) {
      resumeStore.load()?.let { saved ->
        _state.value = _state.value.copy(
          queue = saved.queue, index = saved.index, positionMs = saved.positionMs,
          durationMs = saved.queue.getOrNull(saved.index)?.durationMs ?: 0,
          restorePending = true,
        )
      }
    }

    // Shake to skip. The accelerometer is only registered while the setting is on AND something is
    // playing — it used to stay registered for the whole process lifetime (setting off or not),
    // sampling at UI rate forever, which is a constant battery drain.
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? android.hardware.SensorManager
    val accelerometer = sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
    if (sensorManager != null && accelerometer != null) {
      val shakeDetector = ShakeDetector { if (_state.value.isPlaying) next() }
      scope.launch {
        combine(settings.settings.map { it.shakeToSkip }, _state.map { it.isPlaying }) { on, playing -> on && playing }
          .distinctUntilChanged()
          .collect { active ->
            if (active) sensorManager.registerListener(shakeDetector, accelerometer, android.hardware.SensorManager.SENSOR_DELAY_UI)
            else sensorManager.unregisterListener(shakeDetector)
          }
      }
    }
  }

  private val playerListener = object : Player.Listener {
    override fun onIsPlayingChanged(isPlaying: Boolean) {
      _state.value = _state.value.copy(isPlaying = isPlaying)
      if (isPlaying) startPositionLoop() else {
        positionJob?.cancel(); persistState()
        if (pendingListenMs > 0) { stats?.add(pendingListenMs); pendingListenMs = 0 }
      }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
      _state.value = _state.value.copy(
        isBuffering = playbackState == Player.STATE_BUFFERING,
        durationMs = controller?.duration?.coerceAtLeast(0) ?: 0,
      )
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
      val idx = controller?.currentMediaItemIndex ?: -1
      // Sleep timer "end of track": the previous track just finished on its own, so stop here.
      if (_state.value.sleepAtTrackEnd && (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT)) {
        controller?.pause()
        _state.value = _state.value.copy(sleepAtTrackEnd = false)
      }
      // A–B loop points are per-track — carrying them into the next song would silently trap
      // playback in a loop the user never set on it.
      _state.value = _state.value.copy(index = idx, loopA = null, loopB = null)
      retriedKey = null
      // Every track start (manual play, skip, or auto-advance) — not just the very first one —
      // gets the same Fade In treatment; otherwise track 2+ in a queue would jump straight to
      // full volume regardless of the setting.
      fadeJob?.cancel()
      if (sleepTimerJob?.isActive == true) { /* the sleep timer owns volume while it runs */ }
      else if (settings.settings.value.fadeInOnPlay) { controller?.volume = 0f; fadeInVolume() }
      else controller?.volume = 1f
      persistState()
    }

    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
      // Most real failures are an expired/blocked stream URL (YouTube's expire after hours, so any
      // long pause or old queue hits this). Re-resolve once and resume where we were; if that
      // fails, skip to the next track instead of leaving the whole queue dead.
      val s = _state.value
      val track = s.current
      if (track == null) {
        val msg = error.message ?: "Playback error"
        _state.value = s.copy(error = msg)
        _errorEvents.tryEmit(msg)
        return
      }
      if (retriedKey == track.key) { skipAfterError(track); return }
      retriedKey = track.key
      val at = s.positionMs
      scope.launch {
        catalog.invalidate(track)
        val url = catalog.resolveStreamUrl(track)
        val c = controller ?: return@launch
        val idx = c.currentMediaItemIndex
        if (!url.isNullOrBlank() && idx in 0 until c.mediaItemCount) {
          c.replaceMediaItem(idx, track.copy(streamUrl = url).toMediaItem())
          c.seekTo(idx, at); c.prepare(); c.play()
        } else skipAfterError(track)
      }
    }
  }

  private fun skipAfterError(track: Track) {
    val msg = "Couldn't play \"${track.title}\" — skipped"
    _state.value = _state.value.copy(error = msg)
    _errorEvents.tryEmit(msg)
    val c = controller ?: return
    if (c.hasNextMediaItem()) { c.seekToNextMediaItem(); c.prepare(); c.play() }
  }

  private fun startPositionLoop() {
    positionJob?.cancel()
    positionJob = scope.launch {
      while (true) {
        controller?.let { c ->
          val pos = c.currentPosition.coerceAtLeast(0)
          val s = _state.value
          val loopB = s.loopB
          if (loopB != null && pos >= loopB) {
            val target = s.loopA ?: 0
            c.seekTo(target)
            _state.value = s.copy(positionMs = target)
          } else {
            _state.value = s.copy(positionMs = pos)
            if (settings.settings.value.fadeOutOnEnd && s.durationMs > 0) {
              val remaining = s.durationMs - pos
              if (remaining in 0..FADE_MS) c.volume = (remaining / FADE_MS.toFloat()).coerceIn(0f, 1f)
            }
          }
        }
        saveTickCounter++
        if (!settings.settings.value.privateSession) pendingListenMs += if (settings.settings.value.batterySaver) 1000 else 250
        if (saveTickCounter % 20 == 0) {
          persistState()
          if (pendingListenMs > 0) { stats?.add(pendingListenMs); pendingListenMs = 0 }
        }
        // Battery saver: a coarser position tick is the one broadly-real, safe lever this app has
        // over its own battery use (less wakeups, less recomposition) — not a fake toggle.
        delay(if (settings.settings.value.batterySaver) 1000 else 250)
      }
    }
  }

  private fun persistState() {
    val s = _state.value
    if (s.queue.isNotEmpty()) resumeStore.save(s.queue, s.index, s.positionMs)
  }

  /** Resolves stream URLs (network) then plays [tracks] starting at [startIndex]. */
  fun play(tracks: List<Track>, startIndex: Int, startPositionMs: Long = 0, applyDefaults: Boolean = true) {
    if (tracks.isEmpty()) return // coerceIn(0, -1) below used to throw on an empty list
    val session = ++playSession
    scope.launch {
      val start = startIndex.coerceIn(0, tracks.lastIndex)
      val target = tracks[start]
      // Show the tapped song (with a loading indicator) right away instead of leaving the old one
      // on screen while its stream resolves.
      controller?.pause()
      _state.value = _state.value.copy(queue = listOf(target), index = 0, isBuffering = true, isPlaying = false, positionMs = 0, durationMs = 0, error = null)
      // Always go through the catalog: it re-resolves expiring YouTube URLs (including ones stored
      // in Favorites/History) and returns every other source's own URL unchanged.
      suspend fun resolve(t: Track) = t.copy(streamUrl = catalog.resolveStreamUrl(t))
      // Resolve only the tapped track before starting — resolving a whole results list up front
      // (one network round-trip per track) made taps look dead for many seconds.
      val first = resolve(target)
      if (session != playSession) return@launch
      if (first.streamUrl.isNullOrBlank()) {
        val msg = if (settings.settings.value.offlineMode) "\"${target.title}\" isn't downloaded — Offline mode is on"
        else "Couldn't play \"${target.title}\" — no stream available"
        _state.value = _state.value.copy(error = msg, restorePending = false, isBuffering = false)
        _errorEvents.tryEmit(msg)
        return@launch
      }
      // Private session — a real, honored toggle: nothing gets written to Recently Played, Most
      // Played, or the listening streak while it's on. Recorded only once the track can actually
      // play, so failed taps no longer inflate play counts and minutes listened.
      if (applyDefaults && !settings.settings.value.privateSession) {
        history?.record(target)
        streaks?.recordToday()
      }

      val prefs = settings.settings.value
      // New queues start at the default speed; resuming the saved queue keeps whatever is current.
      val speed = if (applyDefaults) prefs.defaultSpeed else _state.value.speed
      var shuffle = _state.value.shuffle
      var repeat = _state.value.repeat
      var rest = tracks.drop(start + 1)
      if (applyDefaults) {
        repeat = prefs.defaultRepeat
        shuffle = prefs.shuffleOnPlay
        // Shuffle the not-yet-resolved remainder. This used to shuffle `listOf(first)` — a
        // one-item list — so "shuffle by default" never changed the order of anything.
        if (shuffle) rest = rest.shuffled(kotlin.random.Random(System.currentTimeMillis()))
        unshuffledOrder = if (shuffle) tracks.drop(start).map { it.key } else null
      }

      _state.value = _state.value.copy(queue = listOf(first), index = 0, error = null, restorePending = false, shuffle = shuffle, repeat = repeat, speed = speed)
      val c = controllerReady.await()
      if (session != playSession) return@launch
      c.apply {
        setPlaybackSpeed(speed)
        repeatMode = repeat.toPlayer()
        setMediaItems(listOf(first.toMediaItem()), 0, startPositionMs)
        prepare()
        play()
      }
      // Fill "up next" in the background, in order, skipping anything that won't resolve.
      // Resolve up to 4 at a time (results still appended in order, fewer under Battery Saver);
      // the first of these doubles as a prefetch of the next song, so skipping forward is instant.
      val gate = kotlinx.coroutines.sync.Semaphore(if (prefs.batterySaver) 1 else 4)
      val pending = rest.map { t -> async { gate.withPermit { resolve(t) } } }
      for (d in pending) {
        val r = d.await()
        if (session != playSession) { pending.forEach { it.cancel() }; break } // user started something else
        if (r.streamUrl.isNullOrBlank()) continue
        _state.value = _state.value.copy(queue = _state.value.queue + r)
        c.addMediaItem(r.toMediaItem())
      }
    }
  }

  private fun RepeatMode.toPlayer() = when (this) {
    RepeatMode.OFF -> Player.REPEAT_MODE_OFF
    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
  }

  /** Searches every enabled source for [query] and plays the results — the ecosystem's
   * `pepe://music/play?q=` entry point (Weather's "music for this weather", Clock, etc.). */
  fun playQuery(query: String) {
    scope.launch {
      val results = catalog.search(query, settings.settings.value)
      if (results.isEmpty()) { _errorEvents.tryEmit("Nothing found for \"$query\""); return@launch }
      play(results.take(25), 0)
    }
  }

  /** Resumes the queue restored from disk at app start, if any is pending. */
  fun resumeIfPending() {
    val s = _state.value
    if (!s.restorePending || s.queue.isEmpty()) return
    play(s.queue, s.index, s.positionMs, applyDefaults = false)
  }

  fun togglePlayPause() {
    if (_state.value.restorePending) { resumeIfPending(); return }
    val c = controller ?: return
    if (c.isPlaying) c.pause() else c.play()
  }

  fun next() {
    val s = _state.value
    // Repeat-One must still let the user skip by hand — QueueOps returns the same index for ONE
    // (right for auto-advance), which made Next/Previous dead buttons in that mode.
    val nextIndex = QueueOps.next(s.index, s.queue.size, s.repeat.manual()) ?: return
    controller?.seekTo(nextIndex, 0)
  }

  fun previous() {
    val s = _state.value
    val prevIndex = QueueOps.previous(s.index, s.positionMs, s.queue.size, s.repeat.manual())
    controller?.seekTo(prevIndex, 0)
  }

  private fun RepeatMode.manual() = if (this == RepeatMode.ONE) RepeatMode.ALL else this

  fun seekBy(deltaMs: Long) {
    val target = (_state.value.positionMs + deltaMs).coerceIn(0, _state.value.durationMs.coerceAtLeast(0))
    seekTo(target)
  }

  fun seekTo(positionMs: Long) {
    // Update state immediately — otherwise the slider snaps back to the stale pre-seek position
    // for up to one 250ms tick of the position loop before catching up, which looks like the
    // scrubber "resetting" right after you let go.
    controller?.seekTo(positionMs)
    // Fade Out lowers volume near the end; seeking back out of that window used to leave the rest
    // of the song quiet. (The sleep timer owns volume while it runs.)
    if (sleepTimerJob?.isActive != true) controller?.volume = 1f
    _state.value = _state.value.copy(positionMs = positionMs)
  }

  fun toggleShuffle() {
    val s = _state.value
    if (s.queue.isEmpty()) { _state.value = s.copy(shuffle = !s.shuffle); return }
    val c = controller
    val pos = c?.currentPosition ?: s.positionMs
    if (!s.shuffle) {
      unshuffledOrder = s.queue.map { it.key }
      val shuffled = QueueOps.shuffle(s.queue, s.index.coerceIn(0, s.queue.lastIndex), seed = System.currentTimeMillis())
      _state.value = s.copy(queue = shuffled, index = 0, shuffle = true)
      c?.setMediaItems(shuffled.map { it.toMediaItem() }, 0, pos)
    } else {
      // Restore the original order (tracks added while shuffled keep their place at the end).
      val order = unshuffledOrder.orEmpty().withIndex().associate { (i, k) -> k to i }
      val restored = s.queue.sortedBy { order[it.key] ?: Int.MAX_VALUE }
      val newIndex = restored.indexOfFirst { it.key == s.current?.key }.coerceAtLeast(0)
      _state.value = s.copy(queue = restored, index = newIndex, shuffle = false)
      c?.setMediaItems(restored.map { it.toMediaItem() }, newIndex, pos)
      unshuffledOrder = null
    }
    persistState()
  }

  fun cycleRepeat() {
    val next = when (_state.value.repeat) { RepeatMode.OFF -> RepeatMode.ALL; RepeatMode.ALL -> RepeatMode.ONE; RepeatMode.ONE -> RepeatMode.OFF }
    _state.value = _state.value.copy(repeat = next)
    controller?.repeatMode = next.toPlayer()
  }

  fun setPlaybackSpeed(speed: Float) {
    controller?.playbackParameters = androidx.media3.common.PlaybackParameters(speed, _state.value.pitch)
    _state.value = _state.value.copy(speed = speed)
  }

  /** Independent pitch shift — `PlaybackParameters` takes speed and pitch as separate factors, so
   * this doesn't need a custom session command like the EQ does; `Player.setPlaybackParameters`
   * is on the common `Player` interface `MediaController` already implements. */
  fun setPitch(pitch: Float) {
    controller?.playbackParameters = androidx.media3.common.PlaybackParameters(_state.value.speed, pitch)
    _state.value = _state.value.copy(pitch = pitch)
  }

  /** Ramps volume 0→1 over [FADE_MS] instead of starting at full volume — Fade In from the roadmap. */
  private fun fadeInVolume() {
    fadeJob = scope.launch {
      val steps = 12
      for (i in 1..steps) {
        delay(FADE_MS / steps)
        controller?.volume = i / steps.toFloat()
      }
    }
  }

  private companion object { const val FADE_MS = 600L }

  // ---- A–B loop ----

  fun setLoopA() { _state.value = _state.value.copy(loopA = _state.value.positionMs) }
  fun setLoopB() { _state.value = _state.value.copy(loopB = _state.value.positionMs) }
  fun clearLoop() { _state.value = _state.value.copy(loopA = null, loopB = null) }

  // ---- Queue management ----

  /** Reorders the queue (drag-to-reorder in the Now Playing queue sheet). */
  fun moveQueueItem(from: Int, to: Int) {
    val s = _state.value
    if (from == to || from !in s.queue.indices || to !in s.queue.indices) return
    val (newQueue, newIndex) = QueueOps.move(s.queue, s.index, from, to)
    _state.value = s.copy(queue = newQueue, index = newIndex)
    controller?.moveMediaItem(from, to)
    persistState()
  }

  /** Removes a track from the queue. */
  fun removeFromQueue(index: Int) {
    val s = _state.value
    if (index !in s.queue.indices) return
    val (newQueue, newIndex) = QueueOps.remove(s.queue, s.index, index)
    _state.value = s.copy(queue = newQueue, index = newIndex)
    controller?.removeMediaItem(index)
    persistState()
  }

  /** Inserts [track] to play right after the current one. */
  fun playNext(track: Track) {
    scope.launch {
      val resolved = track.copy(streamUrl = catalog.resolveStreamUrl(track))
      if (resolved.streamUrl.isNullOrBlank()) { _errorEvents.tryEmit("Couldn't queue \"${track.title}\" — no stream available"); return@launch }
      // Nothing playing yet: "Play next"/"Add to queue" used to add to a controller with no
      // playlist, so nothing audible happened. Start playback instead.
      if (_state.value.queue.isEmpty()) { play(listOf(resolved), 0); return@launch }
      val s = _state.value
      val insertAt = (s.index + 1).coerceIn(0, s.queue.size)
      val newQueue = s.queue.toMutableList().apply { add(insertAt, resolved) }
      _state.value = s.copy(queue = newQueue)
      controller?.addMediaItem(insertAt, resolved.toMediaItem())
      persistState()
    }
  }

  /** Appends [track] to the end of the queue. */
  fun addToQueue(track: Track) {
    scope.launch {
      val resolved = track.copy(streamUrl = catalog.resolveStreamUrl(track))
      if (resolved.streamUrl.isNullOrBlank()) { _errorEvents.tryEmit("Couldn't queue \"${track.title}\" — no stream available"); return@launch }
      // Nothing playing yet: "Play next"/"Add to queue" used to add to a controller with no
      // playlist, so nothing audible happened. Start playback instead.
      if (_state.value.queue.isEmpty()) { play(listOf(resolved), 0); return@launch }
      val s = _state.value
      val newQueue = s.queue + resolved
      _state.value = s.copy(queue = newQueue)
      controller?.addMediaItem(resolved.toMediaItem())
      persistState()
    }
  }

  // ---- Sleep timer ----

  /** Starts (or replaces) a sleep timer that fades volume out and pauses after [minutes]. */
  fun setSleepTimer(minutes: Int) {
    sleepTimerJob?.cancel()
    val endAt = System.currentTimeMillis() + minutes * 60_000L
    _state.value = _state.value.copy(sleepTimerEndAtMs = endAt, sleepAtTrackEnd = false)
    sleepTimerJob = scope.launch {
      while (true) {
        val remaining = SleepTimer.remaining(System.currentTimeMillis(), endAt)
        controller?.volume = SleepTimer.fadeVolume(remaining, settings.settings.value.sleepFadeSeconds * 1000L)
        if (remaining <= 0) {
          controller?.pause()
          controller?.volume = 1f
          _state.value = _state.value.copy(sleepTimerEndAtMs = null)
          return@launch
        }
        delay(500)
      }
    }
  }

  fun cancelSleepTimer() {
    sleepTimerJob?.cancel()
    sleepTimerJob = null
    controller?.volume = 1f
    _state.value = _state.value.copy(sleepTimerEndAtMs = null, sleepAtTrackEnd = false)
  }

  /** Pauses playback when the current track finishes (replaces any minute-based timer). */
  fun sleepAtEndOfTrack() {
    sleepTimerJob?.cancel()
    sleepTimerJob = null
    controller?.volume = 1f
    _state.value = _state.value.copy(sleepTimerEndAtMs = null, sleepAtTrackEnd = true)
  }

  // ---- Radio and queue tools ----

  /** "Start radio": plays [seed] then a shuffled mix of similar tracks (same artist and title searches). */
  fun startRadio(seed: Track) {
    scope.launch {
      val prefs = settings.settings.value
      val byArtist = runCatching { catalog.search(seed.artist, prefs) }.getOrDefault(emptyList())
      val byTitle = runCatching { catalog.search("${seed.title} ${seed.artist} mix", prefs) }.getOrDefault(emptyList())
      val mix = (byArtist + byTitle).distinctBy { it.key }.filter { it.key != seed.key }
        .shuffled(kotlin.random.Random(System.currentTimeMillis())).take(30)
      if (mix.isEmpty()) { _errorEvents.tryEmit("No similar tracks found for \"${seed.title}\""); return@launch }
      play(listOf(seed) + mix, 0)
    }
  }

  /** Removes everything after the current track. */
  fun clearUpNext() {
    val s = _state.value
    if (s.queue.size <= s.index + 1 || s.index < 0) return
    val count = s.queue.size - (s.index + 1)
    _state.value = s.copy(queue = s.queue.take(s.index + 1))
    controller?.removeMediaItems(s.index + 1, s.index + 1 + count)
    persistState()
  }

  /** Shuffles only the tracks after the current one. */
  fun shuffleUpNext() {
    val s = _state.value
    if (s.index < 0 || s.queue.size <= s.index + 2) return
    val tail = s.queue.drop(s.index + 1).shuffled(kotlin.random.Random(System.currentTimeMillis()))
    _state.value = s.copy(queue = s.queue.take(s.index + 1) + tail)
    controller?.let { c ->
      c.removeMediaItems(s.index + 1, c.mediaItemCount)
      c.addMediaItems(tail.map { it.toMediaItem() })
    }
    persistState()
  }

  // ---- Equalizer ----

  private fun refreshEqDescribe() {
    val c = controller ?: return
    val future = c.sendCustomCommand(SessionCommand(PlaybackService.CMD_EQ_DESCRIBE, Bundle.EMPTY), Bundle.EMPTY)
    future.addListener({
      val extras = runCatching { future.get().extras }.getOrNull() ?: return@addListener
      _eq.value = EqState(
        supported = extras.getBoolean("supported"),
        presets = extras.getStringArray("presets")?.toList() ?: emptyList(),
        enabled = extras.getBoolean("enabled"),
        preset = extras.getInt("preset", -1),
      )
    }, MoreExecutors.directExecutor())
  }

  fun setEqEnabled(enabled: Boolean) {
    _eq.value = _eq.value.copy(enabled = enabled)
    controller?.sendCustomCommand(SessionCommand(PlaybackService.CMD_EQ_SET_ENABLED, Bundle.EMPTY), Bundle().apply { putBoolean("enabled", enabled) })
  }

  fun setEqPreset(preset: Int) {
    _eq.value = _eq.value.copy(preset = preset, enabled = true)
    controller?.sendCustomCommand(SessionCommand(PlaybackService.CMD_EQ_SET_PRESET, Bundle.EMPTY), Bundle().apply { putInt("preset", preset) })
  }
}

/** Mirrors [android.media.audiofx.Equalizer] state across the session boundary; see PlaybackService. */
data class EqState(
  val supported: Boolean = false,
  val presets: List<String> = emptyList(),
  val enabled: Boolean = false,
  val preset: Int = -1,
)
