package xyz.pepe.glacierclient.musicapp.domain.util

import xyz.pepe.glacierclient.musicapp.domain.model.LyricLine

private val TIME_TAG = Regex("""\[(\d{2}):(\d{2})(?:[.:](\d{1,3}))?\]""")
private val OFFSET_TAG = Regex("""\[offset:\s*([+-]?\d+)\]""", RegexOption.IGNORE_CASE)

object Lrc {
  /** Parses LRC text into sorted, offset-applied lyric lines. Metadata tags are ignored. */
  fun parse(text: String): List<LyricLine> {
    var offsetMs = 0L
    OFFSET_TAG.find(text)?.let { offsetMs = it.groupValues[1].toLong() }

    val lines = mutableListOf<LyricLine>()
    for (rawLine in text.lineSequence()) {
      val tags = TIME_TAG.findAll(rawLine).toList()
      if (tags.isEmpty()) continue
      val content = rawLine.substring(tags.last().range.last + 1).trim()
      if (content.isEmpty() && content != "♪") continue
      for (tag in tags) {
        val min = tag.groupValues[1].toLong()
        val sec = tag.groupValues[2].toLong()
        val frac = tag.groupValues[3]
        val fracMs = when (frac.length) {
          0 -> 0L
          1 -> frac.toLong() * 100
          2 -> frac.toLong() * 10
          else -> frac.toLong()
        }
        val timeMs = min * 60_000 + sec * 1000 + fracMs + offsetMs
        lines += LyricLine(timeMs, content)
      }
    }
    return lines.sortedBy { it.timeMs }
  }

  /** Index of the line active at [positionMs], or -1 if before the first line. */
  fun currentIndex(lines: List<LyricLine>, positionMs: Long): Int {
    if (lines.isEmpty() || positionMs < lines[0].timeMs) return -1
    var lo = 0
    var hi = lines.size - 1
    var result = 0
    while (lo <= hi) {
      val mid = (lo + hi) / 2
      if (lines[mid].timeMs <= positionMs) {
        result = mid
        lo = mid + 1
      } else {
        hi = mid - 1
      }
    }
    return result
  }
}
