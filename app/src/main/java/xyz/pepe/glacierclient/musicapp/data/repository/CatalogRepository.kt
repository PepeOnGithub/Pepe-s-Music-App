package xyz.pepe.glacierclient.musicapp.data.repository

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import xyz.pepe.glacierclient.musicapp.data.remote.AudiusClient
import xyz.pepe.glacierclient.musicapp.data.remote.CcMixterClient
import xyz.pepe.glacierclient.musicapp.data.remote.InternetArchiveClient
import xyz.pepe.glacierclient.musicapp.data.remote.YtMusicClient
import xyz.pepe.glacierclient.musicapp.domain.model.AppSettings
import xyz.pepe.glacierclient.musicapp.domain.model.Source
import xyz.pepe.glacierclient.musicapp.domain.model.Track

/**
 * Fans a query out to every enabled full-track source (in [AppSettings.sourcePriority] order)
 * and merges the results; each source failing independently never blocks the others.
 */
class CatalogRepository(
  private val ytMusic: YtMusicClient,
  private val audius: AudiusClient,
  private val internetArchive: InternetArchiveClient,
  private val ccMixter: CcMixterClient,
  private val clock: () -> Long = System::currentTimeMillis,
) {
  // googlevideo URLs carry an `expire=` param (~6h). Cache resolved ones well inside that so the
  // prefetch below actually saves the tap a round-trip (it used to resolve and throw the URL away),
  // and so a URL persisted in Favorites/History/resume days ago is never handed to ExoPlayer.
  private data class Cached(val url: String, val at: Long)
  private val ytCache = ConcurrentHashMap<String, Cached>()

  /** Set by the container: true while Offline mode is on, so nothing touches the network. */
  var offlineMode: () -> Boolean = { false }

  /** Set by the container: every fully downloaded track (what Offline mode searches). */
  var downloadedTracks: suspend () -> List<Track> = { emptyList() }

  suspend fun search(query: String, settings: AppSettings): List<Track> {
    if (offlineMode()) {
      val words = query.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
      return downloadedTracks().filter { t ->
        val hay = "${t.title} ${t.artist}".lowercase()
        words.all { it in hay }
      }
    }
    return searchOnline(query, settings)
  }

  private suspend fun searchOnline(query: String, settings: AppSettings): List<Track> = coroutineScope {
    // The chosen primary source always leads, then the rest in priority order.
    val enabled = (listOf(settings.primarySource) + settings.sourcePriority).distinct().filter { it in settings.enabledSources }
    val deferred = enabled.map { source ->
      async(Dispatchers.IO) {
        // One slow host used to hold every other source's results hostage until OkHttp's own 15s
        // timeout; cap each source so the merged list arrives in bounded time.
        withTimeoutOrNull(SOURCE_TIMEOUT_MS) {
          runCatching {
            when (source) {
              Source.YOUTUBE_MUSIC -> ytMusic.search(query)
              Source.AUDIUS -> audius.search(query)
              Source.INTERNET_ARCHIVE -> internetArchive.search(query)
              Source.CCMIXTER -> ccMixter.search(query)
              else -> emptyList()
            }
          }.getOrDefault(emptyList())
        } ?: emptyList()
      }
    }
    deferred.flatMap { it.await() }.distinctBy { it.key }
  }

  suspend fun trending(settings: AppSettings): List<Track> {
    if (offlineMode()) return downloadedTracks()
    // Audius is the only source with a real trending chart. If the user turned it off (or it
    // returned nothing) fall back to a search over whichever sources are enabled instead of
    // silently using a disabled source.
    val chart = if (Source.AUDIUS in settings.enabledSources) {
      withContext(Dispatchers.IO) { runCatching { audius.trending() }.getOrDefault(emptyList()) }
    } else emptyList()
    return chart.ifEmpty { search("top hits", settings) }
  }

  /** Resolves a playable stream URL. YouTube Music URLs expire, so a stored one is never trusted. */
  suspend fun resolveStreamUrl(track: Track): String? =
    // A downloaded copy always wins: instant start, no data use, and works with no network.
    localPathFor(track) ?: resolveRemoteStreamUrl(track)

  /** Set by the container once downloads exist; returns a saved file's URI for offline playback. */
  var localPathFor: suspend (Track) -> String? = { null }

  suspend fun resolveRemoteStreamUrl(track: Track): String? = withContext(Dispatchers.IO) {
    if (offlineMode()) return@withContext null
    when (track.source) {
      Source.YOUTUBE_MUSIC -> cachedYt(track.sourceId) ?: runCatching { ytMusic.streamUrl(track.sourceId) }.getOrNull()
        ?.also { ytCache[track.sourceId] = Cached(it, clock()) }
      else -> track.streamUrl?.takeIf { it.startsWith("http") } // a stored file:// URI may be stale
    }
  }

  /** Drops a cached URL that just failed at playback time so the retry fetches a fresh one. */
  fun invalidate(track: Track) { ytCache.remove(track.sourceId) }

  private fun cachedYt(videoId: String): String? {
    val c = ytCache[videoId] ?: return null
    if (clock() - c.at < YT_URL_TTL_MS) return c.url
    ytCache.remove(videoId)
    return null
  }

  /** Resolves (and caches) the first few YouTube results in the background so a tap starts fast. */
  suspend fun prefetch(tracks: List<Track>, count: Int = 3) = withContext(Dispatchers.IO) {
    tracks.filter { it.source == Source.YOUTUBE_MUSIC && cachedYt(it.sourceId) == null }.take(count)
      .map { t -> async { resolveStreamUrl(t) } }.forEach { it.await() }
  }

  fun warmUp() = ytMusic.warmUp()

  fun accountInfo() = runCatching { ytMusic.accountInfo() }.getOrNull()

  companion object {
    const val YT_URL_TTL_MS = 3 * 60 * 60 * 1000L
    const val SOURCE_TIMEOUT_MS = 8_000L
  }
}
