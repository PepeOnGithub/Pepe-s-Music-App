package xyz.pepe.glacierclient.musicapp.domain.model

enum class NavStyle { FLOATING, CLASSIC }
enum class NavLabels { SELECTED, ALL, NONE }
enum class RepeatMode { OFF, ALL, ONE }

// Every field below is read by real app behavior — see PlayerController/Theme for where each
// one is applied. Don't add a toggle here that nothing acts on; remove it instead.
data class AppSettings(
  val defaultSpeed: Float = 1f,
  val resumePlayback: Boolean = true,
  val primarySource: Source = Source.YOUTUBE_MUSIC,
  val sourcePriority: List<Source> = listOf(Source.YOUTUBE_MUSIC, Source.AUDIUS, Source.INTERNET_ARCHIVE, Source.CCMIXTER),
  val enabledSources: Set<Source> = setOf(Source.YOUTUBE_MUSIC, Source.AUDIUS, Source.INTERNET_ARCHIVE, Source.CCMIXTER),
  val wallpaperColors: Boolean = false,
  val glassEffects: Boolean = true,
  val haptics: Boolean = true,
  val navStyle: NavStyle = NavStyle.CLASSIC,
  val navLabels: NavLabels = NavLabels.SELECTED,
  val shuffleOnPlay: Boolean = false,
  val defaultRepeat: RepeatMode = RepeatMode.OFF,
  val eqEnabled: Boolean = false,
  val eqPreset: Int = -1,
  val displayName: String = "",
  val showGreeting: Boolean = true,
  val showNotificationButton: Boolean = true,
  val showCategories: Boolean = true,
  val categories: List<String> = DEFAULT_CATEGORIES,
  val homeGrid: Boolean = false,
  /** Encoded DashboardLayout of the Home sections (order, width, hidden); blank = default. */
  val homeLayout: String = "",
  val skipSilence: Boolean = false,
  val bassBoost: Int = 0,
  val trebleBoost: Int = 0,
  val doubleTapSeek: Boolean = true,
  val doubleTapSeekSeconds: Int = 10,
  val swipeMiniPlayerToSkip: Boolean = true,
  val fadeInOnPlay: Boolean = false,
  val fadeOutOnEnd: Boolean = false,
  val shakeToSkip: Boolean = false,
  val batterySaver: Boolean = false,
  val privateSession: Boolean = false,
  val lyricsFontScale: Float = 1f,
  /** Downloads wait for an unmetered connection (Wi-Fi) unless turned off. */
  val wifiOnlyDownloads: Boolean = true,
  /** Only downloaded songs are searched and played; nothing touches the network. */
  val offlineMode: Boolean = false,
  /** Every liked song is downloaded automatically (respects [wifiOnlyDownloads]). */
  val autoDownloadLiked: Boolean = false,
  /** Raises quiet tracks with the system loudness enhancer. */
  val loudnessBoost: Boolean = false,
  /** Length of the volume fade-out at the end of the sleep timer, in seconds. */
  val sleepFadeSeconds: Int = 10,
  /** Shifts synced lyrics earlier (negative) or later (positive), in milliseconds. */
  val lyricsOffsetMs: Int = 0,
  /** Pause when headphones are unplugged or Bluetooth disconnects. */
  val pauseOnUnplug: Boolean = true,
)

val DEFAULT_CATEGORIES = listOf("Party", "Chill", "Hip Hop", "Punjabi", "Sad", "Workout", "Pop", "Lo-fi")
