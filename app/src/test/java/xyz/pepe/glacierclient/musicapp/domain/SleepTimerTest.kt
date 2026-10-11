package xyz.pepe.glacierclient.musicapp.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.pepe.glacierclient.musicapp.domain.util.SleepTimer

class SleepTimerTest {
  @Test fun `remaining computes difference`() {
    assertEquals(5000L, SleepTimer.remaining(nowMs = 1000, targetAtMs = 6000))
  }

  @Test fun `remaining never negative`() {
    assertEquals(0L, SleepTimer.remaining(nowMs = 9000, targetAtMs = 1000))
  }

  @Test fun `fade volume is full outside the fade window`() {
    assertEquals(1f, SleepTimer.fadeVolume(15_000), 0.001f)
  }

  @Test fun `fade volume is zero at target`() {
    assertEquals(0f, SleepTimer.fadeVolume(0), 0.001f)
  }

  @Test fun `fade volume is proportional inside the window`() {
    assertEquals(0.5f, SleepTimer.fadeVolume(5000), 0.001f)
  }
}
