package xyz.pepe.glacierclient.musicapp.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.pepe.glacierclient.musicapp.domain.model.RepeatMode
import xyz.pepe.glacierclient.musicapp.domain.util.QueueOps

class QueueOpsTest {
  @Test fun `shuffle keeps current item first`() {
    val q = listOf(1, 2, 3, 4, 5)
    val shuffled = QueueOps.shuffle(q, 2, seed = 42L)
    assertEquals(3, shuffled[0])
  }

  @Test fun `shuffle is deterministic with a seed`() {
    val q = listOf(1, 2, 3, 4, 5)
    val a = QueueOps.shuffle(q, 0, seed = 7L)
    val b = QueueOps.shuffle(q, 0, seed = 7L)
    assertEquals(a, b)
  }

  @Test fun `shuffle preserves all elements`() {
    val q = listOf(1, 2, 3, 4, 5)
    val shuffled = QueueOps.shuffle(q, 1, seed = 1L)
    assertEquals(q.toSet(), shuffled.toSet())
  }

  @Test fun `next with repeat OFF advances or returns null at end`() {
    assertEquals(1, QueueOps.next(0, 3, RepeatMode.OFF))
    assertEquals(null, QueueOps.next(2, 3, RepeatMode.OFF))
  }

  @Test fun `next with repeat ALL wraps around`() {
    assertEquals(0, QueueOps.next(2, 3, RepeatMode.ALL))
  }

  @Test fun `next with repeat ONE stays on same index`() {
    assertEquals(1, QueueOps.next(1, 3, RepeatMode.ONE))
  }

  @Test fun `previous restarts track past 3 seconds`() {
    assertEquals(2, QueueOps.previous(2, 5000, 5, RepeatMode.OFF))
  }

  @Test fun `previous moves back under 3 seconds`() {
    assertEquals(1, QueueOps.previous(2, 1000, 5, RepeatMode.OFF))
  }

  @Test fun `previous with repeat ALL wraps to end`() {
    assertEquals(4, QueueOps.previous(0, 0, 5, RepeatMode.ALL))
  }

  @Test fun `move adjusts current index when moving item before it`() {
    val (queue, idx) = QueueOps.move(listOf("a", "b", "c", "d"), currentIndex = 2, from = 0, to = 2)
    assertEquals(listOf("b", "c", "a", "d"), queue)
    assertEquals(1, idx)
  }

  @Test fun `move keeps current index tracking the moved item itself`() {
    val (queue, idx) = QueueOps.move(listOf("a", "b", "c"), currentIndex = 0, from = 0, to = 2)
    assertEquals(listOf("b", "c", "a"), queue)
    assertEquals(2, idx)
  }

  @Test fun `remove before current index shifts it back`() {
    val (queue, idx) = QueueOps.remove(listOf("a", "b", "c"), currentIndex = 2, index = 0)
    assertEquals(listOf("b", "c"), queue)
    assertEquals(1, idx)
  }

  @Test fun `remove at current index clamps to last valid`() {
    val (queue, idx) = QueueOps.remove(listOf("a", "b"), currentIndex = 1, index = 1)
    assertEquals(listOf("a"), queue)
    assertEquals(0, idx)
  }
}
