package xyz.pepe.glacierclient.musicapp.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import xyz.pepe.glacierclient.musicapp.MusicApplication
import xyz.pepe.glacierclient.musicapp.data.remote.BrowserFingerprint

/** Hosts the single app-wide [ExoPlayer] and [MediaSession] for background playback. */
class PlaybackService : MediaSessionService() {
  private lateinit var player: ExoPlayer
  private lateinit var mediaSession: MediaSession
  private var equalizer: Equalizer? = null
  private var bassBoost: BassBoost? = null
  private var loudness: LoudnessEnhancer? = null
  private var loudnessWanted = false
  // Cancelled in onDestroy — the settings collector used to outlive the service and keep poking a
  // released ExoPlayer (IllegalStateException) whenever a setting changed after playback stopped.
  private val serviceScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main.immediate)

  override fun onCreate() {
    super.onCreate()
    // Per-host headers, not global ones: the innertube API call resolving a YouTube Music stream
    // URL can succeed while the actual audio-byte fetch from *.googlevideo.com below still gets
    // bot-walled if it doesn't also look like a real browser (this used to send the app's own
    // "PepesMusic/1.0" UA with no other fingerprint at all, on every request regardless of
    // source). ccMixter needs its own Referer for a completely different reason — plain hotlink
    // protection, not bot detection — and sending either host's headers to the other, or to
    // Audius/Internet Archive, would be wrong, not just unnecessary.
    val okHttp = OkHttpClient.Builder()
      .addInterceptor { chain ->
        val request = chain.request()
        val extra = when {
          request.url.host.endsWith("googlevideo.com") -> BrowserFingerprint.STREAMING_HEADERS - "User-Agent"
          request.url.host == "ccmixter.org" -> mapOf("Referer" to "https://ccmixter.org/")
          else -> emptyMap()
        }
        val builder = request.newBuilder()
        extra.forEach { (k, v) -> builder.header(k, v) }
        chain.proceed(builder.build())
      }
      .build()
    val dataSourceFactory = OkHttpDataSource.Factory(okHttp)
      .setUserAgent(BrowserFingerprint.DESKTOP_USER_AGENT)
    player = ExoPlayer.Builder(this)
      .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
      // Start as soon as ~0.75s of audio is buffered (default waits for 2.5s) — audio is tiny,
      // so the rest arrives long before it's needed.
      .setLoadControl(
        androidx.media3.exoplayer.DefaultLoadControl.Builder()
          .setBufferDurationsMs(20_000, 60_000, 750, 1_500)
          .build(),
      )
      .setAudioAttributes(
        AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
        /* handleAudioFocus = */ true,
      )
      .setHandleAudioBecomingNoisy(true)
      .setWakeMode(C.WAKE_MODE_NETWORK)
      .build()
    player.addListener(object : androidx.media3.common.Player.Listener {
      // The audio session can change when playback (re)starts; effects must follow it.
      override fun onAudioSessionIdChanged(audioSessionId: Int) {
        loudness?.release(); loudness = null
        applyLoudness()
      }
    })
    mediaSession = MediaSession.Builder(this, player).setCallback(EqualizerSessionCallback()).build()
    // Skip Silence and Bass Boost don't need session custom commands like the EQ does — the
    // service runs in the app's own process, so it can just observe Settings directly and apply
    // both immediately whenever the user flips them.
    serviceScope.launch {
      (application as MusicApplication).container.settings.settings.collect { s ->
        player.skipSilenceEnabled = s.skipSilence
        player.setHandleAudioBecomingNoisy(s.pauseOnUnplug)
        loudnessWanted = s.loudnessBoost
        applyLoudness()
        runCatching {
          val bb = bassBoostOrNull() ?: return@runCatching
          bb.enabled = s.bassBoost > 0
          if (s.bassBoost > 0) bb.setStrength((s.bassBoost * 100).toShort())
        }
        applyEqualizer(s.eqEnabled, s.eqPreset, s.trebleBoost)
      }
    }
  }

  /** Loudness boost: a fixed +5 dB gain from the system LoudnessEnhancer, only while the setting is on. */
  private fun applyLoudness() {
    runCatching {
      if (!loudnessWanted && loudness == null) return
      val sessionId = player.audioSessionId
      if (sessionId == C.AUDIO_SESSION_ID_UNSET) return
      val le = loudness ?: LoudnessEnhancer(sessionId).also { loudness = it }
      le.setTargetGain(500)
      le.enabled = loudnessWanted
    }
  }

  // Last values pushed to the Equalizer effect, so unrelated settings changes don't re-apply (and
  // so re-reset) the band levels.
  private var appliedEq: Triple<Boolean, Int, Int>? = null

  /**
   * Single place that drives the device [Equalizer] from settings (so Reset/Import work too):
   * on when the EQ switch is on OR treble boost is raised; the preset is applied first, then
   * treble boost raises the top ~20% of bands on top of it. Treble back to 0 restores the preset.
   */
  private fun applyEqualizer(enabled: Boolean, preset: Int, treble: Int) {
    val key = Triple(enabled, preset, treble)
    if (key == appliedEq) return
    if (!enabled && treble == 0 && equalizer == null) { appliedEq = key; return } // never touched: don't create the effect
    val eq = equalizerOrNull() ?: return
    appliedEq = key
    runCatching {
      if (preset in 0 until eq.numberOfPresets) eq.usePreset(preset.toShort())
      else repeat(eq.numberOfBands.toInt()) { eq.setBandLevel(it.toShort(), 0) }
      applyTrebleBoost(eq, treble)
      eq.enabled = enabled || treble > 0
    }
  }

  /**
   * There's no dedicated "TrebleBoost" AudioEffect on Android the way there's a [BassBoost] one —
   * real treble boost is done by raising the highest band(s) of a real parametric equalizer, so
   * that's what this does, on top of whatever preset is active.
   */
  private fun applyTrebleBoost(eq: Equalizer, level: Int) {
    if (level <= 0) return
    val bandCount = eq.numberOfBands.toInt()
    if (bandCount <= 0) return
    val trebleBands = (bandCount - (bandCount / 5).coerceAtLeast(1) until bandCount)
    val maxLevel = eq.bandLevelRange.getOrNull(1) ?: return
    val gain = (maxLevel * (level / 10f)).toInt().toShort()
    trebleBands.forEach { band -> runCatching { eq.setBandLevel(band.toShort(), gain) } }
  }

  private fun bassBoostOrNull(): BassBoost? {
    bassBoost?.let { return it }
    val sessionId = player.audioSessionId
    if (sessionId == C.AUDIO_SESSION_ID_UNSET) return null
    return runCatching { BassBoost(0, sessionId).also { bassBoost = it } }.getOrNull()
  }

  /** Lazily attaches an [Equalizer] to the player's audio session; null if the device has none. */
  private fun equalizerOrNull(): Equalizer? {
    equalizer?.let { return it }
    val sessionId = player.audioSessionId
    if (sessionId == C.AUDIO_SESSION_ID_UNSET) return null
    return runCatching { Equalizer(0, sessionId).also { equalizer = it } }.getOrNull()
  }

  /**
   * A real, on-device 5-band-ish EQ via [android.media.audiofx.Equalizer] — this is Android's
   * standard audio-effect API, not something hand-rolled. Exposed to [PlayerController] as
   * session custom commands since the effect itself must live in this process (attached to the
   * player's own audio session), while the UI talks to the player only through a [MediaSession].
   */
  private inner class EqualizerSessionCallback : MediaSession.Callback {
    private var lastHeadsetPressAtMs = 0L

    // Double-press the headset/media button to skip (always on; independent of the Now Playing double-tap seek) — real players' standard behavior. Media3's
    // default handling already maps a single press to play/pause; this only intercepts to detect
    // a second press arriving inside the double-press window, and otherwise lets the default
    // handling run normally.
    override fun onMediaButtonEvent(session: MediaSession, controllerInfo: MediaSession.ControllerInfo, intent: android.content.Intent): Boolean {
      val keyEvent = intent.getParcelableExtra<android.view.KeyEvent>(android.content.Intent.EXTRA_KEY_EVENT)
      val isHeadsetKey = keyEvent?.keyCode == android.view.KeyEvent.KEYCODE_HEADSETHOOK || keyEvent?.keyCode == android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
      if (isHeadsetKey && keyEvent?.action == android.view.KeyEvent.ACTION_UP) {
        val now = android.os.SystemClock.elapsedRealtime()
        val isDouble = now - lastHeadsetPressAtMs < 400
        lastHeadsetPressAtMs = now
        if (isDouble) { player.seekToNextMediaItem(); return true }
      }
      return super.onMediaButtonEvent(session, controllerInfo, intent)
    }

    override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
      val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
        .add(SessionCommand(CMD_EQ_DESCRIBE, Bundle.EMPTY))
        .add(SessionCommand(CMD_EQ_SET_ENABLED, Bundle.EMPTY))
        .add(SessionCommand(CMD_EQ_SET_PRESET, Bundle.EMPTY))
        .build()
      return MediaSession.ConnectionResult.accept(commands, MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS)
    }

    override fun onCustomCommand(
      session: MediaSession,
      controller: MediaSession.ControllerInfo,
      customCommand: SessionCommand,
      args: Bundle,
    ): ListenableFuture<SessionResult> {
      val settings = (application as MusicApplication).container.settings
      return when (customCommand.customAction) {
        CMD_EQ_DESCRIBE -> {
          val eq = equalizerOrNull()
          val result = Bundle().apply {
            putStringArray("presets", eq?.let { e -> Array(e.numberOfPresets.toInt()) { e.getPresetName(it.toShort()) } } ?: emptyArray())
            putBoolean("supported", eq != null)
            putBoolean("enabled", settings.settings.value.eqEnabled)
            putInt("preset", settings.settings.value.eqPreset)
          }
          Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, result))
        }
        CMD_EQ_SET_ENABLED -> {
          val enabled = args.getBoolean("enabled")
          settings.update { it.copy(eqEnabled = enabled) } // the settings collector applies it
          Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
        CMD_EQ_SET_PRESET -> {
          val preset = args.getInt("preset")
          settings.update { it.copy(eqPreset = preset, eqEnabled = true) } // picking a preset turns the EQ on
          Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
        else -> super.onCustomCommand(session, controller, customCommand, args)
      }
    }
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

  override fun onDestroy() {
    serviceScope.cancel()
    equalizer?.release()
    bassBoost?.release()
    loudness?.release()
    mediaSession.release()
    player.release()
    super.onDestroy()
  }

  companion object {
    const val CMD_EQ_DESCRIBE = "eq.describe"
    const val CMD_EQ_SET_ENABLED = "eq.setEnabled"
    const val CMD_EQ_SET_PRESET = "eq.setPreset"
  }
}
