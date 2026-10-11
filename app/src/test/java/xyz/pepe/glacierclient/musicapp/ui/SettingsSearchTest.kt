package xyz.pepe.glacierclient.musicapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.pepe.glacierclient.musicapp.ui.settings.SETTINGS_INDEX
import xyz.pepe.glacierclient.musicapp.ui.settings.SettingEntry
import xyz.pepe.glacierclient.musicapp.ui.settings.SettingsPage
import xyz.pepe.glacierclient.musicapp.ui.settings.SettingsSearch

class SettingsSearchTest {
  private val entries = listOf(
    SettingEntry("Battery saver", "Fewer wakeups", SettingsPage.PLAYBACK, listOf("power")),
    SettingEntry("Bass boost", "Strengthens low frequencies", SettingsPage.EQUALIZER),
    SettingEntry("Wallpaper colors", "Use your wallpaper's colors", SettingsPage.APPEARANCE, listOf("material you")),
  )

  @Test fun `blank query returns nothing`() {
    assertTrue(SettingsSearch.filter("   ", entries).isEmpty())
  }

  @Test fun `matches case-insensitively on title prefix`() {
    assertEquals(listOf("Bass boost", "Battery saver"), SettingsSearch.filter("BA", entries).map { it.title })
  }

  @Test fun `matches substring in description and keywords and page`() {
    assertEquals("Battery saver", SettingsSearch.filter("power", entries).single().title)
    assertEquals("Bass boost", SettingsSearch.filter("frequen", entries).single().title)
    assertEquals("Wallpaper colors", SettingsSearch.filter("appearance", entries).single().title)
  }

  @Test fun `every word must match`() {
    assertEquals("Wallpaper colors", SettingsSearch.filter("material colors", entries).single().title)
    assertTrue(SettingsSearch.filter("battery bass", entries).isEmpty())
  }

  @Test fun `title matches rank above description matches`() {
    val list = listOf(
      SettingEntry("Zebra", "mentions boost here", SettingsPage.HOME),
      SettingEntry("Boost", "something else", SettingsPage.HOME),
    )
    assertEquals(listOf("Boost", "Zebra"), SettingsSearch.filter("boost", list).map { it.title })
  }

  @Test fun `real index has no duplicate title and page pairs`() {
    val keys = SETTINGS_INDEX.map { it.title to it.page }
    assertEquals(keys.size, keys.toSet().size)
    assertTrue(SettingsSearch.filter("shuffle").any { it.title == "Shuffle by default" })
  }
}
