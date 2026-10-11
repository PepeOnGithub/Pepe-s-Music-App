package xyz.pepe.glacierclient.musicapp.domain.repository

import kotlinx.coroutines.flow.StateFlow
import xyz.pepe.glacierclient.musicapp.domain.model.AppSettings

interface SettingsRepository {
  val settings: StateFlow<AppSettings>
  fun update(transform: (AppSettings) -> AppSettings)
  /** The raw settings JSON, for Settings → Export/Import (real values, not a placeholder). */
  fun exportJson(): String
  /** Replaces settings from a previously-exported JSON blob; returns false if it couldn't parse. */
  fun importJson(json: String): Boolean
}
