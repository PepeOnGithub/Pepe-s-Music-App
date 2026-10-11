package xyz.pepe.glacierclient.musicapp.ecosystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import xyz.pepe.glacierclient.ecosystem.CardSize
import xyz.pepe.glacierclient.ecosystem.CardSlot
import xyz.pepe.glacierclient.ecosystem.DashboardLayout

class DashboardLayoutTest {
  private fun layout() = DashboardLayout.of(listOf(CardSlot("a"), CardSlot("b"), CardSlot("c"), CardSlot("d")))
  private fun ids(l: DashboardLayout) = l.visible.map { it.id }

  @Test fun moveByReordersAndClamps() {
    assertEquals(listOf("b", "a", "c", "d"), ids(layout().moveBy("a", 1)))
    assertEquals(listOf("a", "b", "d", "c"), ids(layout().moveBy("d", -1)))
    assertEquals(listOf("b", "c", "d", "a"), ids(layout().moveBy("a", 99)))
    assertEquals(ids(layout()), ids(layout().moveBy("a", -5)))
    assertEquals(ids(layout()), ids(layout().moveBy("nope", 1)))
  }

  @Test fun moveToUsesVisibleIndex() {
    assertEquals(listOf("c", "a", "b", "d"), ids(layout().moveTo("c", 0)))
  }

  @Test fun hiddenCardsKeepTheirPlaceWhileOthersMove() {
    val l = layout().setHidden("b", true)
    assertEquals(listOf("a", "c", "d"), ids(l))
    val moved = l.moveBy("a", 1) // a swaps with c, b stays hidden in between
    assertEquals(listOf("c", "a", "d"), ids(moved))
    assertEquals(listOf("b"), moved.hiddenSlots.map { it.id })
    assertEquals(listOf("a", "b", "c", "d"), moved.setHidden("b", false).slots.map { it.id }.sorted())
  }

  @Test fun rowsPackHalvesInPairs() {
    val l = layout().resize("a", CardSize.HALF).resize("b", CardSize.HALF).resize("d", CardSize.HALF)
    assertEquals(listOf(listOf("a", "b"), listOf("c"), listOf("d")), l.rows().map { r -> r.map { it.id } })
  }

  @Test fun loneHalfBeforeFullGetsItsOwnRow() {
    val l = layout().resize("b", CardSize.HALF)
    assertEquals(listOf(listOf("a"), listOf("b"), listOf("c"), listOf("d")), l.rows().map { r -> r.map { it.id } })
  }

  @Test fun toggleSizeFlips() {
    val l = layout().toggleSize("a")
    assertEquals(CardSize.HALF, l.slot("a")?.size)
    assertEquals(CardSize.FULL, l.toggleSize("a").slot("a")?.size)
  }

  @Test fun encodeDecodeRoundTrip() {
    val l = layout().resize("b", CardSize.HALF).setHidden("c", true).moveBy("d", -2)
    assertEquals(l, DashboardLayout.decode(l.encode()))
  }

  @Test fun decodeRejectsGarbage() {
    assertNull(DashboardLayout.decode(null))
    assertNull(DashboardLayout.decode(""))
    assertNull(DashboardLayout.decode("a:X:0"))
    assertNull(DashboardLayout.decode("a:F"))
    assertNull(DashboardLayout.decode("bad id:F:0"))
    assertNull(DashboardLayout.decode("a:F:0,".repeat(2000)))
    assertNotNull(DashboardLayout.decode("a:F:0,b:H:1"))
  }

  @Test fun decodeDropsDuplicateIds() {
    assertEquals(listOf("a", "b"), DashboardLayout.decode("a:F:0,a:H:1,b:F:0")?.slots?.map { it.id })
  }

  @Test fun reconcileKeepsSavedOrderDropsOldAppendsNew() {
    val saved = DashboardLayout.decode("c:H:0,old:F:0,a:F:1")
    val defaults = listOf(CardSlot("a"), CardSlot("b", CardSize.HALF), CardSlot("c"))
    val r = DashboardLayout.reconcile(saved, defaults)
    assertEquals(listOf("c", "a", "b"), r.slots.map { it.id })
    assertEquals(CardSize.HALF, r.slot("c")?.size) // saved size wins
    assertEquals(true, r.slot("a")?.hidden)          // saved hidden wins
    assertEquals(CardSize.HALF, r.slot("b")?.size)   // new card uses its default
  }

  @Test fun reconcileWithoutSavedIsTheDefaults() {
    val d = listOf(CardSlot("a"), CardSlot("b"))
    assertEquals(DashboardLayout.of(d), DashboardLayout.reconcile(null, d))
  }
}
