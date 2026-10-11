package xyz.pepe.glacierclient.musicapp.domain.util

object SleepTimer {
  const val FADE_WINDOW_MS = 10_000L

  /** Remaining time until [targetAtMs], never negative. */
  fun remaining(nowMs: Long, targetAtMs: Long): Long = (targetAtMs - nowMs).coerceAtLeast(0)

  /** Volume multiplier during the fade-out window; 1f outside it, 0f at/after the target. */
  fun fadeVolume(remainingMs: Long, fadeWindowMs: Long = FADE_WINDOW_MS): Float {
    if (remainingMs <= 0) return 0f
    if (fadeWindowMs <= 0 || remainingMs >= fadeWindowMs) return 1f
    return remainingMs / fadeWindowMs.toFloat()
  }
}
