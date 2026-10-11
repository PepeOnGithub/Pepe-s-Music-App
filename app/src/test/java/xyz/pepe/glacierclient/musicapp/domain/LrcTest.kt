package xyz.pepe.glacierclient.musicapp.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.pepe.glacierclient.musicapp.domain.util.Lrc

class LrcTest {
  @Test fun `basic line parses time and text`() {
    val lines = Lrc.parse("[00:12.50]Hello world")
    assertEquals(1, lines.size)
    assertEquals(12500L, lines[0].timeMs)
    assertEquals("Hello world", lines[0].text)
  }

  @Test fun `multiple tags on one line produce multiple entries`() {
    val lines = Lrc.parse("[00:01.00][00:05.00]Repeated")
    assertEquals(2, lines.size)
    assertEquals(1000L, lines[0].timeMs)
    assertEquals(5000L, lines[1].timeMs)
    assertEquals("Repeated", lines[1].text)
  }

  @Test fun `offset tag shifts all times`() {
    val lines = Lrc.parse("[offset:+500]\n[00:01.00]Line")
    assertEquals(1500L, lines[0].timeMs)
  }

  @Test fun `negative offset shifts backward`() {
    val lines = Lrc.parse("[offset:-200]\n[00:01.00]Line")
    assertEquals(800L, lines[0].timeMs)
  }

  @Test fun `metadata tags are ignored`() {
    val lines = Lrc.parse("[ar:Artist]\n[ti:Title]\n[00:01.00]Line")
    assertEquals(1, lines.size)
  }

  @Test fun `unsorted input is sorted by time`() {
    val lines = Lrc.parse("[00:10.00]Second\n[00:01.00]First")
    assertEquals("First", lines[0].text)
    assertEquals("Second", lines[1].text)
  }

  @Test fun `empty input returns empty list`() {
    assertEquals(emptyList<Any>(), Lrc.parse(""))
  }

  @Test fun `three digit fraction is milliseconds`() {
    val lines = Lrc.parse("[00:01.500]Line")
    assertEquals(1500L, lines[0].timeMs)
  }

  @Test fun `instrumental gap is kept`() {
    val lines = Lrc.parse("[00:01.00]♪")
    assertEquals("♪", lines[0].text)
  }

  @Test fun `currentIndex before first line is -1`() {
    val lines = Lrc.parse("[00:10.00]A")
    assertEquals(-1, Lrc.currentIndex(lines, 5000))
  }

  @Test fun `currentIndex at exact line time`() {
    val lines = Lrc.parse("[00:10.00]A\n[00:20.00]B")
    assertEquals(0, Lrc.currentIndex(lines, 10000))
    assertEquals(1, Lrc.currentIndex(lines, 20000))
  }

  @Test fun `currentIndex between lines returns earlier index`() {
    val lines = Lrc.parse("[00:10.00]A\n[00:20.00]B")
    assertEquals(0, Lrc.currentIndex(lines, 15000))
  }

  @Test fun `currentIndex after last line returns last index`() {
    val lines = Lrc.parse("[00:10.00]A\n[00:20.00]B")
    assertEquals(1, Lrc.currentIndex(lines, 999_999))
  }
}
