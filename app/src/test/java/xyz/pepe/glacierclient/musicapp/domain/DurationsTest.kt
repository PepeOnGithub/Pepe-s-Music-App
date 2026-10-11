package xyz.pepe.glacierclient.musicapp.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.pepe.glacierclient.musicapp.domain.util.Durations

class DurationsTest {
  @Test fun `zero formats as 0-00`() {
    assertEquals("0:00", Durations.format(0))
  }

  @Test fun `59 seconds formats as 0-59`() {
    assertEquals("0:59", Durations.format(59_000))
  }

  @Test fun `over an hour includes hours`() {
    assertEquals("1:00:05", Durations.format(3_605_000))
  }

  @Test fun `null or negative returns placeholder`() {
    assertEquals("--:--", Durations.format(null))
    assertEquals("--:--", Durations.format(-1))
  }

  @Test fun `remaining formats with a leading minus`() {
    assertEquals("-0:30", Durations.formatRemaining(30_000, 60_000))
  }

  @Test fun `remaining clamps to zero past the end`() {
    assertEquals("-0:00", Durations.formatRemaining(70_000, 60_000))
  }
}
