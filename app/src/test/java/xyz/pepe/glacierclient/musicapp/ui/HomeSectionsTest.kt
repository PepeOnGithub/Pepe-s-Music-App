package xyz.pepe.glacierclient.musicapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.pepe.glacierclient.ecosystem.CardSize
import xyz.pepe.glacierclient.musicapp.ui.home.HomeSections

class HomeSectionsTest {
  @Test fun defaultsHaveUniqueIdsAndAreFull() {
    val ids = HomeSections.DEFAULTS.map { it.id }
    assertEquals(ids.size, ids.toSet().size)
    assertTrue(HomeSections.DEFAULTS.all { it.size == CardSize.FULL && !it.hidden })
  }

  @Test fun blankLoadsDefaultsAndSavesBlank() {
    val l = HomeSections.load("")
    assertEquals(HomeSections.DEFAULTS.map { it.id }, l.slots.map { it.id })
    assertEquals("", HomeSections.save(l))
  }

  @Test fun newSectionsAreAppendedAndUnknownDropped() {
    val l = HomeSections.load("trending:F:0,gone:F:0,categories:F:1")
    assertEquals(listOf("trending", "categories"), l.slots.map { it.id }.take(2))
    assertTrue(l.slots.none { it.id == "gone" })
    assertEquals(HomeSections.DEFAULTS.size, l.slots.size)
    assertTrue(l.slot("categories")!!.hidden)
  }

  @Test fun halfOnlyWhereAllowed() {
    val l = HomeSections.load("trending:H:0,quick_picks:H:0")
    assertEquals(CardSize.FULL, l.slot("trending")!!.size)
    assertEquals(CardSize.HALF, l.slot("quick_picks")!!.size)
    assertTrue(HomeSections.halfAllowed("speed_dial"))
  }

  @Test fun garbageFallsBackToDefaults() {
    assertEquals(HomeSections.load(null), HomeSections.load("%%%"))
  }
}
