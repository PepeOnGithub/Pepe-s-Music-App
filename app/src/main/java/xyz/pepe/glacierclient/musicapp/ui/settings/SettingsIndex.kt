package xyz.pepe.glacierclient.musicapp.ui.settings

/** Every page the Settings host can show. [label] is what search results print as "Page name". */
enum class SettingsPage(val label: String) {
  MAIN("Settings"),
  PLAYBACK("Playback"),
  EQUALIZER("Playback · Equalizer"),
  GESTURES("Gestures"),
  SOURCES("Sources"),
  DOWNLOADS("Downloads"),
  APPEARANCE("Appearance"),
  HOME("Home & library"),
  PRIVACY("Privacy & data"),
  ABOUT("About"),
  LICENSES("About · Open-source licenses"),
}

/** One searchable setting. */
data class SettingEntry(
  val title: String,
  val description: String,
  val page: SettingsPage,
  val keywords: List<String> = emptyList(),
)

/**
 * THE single list the Settings search uses. NEW SETTINGS MUST BE ADDED HERE (title and description
 * should match what the page shows, plus a few keywords people might type) or they will not be
 * found by the search field on the main Settings page.
 */
val SETTINGS_INDEX: List<SettingEntry> = listOf(
  // About
  SettingEntry("Keyboard shortcuts", "Space, arrows, L, S, R, Ctrl+1 to 4", SettingsPage.ABOUT, listOf("keyboard", "keys", "hotkeys", "mouse", "shortcut")),
  SettingEntry("Open-source licenses", "Libraries used by this app", SettingsPage.LICENSES, listOf("license", "legal", "libraries", "credits")),
  SettingEntry("Pepe's apps", "Open the other apps in the Pepe family", SettingsPage.ABOUT, listOf("ecosystem", "notes", "weather", "clock", "translator", "calculator")),
  SettingEntry("Version", "The installed version of the app", SettingsPage.ABOUT, listOf("build", "about", "release")),
  // Appearance
  SettingEntry("Frosted glass", "Translucent floating nav bar and mini player; off uses solid surfaces", SettingsPage.APPEARANCE, listOf("blur", "translucent", "transparent", "glass")),
  SettingEntry("Haptic feedback", "Light vibration on switches, tabs and player buttons", SettingsPage.APPEARANCE, listOf("vibration", "vibrate", "touch")),
  SettingEntry("Navigation bar", "Floating is a pill over the content; Classic is the standard bar", SettingsPage.APPEARANCE, listOf("floating", "classic", "tabs", "style", "bottom")),
  SettingEntry("Navigation labels", "Show tab labels on the selected tab, always or never", SettingsPage.APPEARANCE, listOf("tabs", "text", "names")),
  SettingEntry("Wallpaper colors", "Use your wallpaper's colors for the app theme", SettingsPage.APPEARANCE, listOf("dynamic", "material you", "theme", "color")),
  // Downloads
  SettingEntry("Auto-download liked songs", "Every song you like is saved for offline play (respects Wi-Fi only)", SettingsPage.DOWNLOADS, listOf("automatic", "favorites", "save", "offline")),
  SettingEntry("Delete all downloads", "Frees the space; songs can be downloaded again any time", SettingsPage.DOWNLOADS, listOf("remove", "storage", "clear")),
  SettingEntry("Download on Wi-Fi only", "Saves mobile data. Turn off to download on any connection.", SettingsPage.DOWNLOADS, listOf("wifi", "mobile data", "cellular", "metered")),
  SettingEntry("Offline mode", "Search and play only downloaded songs; no network is used", SettingsPage.DOWNLOADS, listOf("airplane", "no internet", "data saver", "local")),
  SettingEntry("Storage used", "How much space downloaded songs take", SettingsPage.DOWNLOADS, listOf("size", "space", "megabytes")),
  // Equalizer
  SettingEntry("Bass boost", "Strengthens low frequencies", SettingsPage.EQUALIZER, listOf("bass", "low", "sound")),
  SettingEntry("Equalizer", "Uses the device's built-in audio effect", SettingsPage.EQUALIZER, listOf("eq", "sound", "bands", "audio effect")),
  SettingEntry("Equalizer preset", "Picking a preset turns the equalizer on", SettingsPage.EQUALIZER, listOf("rock", "pop", "jazz", "classical", "normal", "eq")),
  SettingEntry("Treble boost", "Raises the highest bands on top of the preset", SettingsPage.EQUALIZER, listOf("treble", "high", "sound")),
  // Gestures
  SettingEntry("Double-tap seek amount", "How far double-tapping the cover art seeks", SettingsPage.GESTURES, listOf("seconds", "skip", "rewind", "forward")),
  SettingEntry("Double-tap to seek", "Double-tap the left or right half of the cover art on Now Playing", SettingsPage.GESTURES, listOf("rewind", "forward", "cover art")),
  SettingEntry("Shake to skip", "Shake the phone to skip to the next track while music is playing", SettingsPage.GESTURES, listOf("accelerometer", "next")),
  SettingEntry("Swipe mini player to skip", "Swipe left for next, right for previous", SettingsPage.GESTURES, listOf("next", "previous", "mini player")),
  // Home & library
  SettingEntry("Categories", "Genre chips on Home that switch the song list", SettingsPage.HOME, listOf("genre", "chips", "mood")),
  SettingEntry("Categories (comma-separated)", "Each one searches \"<category> songs\" when tapped", SettingsPage.HOME, listOf("genre", "edit", "custom")),
  SettingEntry("Edit home layout", "Resize, reorder and hide the sections on Home", SettingsPage.HOME, listOf("customize", "arrange", "sections", "edit mode", "widgets", "tiles", "move")),
  SettingEntry("Greeting", "\"Good morning\" header with your name", SettingsPage.HOME, listOf("header", "hello", "welcome")),
  SettingEntry("Grid layout", "Show Home songs as cards instead of a list", SettingsPage.HOME, listOf("cards", "list", "view")),
  SettingEntry("Lyrics sync offset", "Nudge synced lyrics earlier or later", SettingsPage.HOME, listOf("timing", "delay", "karaoke", "late", "early")),
  SettingEntry("Lyrics text size", "Also adjustable with the A buttons in the lyrics view", SettingsPage.HOME, listOf("font", "bigger", "smaller", "scale")),
  SettingEntry("Notification button", "Bell on Home with your in-app notifications", SettingsPage.HOME, listOf("bell", "inbox", "alerts")),
  SettingEntry("Reset categories", "Puts the genre chips back to the defaults", SettingsPage.HOME, listOf("default", "genre")),
  SettingEntry("Reset home layout", "Puts the Home sections back to their default order and size", SettingsPage.HOME, listOf("default", "sections", "restore", "tiles")),
  SettingEntry("Your name", "Shown in the greeting", SettingsPage.HOME, listOf("display name", "username", "profile")),
  // Playback
  SettingEntry("Battery saver", "Updates the progress bar once a second and prepares fewer upcoming tracks", SettingsPage.PLAYBACK, listOf("power", "efficiency", "battery")),
  SettingEntry("Default repeat mode", "Repeat mode every new queue starts with (off, all, one)", SettingsPage.PLAYBACK, listOf("loop", "repeat one", "repeat all")),
  SettingEntry("Default speed", "Playback speed applied to each new queue", SettingsPage.PLAYBACK, listOf("tempo", "faster", "slower", "1.5x")),
  SettingEntry("Fade in on play", "Ramps volume up over the first 0.6 s of every track", SettingsPage.PLAYBACK, listOf("fade", "volume", "ramp")),
  SettingEntry("Fade out at track end", "Ramps volume down over the last 0.6 s of every track", SettingsPage.PLAYBACK, listOf("fade", "volume", "ramp", "crossfade")),
  SettingEntry("Loudness boost", "Raises quiet tracks so volume feels more even", SettingsPage.PLAYBACK, listOf("normalize", "normalization", "volume", "louder", "gain")),
  SettingEntry("Pause when headphones disconnect", "Pause playback when headphones are unplugged or Bluetooth drops", SettingsPage.PLAYBACK, listOf("unplug", "bluetooth", "noisy", "earphones", "headset")),
  SettingEntry("Resume playback on launch", "Reopen the last queue and position; tap play to continue", SettingsPage.PLAYBACK, listOf("restore", "last queue", "start")),
  SettingEntry("Shuffle by default", "New queues start shuffled", SettingsPage.PLAYBACK, listOf("random", "queue")),
  SettingEntry("Skip silence", "Skips silent stretches inside tracks", SettingsPage.PLAYBACK, listOf("gaps", "quiet", "silent")),
  SettingEntry("Sleep timer fade-out", "How long the volume fades before the sleep timer pauses", SettingsPage.PLAYBACK, listOf("sleep", "bedtime", "seconds", "timer")),
  // Privacy & data
  SettingEntry("Clear cache", "Frees space used by cached artwork and exported files", SettingsPage.PRIVACY, listOf("storage", "temporary", "images")),
  SettingEntry("Clear play history", "Removes Recently Played and Speed dial data", SettingsPage.PRIVACY, listOf("recently played", "most played", "delete")),
  SettingEntry("Clear search history", "Removes recent searches shown in Search", SettingsPage.PRIVACY, listOf("recent searches", "delete", "queries")),
  SettingEntry("Export settings", "Copies your settings as text you can back up", SettingsPage.PRIVACY, listOf("backup", "copy", "json")),
  SettingEntry("Import settings", "Paste a previously exported settings block", SettingsPage.PRIVACY, listOf("restore", "paste", "json")),
  SettingEntry("Private session", "Nothing is added to Recently Played, Most Played, search history or your streak", SettingsPage.PRIVACY, listOf("incognito", "history", "tracking")),
  SettingEntry("Reset all settings", "Puts every setting on every page back to its default", SettingsPage.PRIVACY, listOf("defaults", "factory", "restore")),
  // Sources
  SettingEntry("Audius", "Full tracks, free and open", SettingsPage.SOURCES, listOf("source", "enable", "disable", "provider")),
  SettingEntry("ccMixter", "Full tracks, Creative Commons remix community", SettingsPage.SOURCES, listOf("source", "enable", "disable", "provider", "remix")),
  SettingEntry("Enable and reorder priority", "Turn sources on or off and drag to reorder them", SettingsPage.SOURCES, listOf("sources", "order", "drag", "providers")),
  SettingEntry("Internet Archive", "Full tracks, independent/Creative Commons catalog", SettingsPage.SOURCES, listOf("source", "enable", "disable", "provider", "archive.org")),
  SettingEntry("Primary source", "Which source leads search results and is tried first", SettingsPage.SOURCES, listOf("default source", "main")),
  SettingEntry("Sign in to YouTube Music", "Logged-in requests are less likely to be blocked when resolving streams", SettingsPage.SOURCES, listOf("login", "account", "add account", "cookie")),
  SettingEntry("YouTube Music", "Full tracks, unofficial API (default)", SettingsPage.SOURCES, listOf("source", "enable", "disable", "provider", "youtube")),
  SettingEntry("YouTube Music accounts", "Switch or remove signed-in accounts", SettingsPage.SOURCES, listOf("login", "account", "profile", "switch")),
)

/** Pure matching used by the Settings search field; kept free of Android types so it unit-tests on the JVM. */
object SettingsSearch {
  /**
   * Every whitespace-separated word in [query] must match (case-insensitively) the start of a word
   * or any substring of the entry's title, description, keywords or page name. Results are ranked:
   * title starts with the query, then a title word starts with it, then title substring, then the
   * rest; ties break alphabetically. A blank query returns nothing.
   */
  fun filter(query: String, entries: List<SettingEntry> = SETTINGS_INDEX): List<SettingEntry> {
    val tokens = query.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return emptyList()
    return entries.mapNotNull { e ->
      val title = e.title.lowercase()
      val hay = (listOf(e.title, e.description, e.page.label) + e.keywords).joinToString(" ").lowercase()
      if (!tokens.all { it in hay }) return@mapNotNull null
      val score = when {
        title.startsWith(tokens.first()) -> 0
        tokens.all { t -> title.split(Regex("[^a-z0-9]+")).any { it.startsWith(t) } } -> 1
        tokens.all { it in title } -> 2
        else -> 3
      }
      score to e
    }.sortedWith(compareBy({ it.first }, { it.second.title.lowercase() })).map { it.second }
  }
}
