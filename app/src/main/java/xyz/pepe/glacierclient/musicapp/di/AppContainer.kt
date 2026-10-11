package xyz.pepe.glacierclient.musicapp.di

import android.content.Context
import kotlinx.coroutines.launch
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import xyz.pepe.glacierclient.musicapp.data.repository.NotificationsRepository
import androidx.room.Room
import com.squareup.moshi.Moshi
import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import xyz.pepe.glacierclient.musicapp.data.local.AppDatabase
import xyz.pepe.glacierclient.musicapp.data.remote.AudiusClient
import xyz.pepe.glacierclient.musicapp.data.remote.CcMixterClient
import xyz.pepe.glacierclient.musicapp.data.remote.InternetArchiveClient
import xyz.pepe.glacierclient.musicapp.data.remote.RadioBrowserClient
import xyz.pepe.glacierclient.musicapp.data.remote.YtMusicClient
import xyz.pepe.glacierclient.musicapp.data.repository.CatalogRepository
import xyz.pepe.glacierclient.musicapp.data.repository.FavoritesRepository
import xyz.pepe.glacierclient.musicapp.data.repository.PlaybackStateRepository
import xyz.pepe.glacierclient.musicapp.data.repository.PlaylistRepository
import xyz.pepe.glacierclient.musicapp.data.repository.PrefsSettingsRepository
import xyz.pepe.glacierclient.musicapp.data.repository.YtAuthRepository
import xyz.pepe.glacierclient.musicapp.domain.repository.SettingsRepository
import xyz.pepe.glacierclient.musicapp.playback.PlayerController

private const val USER_AGENT = "PepesMusic/1.0 (+https://github.com/pepe; android)"

/** Manual DI container, created once in the Application. No Hilt, Koin or Dagger. */

class AppContainer(private val context: Context) {
  private fun prefs(name: String) = context.getSharedPreferences(name, Context.MODE_PRIVATE)

  val moshi: Moshi = Moshi.Builder().build()

  // Only fills in a default User-Agent when a request didn't set its own — YtMusicClient sets a
  // browser-like one on every request (required for YouTube Music's innertube API to work at
  // all), and this used to unconditionally clobber it back to "PepesMusic/1.0" on every request,
  // including authenticated ones, which is an obvious bot signal.
  private val userAgentInterceptor = Interceptor { chain ->
    val request = chain.request()
    val withUa = if (request.header("User-Agent") != null) request else request.newBuilder().header("User-Agent", USER_AGENT).build()
    chain.proceed(withUa)
  }

  val okHttp: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .addInterceptor(userAgentInterceptor)
    .build()

  fun retrofit(base: String): Retrofit = Retrofit.Builder()
    .baseUrl(base)
    .client(okHttp)
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .build()

  val settings: SettingsRepository by lazy { PrefsSettingsRepository(prefs("settings")) }
  val ytAuth: YtAuthRepository by lazy { YtAuthRepository(prefs("yt_auth")) }
  val ytMusic: YtMusicClient by lazy { YtMusicClient(okHttp, ytAuth) }
  val audius: AudiusClient by lazy { AudiusClient(okHttp) }
  val internetArchive: InternetArchiveClient by lazy { InternetArchiveClient(okHttp) }
  val ccMixter: CcMixterClient by lazy { CcMixterClient(okHttp) }
  val radioBrowser: RadioBrowserClient by lazy { RadioBrowserClient(okHttp) }
  val lyrics: xyz.pepe.glacierclient.musicapp.data.remote.LyricsClient by lazy { xyz.pepe.glacierclient.musicapp.data.remote.LyricsClient(okHttp, prefs("lyrics_cache")) }
  val catalog: CatalogRepository by lazy { CatalogRepository(ytMusic, audius, internetArchive, ccMixter) }
  val favorites: FavoritesRepository by lazy { FavoritesRepository(prefs("favorites")) }
  val history: xyz.pepe.glacierclient.musicapp.data.repository.HistoryRepository by lazy { xyz.pepe.glacierclient.musicapp.data.repository.HistoryRepository(prefs("history")) }
  val bookmarks: xyz.pepe.glacierclient.musicapp.data.repository.BookmarkRepository by lazy { xyz.pepe.glacierclient.musicapp.data.repository.BookmarkRepository(prefs("bookmarks")) }
  val streaks: xyz.pepe.glacierclient.musicapp.data.repository.StreakRepository by lazy { xyz.pepe.glacierclient.musicapp.data.repository.StreakRepository(prefs("streaks")) }
  val database: AppDatabase by lazy { Room.databaseBuilder(context, AppDatabase::class.java, "pepes_music.db").addMigrations(AppDatabase.MIGRATION_1_2).build() }
  val downloads: xyz.pepe.glacierclient.musicapp.data.repository.DownloadRepository by lazy {
    xyz.pepe.glacierclient.musicapp.data.repository.DownloadRepository(
      dao = database.downloadDao(), dir = java.io.File(context.filesDir, "downloads"), http = okHttp,
      resolveUrl = { catalog.resolveRemoteStreamUrl(it) },
      allowedNow = {
        if (!settings.settings.value.wifiOnlyDownloads) true
        else (context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager).let { !it.isActiveNetworkMetered }
      },
    )
  }
  val playlists: PlaylistRepository by lazy { PlaylistRepository(database.playlistDao()) }
  val playbackState: PlaybackStateRepository by lazy { PlaybackStateRepository(prefs("playback_state")) }
  val listeningStats: xyz.pepe.glacierclient.musicapp.data.repository.ListeningStatsRepository by lazy { xyz.pepe.glacierclient.musicapp.data.repository.ListeningStatsRepository(prefs("listening_stats")) }
  val player: PlayerController by lazy { PlayerController(context, catalog, playbackState, settings, history, streaks, listeningStats) }

  /** A tab requested from outside Compose (launcher shortcuts); AppRoot consumes and clears it. */
  val requestedTab = kotlinx.coroutines.flow.MutableStateFlow<Int?>(null)

  /** Keyboard-driven UI commands for AppRoot: "settings", "back", "nowplaying", "search". */
  val uiCommands = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 8)

  /** Set by AppRoot; lets plain arrow keys seek/adjust volume only while Now Playing is open. */
  @Volatile var nowPlayingVisible: Boolean = false

  /** Snackbar messages with an optional Undo action, shown by AppRoot. */
  data class UndoMessage(val text: String, val onUndo: (() -> Unit)? = null)
  val messages = kotlinx.coroutines.flow.MutableSharedFlow<UndoMessage>(extraBufferCapacity = 4)
  val notifications: NotificationsRepository by lazy { NotificationsRepository(prefs("notifications")) }
  val searchState: xyz.pepe.glacierclient.musicapp.ui.search.SearchViewState by lazy { xyz.pepe.glacierclient.musicapp.ui.search.SearchViewState() }
  val searchHistory: xyz.pepe.glacierclient.musicapp.data.repository.SearchHistoryRepository by lazy { xyz.pepe.glacierclient.musicapp.data.repository.SearchHistoryRepository(prefs("search_history")) }

  private val appScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main)

  /** Hooks real app events into the in-app inbox and keeps the active account's name/photo current. */
  fun startBackgroundWork() {
    // Offline-first playback: tell the catalog where saved files live.
    catalog.localPathFor = { t -> downloads.localFile(t)?.let { f -> android.net.Uri.fromFile(f).toString() } }
    catalog.offlineMode = { settings.settings.value.offlineMode }
    catalog.downloadedTracks = { downloads.allTracks() }
    // Auto-download liked songs: whenever the setting is on or the liked list changes, queue any
    // liked song that isn't saved yet. Download itself enforces the Wi-Fi-only rule.
    appScope.launch {
      kotlinx.coroutines.flow.combine(favorites.liked, settings.settings.map { it.autoDownloadLiked }.distinctUntilChanged()) { liked, on -> liked to on }
        .collect { (liked, on) ->
          if (!on || liked.isEmpty()) return@collect
          val have = downloads.downloads.first().map { it.key }.toSet()
          liked.filter { it.key !in have }.forEach { downloads.download(it) }
        }
    }
    // Keep the home-screen widget in step with what's playing.
    appScope.launch {
      player.state.map { Triple(it.current?.key, it.isPlaying, it.current?.title) }.distinctUntilChanged().collect {
        runCatching { xyz.pepe.glacierclient.musicapp.widget.NowPlayingWidget().updateAll(context) }
      }
    }
    appScope.launch { player.errorEvents.collect { notifications.post("Playback problem", it) } }
    appScope.launch {
      var lastId: String? = ytAuth.activeAccount.value?.id
      ytAuth.activeAccount.collect { acc ->
        if (acc == null) return@collect
        if (acc.photoUrl == null || acc.id != lastId) {
          val info = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { catalog.accountInfo() }
          if (info != null && (info.first != acc.label || info.second != acc.photoUrl)) {
            ytAuth.updateProfile(acc.id, info.first, info.second)
            if (acc.id != lastId || acc.label.startsWith("Account ")) notifications.post("Signed in", "YouTube Music: ${info.first}")
          }
        }
        lastId = acc.id
      }
    }
  }
}
