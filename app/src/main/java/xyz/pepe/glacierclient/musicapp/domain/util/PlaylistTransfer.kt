package xyz.pepe.glacierclient.musicapp.domain.util

/** One entry of an imported M3U: what the file says about a track, before it is matched to a real source. */
data class M3uEntry(val artist: String, val title: String, val url: String?)

/** Pure parsing/ordering used by playlist import and sorting. */
object PlaylistTransfer {
  /**
   * Parses the EXTM3U text this app exports (and the common subset other players write): an
   * `#EXTINF:seconds,Artist - Title` line followed by a URL line. Plain URL lines without a title
   * are kept with an empty title; comment lines are ignored.
   */
  fun parseM3u(text: String): List<M3uEntry> {
    val out = ArrayList<M3uEntry>()
    var pendingArtist = ""
    var pendingTitle: String? = null
    for (raw in text.lineSequence()) {
      val line = raw.trim()
      if (line.isEmpty()) continue
      if (line.startsWith("#EXTINF", ignoreCase = true)) {
        val label = line.substringAfter(',', "").trim()
        val dash = label.indexOf(" - ")
        if (dash > 0) { pendingArtist = label.substring(0, dash).trim(); pendingTitle = label.substring(dash + 3).trim() }
        else { pendingArtist = ""; pendingTitle = label }
        continue
      }
      if (line.startsWith("#")) continue
      out += M3uEntry(pendingArtist, pendingTitle ?: "", line)
      pendingArtist = ""; pendingTitle = null
    }
    return out
  }

  private val YT_ID = Regex("(?:v=|youtu\\.be/|/shorts/)([A-Za-z0-9_-]{11})")

  /** The 11-character YouTube video id in a watch/short link, or null. */
  fun youtubeId(url: String?): String? = url?.let { YT_ID.find(it)?.groupValues?.get(1) }

  enum class Sort(val label: String) { TITLE("Title A-Z"), ARTIST("Artist A-Z"), SHORTEST("Shortest first"), LONGEST("Longest first"), REVERSE("Reverse order") }

  /** Row ids in the order [sort] wants; rows are (id, title, artist, durationMs). Stable for ties. */
  fun sortedIds(rows: List<Quad>, sort: Sort): List<Long> = when (sort) {
    Sort.TITLE -> rows.sortedBy { it.title.lowercase() }
    Sort.ARTIST -> rows.sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))
    Sort.SHORTEST -> rows.sortedBy { it.durationMs ?: Long.MAX_VALUE }
    Sort.LONGEST -> rows.sortedByDescending { it.durationMs ?: -1L }
    Sort.REVERSE -> rows.reversed()
  }.map { it.id }

  data class Quad(val id: Long, val title: String, val artist: String, val durationMs: Long?)
}
