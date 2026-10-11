package xyz.pepe.glacierclient.musicapp.data.repository

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import xyz.pepe.glacierclient.musicapp.domain.model.AppSettings
import xyz.pepe.glacierclient.musicapp.domain.model.NavLabels
import xyz.pepe.glacierclient.musicapp.domain.model.NavStyle
import xyz.pepe.glacierclient.musicapp.domain.model.RepeatMode
import xyz.pepe.glacierclient.musicapp.domain.model.Source
import xyz.pepe.glacierclient.musicapp.domain.repository.SettingsRepository

/** Persists [AppSettings] as one JSON blob in SharedPreferences, mirroring Calculator's approach. */
class PrefsSettingsRepository(private val prefs: SharedPreferences) : SettingsRepository {
  private val _settings = MutableStateFlow(load())
  override val settings: StateFlow<AppSettings> = _settings

  override fun update(transform: (AppSettings) -> AppSettings) {
    val next = transform(_settings.value)
    _settings.value = next
    save(next)
  }

  override fun exportJson(): String = prefs.getString(KEY, null) ?: run { save(_settings.value); prefs.getString(KEY, "{}")!! }

  override fun importJson(json: String): Boolean {
    runCatching { JSONObject(json) }.getOrNull() ?: return false // reject garbage before it silently becomes defaults
    val parsed = load(json)
    _settings.value = parsed
    save(parsed)
    return true
  }

  private fun load(json: String? = prefs.getString(KEY, null)): AppSettings {
    json ?: return AppSettings()
    return runCatching {
      val o = JSONObject(json)
      AppSettings(
        defaultSpeed = o.optDouble("defaultSpeed", 1.0).toFloat().takeIf { it in 0.25f..3f } ?: 1f,
        resumePlayback = o.optBoolean("resumePlayback", true),
        primarySource = runCatching { Source.valueOf(o.optString("primarySource")) }.getOrDefault(Source.YOUTUBE_MUSIC),
        sourcePriority = o.optJSONArray("sourcePriority")?.let { arr ->
          (0 until arr.length()).mapNotNull { i -> runCatching { Source.valueOf(arr.getString(i)) }.getOrNull() }
        }?.distinct()?.takeIf { it.isNotEmpty() }?.let { saved ->
          // A source missing from an old/partial blob still gets a slot (at the end) so it can be reordered.
          saved + listOf(Source.YOUTUBE_MUSIC, Source.AUDIUS, Source.INTERNET_ARCHIVE, Source.CCMIXTER).filter { it !in saved }
        } ?: listOf(Source.YOUTUBE_MUSIC, Source.AUDIUS, Source.INTERNET_ARCHIVE, Source.CCMIXTER),
        enabledSources = o.optJSONArray("enabledSources")?.let { arr ->
          (0 until arr.length()).mapNotNull { i -> runCatching { Source.valueOf(arr.getString(i)) }.getOrNull() }.toSet()
        }?.takeIf { it.isNotEmpty() } ?: setOf(Source.YOUTUBE_MUSIC, Source.AUDIUS, Source.INTERNET_ARCHIVE, Source.CCMIXTER), // never load "every source off"
        wallpaperColors = o.optBoolean("wallpaperColors", false),
        glassEffects = o.optBoolean("glassEffects", true),
        haptics = o.optBoolean("haptics", true),
        navStyle = runCatching { NavStyle.valueOf(o.optString("navStyle")) }.getOrDefault(NavStyle.CLASSIC),
        navLabels = runCatching { NavLabels.valueOf(o.optString("navLabels")) }.getOrDefault(NavLabels.SELECTED),
        shuffleOnPlay = o.optBoolean("shuffleOnPlay", false),
        defaultRepeat = runCatching { RepeatMode.valueOf(o.optString("defaultRepeat")) }.getOrDefault(RepeatMode.OFF),
        eqEnabled = o.optBoolean("eqEnabled", false),
        eqPreset = o.optInt("eqPreset", -1),
        displayName = o.optString("displayName", "").take(40),
        showGreeting = o.optBoolean("showGreeting", true),
        showNotificationButton = o.optBoolean("showNotificationButton", true),
        showCategories = o.optBoolean("showCategories", true),
        homeGrid = o.optBoolean("homeGrid", false),
        homeLayout = o.optString("homeLayout", "").take(4096),
        skipSilence = o.optBoolean("skipSilence", false),
        bassBoost = o.optInt("bassBoost", 0).coerceIn(0, 10),
        trebleBoost = o.optInt("trebleBoost", 0).coerceIn(0, 10),
        doubleTapSeek = o.optBoolean("doubleTapSeek", true),
        doubleTapSeekSeconds = o.optInt("doubleTapSeekSeconds", 10).takeIf { it in 1..120 } ?: 10,
        swipeMiniPlayerToSkip = o.optBoolean("swipeMiniPlayerToSkip", true),
        fadeInOnPlay = o.optBoolean("fadeInOnPlay", false),
        fadeOutOnEnd = o.optBoolean("fadeOutOnEnd", false),
        shakeToSkip = o.optBoolean("shakeToSkip", false),
        batterySaver = o.optBoolean("batterySaver", false),
        wifiOnlyDownloads = o.optBoolean("wifiOnlyDownloads", true),
        privateSession = o.optBoolean("privateSession", false),
        offlineMode = o.optBoolean("offlineMode", false),
        autoDownloadLiked = o.optBoolean("autoDownloadLiked", false),
        loudnessBoost = o.optBoolean("loudnessBoost", false),
        sleepFadeSeconds = o.optInt("sleepFadeSeconds", 10).takeIf { it in 1..120 } ?: 10,
        lyricsOffsetMs = o.optInt("lyricsOffsetMs", 0).coerceIn(-5000, 5000),
        pauseOnUnplug = o.optBoolean("pauseOnUnplug", true),
        lyricsFontScale = o.optDouble("lyricsFontScale", 1.0).toFloat().takeIf { it in 0.7f..1.6f } ?: 1f,
        categories = o.optJSONArray("categories")?.let { a -> List(a.length()) { a.getString(it) } }
          ?: xyz.pepe.glacierclient.musicapp.domain.model.DEFAULT_CATEGORIES,
      )
    }.getOrDefault(AppSettings())
  }

  private fun save(s: AppSettings) {
    val o = JSONObject()
      .put("defaultSpeed", s.defaultSpeed.toDouble())
      .put("resumePlayback", s.resumePlayback)
      .put("primarySource", s.primarySource.name)
      .put("sourcePriority", org.json.JSONArray(s.sourcePriority.map { it.name }))
      .put("enabledSources", org.json.JSONArray(s.enabledSources.map { it.name }))
      .put("wallpaperColors", s.wallpaperColors)
      .put("glassEffects", s.glassEffects)
      .put("haptics", s.haptics)
      .put("navStyle", s.navStyle.name)
      .put("navLabels", s.navLabels.name)
      .put("shuffleOnPlay", s.shuffleOnPlay)
      .put("defaultRepeat", s.defaultRepeat.name)
      .put("eqEnabled", s.eqEnabled)
      .put("eqPreset", s.eqPreset)
      .put("displayName", s.displayName)
      .put("showGreeting", s.showGreeting)
      .put("showNotificationButton", s.showNotificationButton)
      .put("showCategories", s.showCategories)
      .put("homeGrid", s.homeGrid)
      .put("homeLayout", s.homeLayout)
      .put("skipSilence", s.skipSilence)
      .put("bassBoost", s.bassBoost)
      .put("trebleBoost", s.trebleBoost)
      .put("doubleTapSeek", s.doubleTapSeek)
      .put("doubleTapSeekSeconds", s.doubleTapSeekSeconds)
      .put("swipeMiniPlayerToSkip", s.swipeMiniPlayerToSkip)
      .put("fadeInOnPlay", s.fadeInOnPlay)
      .put("fadeOutOnEnd", s.fadeOutOnEnd)
      .put("shakeToSkip", s.shakeToSkip)
      .put("batterySaver", s.batterySaver)
      .put("wifiOnlyDownloads", s.wifiOnlyDownloads)
      .put("privateSession", s.privateSession)
      .put("offlineMode", s.offlineMode)
      .put("autoDownloadLiked", s.autoDownloadLiked)
      .put("loudnessBoost", s.loudnessBoost)
      .put("sleepFadeSeconds", s.sleepFadeSeconds)
      .put("lyricsOffsetMs", s.lyricsOffsetMs)
      .put("pauseOnUnplug", s.pauseOnUnplug)
      .put("lyricsFontScale", s.lyricsFontScale.toDouble())
      .put("categories", org.json.JSONArray(s.categories))
    prefs.edit().putString(KEY, o.toString()).apply()
  }

  private companion object { const val KEY = "app_settings_json" }
}
