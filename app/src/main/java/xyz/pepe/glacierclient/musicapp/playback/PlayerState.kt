package xyz.pepe.glacierclient.musicapp.playback

import xyz.pepe.glacierclient.musicapp.domain.model.RepeatMode
import xyz.pepe.glacierclient.musicapp.domain.model.Track

data class PlayerState(
  val queue: List<Track> = emptyList(),
  val index: Int = -1,
  val isPlaying: Boolean = false,
  val isBuffering: Boolean = false,
  val positionMs: Long = 0,
  val durationMs: Long = 0,
  val shuffle: Boolean = false,
  val speed: Float = 1f,
  val pitch: Float = 1f,
  val repeat: RepeatMode = RepeatMode.OFF,
  val error: String? = null,
  val sleepTimerEndAtMs: Long? = null,
  val restorePending: Boolean = false,
  /** Pause automatically when the current track finishes (sleep timer "end of track"). */
  val sleepAtTrackEnd: Boolean = false,
  /** A–B loop points on the current track, ms; both non-null means the position loop seeks back
   * to [loopA] once playback passes [loopB]. */
  val loopA: Long? = null,
  val loopB: Long? = null,
) {
  val current: Track? get() = queue.getOrNull(index)
}
