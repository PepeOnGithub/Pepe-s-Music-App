package xyz.pepe.glacierclient.musicapp.domain.util

object Durations {
  /** Formats a duration in milliseconds as m:ss or h:mm:ss. Returns "--:--" for unknown/negative. */
  fun format(ms: Long?): String {
    if (ms == null || ms < 0) return "--:--"
    val totalSeconds = ms / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
  }

  /** Formats the remaining time as "-m:ss". */
  fun formatRemaining(positionMs: Long, durationMs: Long?): String {
    if (durationMs == null || durationMs < 0) return "--:--"
    val remaining = (durationMs - positionMs).coerceAtLeast(0)
    return "-" + format(remaining)
  }
}
