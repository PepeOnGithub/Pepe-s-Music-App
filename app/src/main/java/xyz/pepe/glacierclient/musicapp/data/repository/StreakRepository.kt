package xyz.pepe.glacierclient.musicapp.data.repository

import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Real listening-streak tracking: records the calendar date (device-local) whenever a track
 * starts, then counts back consecutive days from today. No fabricated numbers — a day with zero
 * plays breaks the streak just like it would on any other player. */
class StreakRepository(private val prefs: SharedPreferences) {
  private val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

  fun recordToday() {
    val today = fmt.format(Date())
    val days = load()
    if (today in days) return
    days.add(today)
    save(days)
  }

  /** Consecutive days up to and including today (or yesterday, if nothing's played yet today). */
  fun currentStreak(): Int {
    val days = load()
    if (days.isEmpty()) return 0
    var count = 0
    var cursor = Date()
    while (true) {
      val key = fmt.format(cursor)
      if (key in days) {
        count++
        cursor = Date(cursor.time - 86_400_000L)
      } else if (count == 0 && key == fmt.format(Date())) {
        // Today has no play yet — check yesterday before giving up, so the streak doesn't reset
        // to zero the moment the clock ticks past midnight before you've played anything today.
        cursor = Date(cursor.time - 86_400_000L)
        if (fmt.format(cursor) !in days) return 0
      } else {
        return count
      }
    }
  }

  private fun load(): MutableSet<String> = (prefs.getStringSet(KEY, emptySet()) ?: emptySet()).toMutableSet()
  private fun save(days: Set<String>) { prefs.edit().putStringSet(KEY, days).apply() }

  private companion object { const val KEY = "days" }
}
