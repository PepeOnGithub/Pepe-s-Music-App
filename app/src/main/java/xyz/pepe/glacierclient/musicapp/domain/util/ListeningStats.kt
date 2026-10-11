package xyz.pepe.glacierclient.musicapp.domain.util

import java.time.LocalDate
import xyz.pepe.glacierclient.musicapp.domain.model.Track

/** Pure helpers behind the weekly listening chart and "Top artists". */
object ListeningStats {
  /** Day keys (yyyy-MM-dd), oldest first, ending at [today]. */
  fun lastDayKeys(today: LocalDate, days: Int): List<String> =
    (days - 1 downTo 0).map { today.minusDays(it.toLong()).toString() }

  /** Listening milliseconds for each of the last [days] days, oldest first; missing days are 0. */
  fun lastDays(totals: Map<String, Long>, today: LocalDate, days: Int = 7): List<Pair<LocalDate, Long>> =
    (days - 1 downTo 0).map { back ->
      val d = today.minusDays(back.toLong())
      d to (totals[d.toString()] ?: 0L)
    }

  /** Whole minutes across [lastDays]. */
  fun minutes(ms: Long): Long = ms / 60_000L

  /** Drops entries older than [keepDays] so the stored map stays small. */
  fun prune(totals: Map<String, Long>, today: LocalDate, keepDays: Int = 60): Map<String, Long> {
    val keep = lastDayKeys(today, keepDays).toSet()
    return totals.filterKeys { it in keep }
  }

  /** Artists ranked by plays; [counts] is keyed by [Track.key] and tracks without a count weigh 1. */
  fun topArtists(tracks: List<Track>, counts: Map<String, Int>, limit: Int = 5): List<Pair<String, Int>> =
    tracks.groupBy { it.artist.trim() }
      .filterKeys { it.isNotEmpty() }
      .map { (artist, list) -> artist to list.sumOf { counts[it.key] ?: 1 } }
      .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first.lowercase() })
      .take(limit)
}
