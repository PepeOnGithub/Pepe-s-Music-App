package xyz.pepe.glacierclient.musicapp.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import xyz.pepe.glacierclient.musicapp.domain.model.Source
import xyz.pepe.glacierclient.musicapp.domain.model.Track
import xyz.pepe.glacierclient.musicapp.domain.util.ListeningStats
import xyz.pepe.glacierclient.musicapp.domain.util.PlaylistTransfer
import xyz.pepe.glacierclient.musicapp.domain.util.SleepTimer

class NewFeatureLogicTest {
  private fun t(id: String, artist: String) = Track(Source.YOUTUBE_MUSIC, id, "T$id", artist)

  @Test fun `last days are oldest first and fill gaps with zero`() {
    val today = LocalDate.of(2026, 3, 10)
    val days = ListeningStats.lastDays(mapOf("2026-03-10" to 120_000L, "2026-03-08" to 60_000L), today, 4)
    assertEquals(listOf(0L, 60_000L, 0L, 120_000L), days.map { it.second })
    assertEquals(LocalDate.of(2026, 3, 7), days.first().first)
  }

  @Test fun `prune drops old days`() {
    val today = LocalDate.of(2026, 3, 10)
    val kept = ListeningStats.prune(mapOf("2026-03-10" to 1L, "2025-01-01" to 2L), today, 30)
    assertEquals(setOf("2026-03-10"), kept.keys)
  }

  @Test fun `top artists ranked by play counts`() {
    val tracks = listOf(t("1", "A"), t("2", "B"), t("3", "B"))
    val top = ListeningStats.topArtists(tracks, mapOf("1" to 1).mapKeys { "YOUTUBE_MUSIC:${it.key}" } + mapOf("YOUTUBE_MUSIC:2" to 3), 2)
    assertEquals(listOf("B" to 4, "A" to 1), top)
  }

  @Test fun `m3u parsing reads artist title and url`() {
    val text = "#EXTM3U\n#EXTINF:215,Daft Punk - One More Time\nhttps://music.youtube.com/watch?v=FGBhQbmPwH8\n\n#EXTINF:10,Solo\nhttps://example.com/a.mp3\n"
    val e = PlaylistTransfer.parseM3u(text)
    assertEquals(2, e.size)
    assertEquals("Daft Punk", e[0].artist)
    assertEquals("One More Time", e[0].title)
    assertEquals("", e[1].artist)
    assertEquals("Solo", e[1].title)
  }

  @Test fun `youtube id extraction`() {
    assertEquals("FGBhQbmPwH8", PlaylistTransfer.youtubeId("https://music.youtube.com/watch?v=FGBhQbmPwH8&list=x"))
    assertEquals("FGBhQbmPwH8", PlaylistTransfer.youtubeId("https://youtu.be/FGBhQbmPwH8"))
    assertNull(PlaylistTransfer.youtubeId("https://example.com/a.mp3"))
  }

  @Test fun `playlist sorting`() {
    val rows = listOf(
      PlaylistTransfer.Quad(1, "b", "z", 300), PlaylistTransfer.Quad(2, "a", "y", 100), PlaylistTransfer.Quad(3, "c", "x", null),
    )
    assertEquals(listOf(2L, 1L, 3L), PlaylistTransfer.sortedIds(rows, PlaylistTransfer.Sort.TITLE))
    assertEquals(listOf(3L, 2L, 1L), PlaylistTransfer.sortedIds(rows, PlaylistTransfer.Sort.ARTIST))
    assertEquals(listOf(2L, 1L, 3L), PlaylistTransfer.sortedIds(rows, PlaylistTransfer.Sort.SHORTEST))
    assertEquals(listOf(3L, 2L, 1L), PlaylistTransfer.sortedIds(rows, PlaylistTransfer.Sort.REVERSE))
  }

  @Test fun `sleep fade uses the configured window`() {
    assertEquals(1f, SleepTimer.fadeVolume(30_000, 30_000), 0.001f)
    assertEquals(0.5f, SleepTimer.fadeVolume(2_500, 5_000), 0.001f)
    assertEquals(0f, SleepTimer.fadeVolume(0, 5_000), 0.001f)
  }
}
