package xyz.pepe.glacierclient.musicapp.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import xyz.pepe.glacierclient.musicapp.data.repository.PrefsSettingsRepository
import xyz.pepe.glacierclient.musicapp.domain.model.Source

@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {
  private fun newRepo(): PrefsSettingsRepository {
    val prefs = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("test_settings", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()
    return PrefsSettingsRepository(prefs)
  }

  @Test fun `defaults have YouTube Music as primary source`() {
    val repo = newRepo()
    assertEquals(Source.YOUTUBE_MUSIC, repo.settings.value.primarySource)
  }

  @Test fun `update round trips through a new repository instance`() {
    val prefs = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("test_settings_roundtrip", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()
    val repo1 = PrefsSettingsRepository(prefs)
    repo1.update { it.copy(primarySource = Source.AUDIUS, defaultSpeed = 1.5f) }

    val repo2 = PrefsSettingsRepository(prefs)
    assertEquals(Source.AUDIUS, repo2.settings.value.primarySource)
    assertEquals(1.5f, repo2.settings.value.defaultSpeed)
  }

  @Test fun `unknown enum value in storage falls back to default`() {
    val prefs = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("test_settings_unknown", Context.MODE_PRIVATE)
    prefs.edit().clear().putString("app_settings_json", """{"primarySource":"NOT_A_REAL_SOURCE"}""").commit()
    val repo = PrefsSettingsRepository(prefs)
    assertEquals(Source.YOUTUBE_MUSIC, repo.settings.value.primarySource)
  }

  @Test fun `enabled sources exclude local download by default`() {
    val repo = newRepo()
    assertTrue(Source.LOCAL_DOWNLOAD !in repo.settings.value.enabledSources)
    assertTrue(Source.YOUTUBE_MUSIC in repo.settings.value.enabledSources)
  }
}
