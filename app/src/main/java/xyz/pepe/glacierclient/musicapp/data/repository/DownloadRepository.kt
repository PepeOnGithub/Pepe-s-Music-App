package xyz.pepe.glacierclient.musicapp.data.repository

import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import xyz.pepe.glacierclient.musicapp.data.local.DownloadDao
import xyz.pepe.glacierclient.musicapp.data.local.DownloadEntity
import xyz.pepe.glacierclient.musicapp.data.remote.BrowserFingerprint
import xyz.pepe.glacierclient.musicapp.domain.model.Source
import xyz.pepe.glacierclient.musicapp.domain.model.Track

/** Live state of one in-flight download; finished ones live in the database instead. */
sealed interface DownloadProgress {
  data object Queued : DownloadProgress
  data class Active(val fraction: Float?) : DownloadProgress
  data class Failed(val message: String) : DownloadProgress
}

/**
 * Saves full tracks to app-private storage so they play with no network. Files are written to a
 * `.part` file and renamed only when complete, so a killed app or dropped connection can never
 * leave a truncated file that later plays as a "downloaded" song.
 */
class DownloadRepository(
  private val dao: DownloadDao,
  private val dir: File,
  private val http: OkHttpClient,
  private val resolveUrl: suspend (Track) -> String?,
  /** Checked before every download starts; false means "not allowed right now" (e.g. Wi-Fi only on mobile data). */
  private val allowedNow: () -> Boolean = { true },
  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
  val downloads = dao.observeAll()

  private val _progress = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
  val progress: StateFlow<Map<String, DownloadProgress>> = _progress
  private val jobs = HashMap<String, Job>()

  init {
    dir.mkdirs()
    dir.listFiles { f -> f.name.endsWith(".part") }?.forEach { it.delete() }
  }

  /** Every fully downloaded track, for Offline mode search. */
  suspend fun allTracks(): List<Track> = withContext(Dispatchers.IO) { dao.getAll().map { it.toTrack() } }

  fun canDownload(track: Track) = track.source != Source.RADIO_BROWSER && track.source != Source.LOCAL_DOWNLOAD

  /** The saved file for [track], or null if it isn't downloaded (or the file has gone missing). */
  suspend fun localFile(track: Track): File? = withContext(Dispatchers.IO) {
    val row = dao.get(track.key) ?: return@withContext null
    val f = File(dir, row.fileName)
    if (f.isFile && f.length() == row.sizeBytes) return@withContext f
    dao.delete(track.key) // file was cleared externally; don't keep a dead row
    null
  }

  @Synchronized
  fun download(track: Track) {
    if (!canDownload(track) || jobs[track.key]?.isActive == true) return
    if (!allowedNow()) {
      _progress.update { it + (track.key to DownloadProgress.Failed("Waiting for Wi-Fi — downloads are set to Wi-Fi only")) }
      return
    }
    _progress.update { it + (track.key to DownloadProgress.Queued) }
    jobs[track.key] = scope.launch {
      val name = safeName(track.key) + ".audio"
      val part = File(dir, "$name.part")
      try {
        if (localFile(track) != null) { _progress.update { it - track.key }; return@launch }
        val url = resolveUrl(track)?.takeIf { it.startsWith("http") } ?: error("No stream available")
        val host = url.substringAfter("://").substringBefore('/')
        val req = Request.Builder().url(url).apply {
          if (host.endsWith("googlevideo.com")) BrowserFingerprint.STREAMING_HEADERS.forEach { (k, v) -> header(k, v) }
          if (host == "ccmixter.org") header("Referer", "https://ccmixter.org/")
        }.build()
        http.newBuilder().readTimeout(30, TimeUnit.SECONDS).build().newCall(req).execute().use { resp ->
          if (!resp.isSuccessful) error("HTTP ${resp.code}")
          val body = resp.body ?: error("Empty response")
          val total = body.contentLength().takeIf { it > 0 }
          var read = 0L
          body.byteStream().use { input ->
            part.outputStream().use { out ->
              val buf = ByteArray(64 * 1024)
              while (true) {
                val n = input.read(buf)
                if (n < 0) break
                ensureActive()
                out.write(buf, 0, n)
                read += n
                _progress.update { it + (track.key to DownloadProgress.Active(total?.let { t -> read.toFloat() / t })) }
              }
            }
          }
          if (total != null && read != total) error("Incomplete download")
          if (read == 0L) error("Empty file")
          val final = File(dir, name)
          final.delete()
          if (!part.renameTo(final)) error("Couldn't save the file")
          dao.upsert(
            DownloadEntity(track.key, track.source.name, track.sourceId, track.title, track.artist, track.artworkUrl, track.durationMs, track.shareUrl, name, final.length(), System.currentTimeMillis()),
          )
        }
        _progress.update { it - track.key }
      } catch (e: CancellationException) {
        part.delete()
        _progress.update { it - track.key }
        throw e
      } catch (e: Exception) {
        part.delete()
        _progress.update { it + (track.key to DownloadProgress.Failed(e.message ?: "Download failed")) }
      }
    }
  }

  fun cancel(track: Track) { jobs[track.key]?.cancel() }

  fun dismissFailure(track: Track) = _progress.update { it - track.key }

  suspend fun delete(track: Track) = withContext(Dispatchers.IO) {
    jobs[track.key]?.cancel()
    dao.get(track.key)?.let { File(dir, it.fileName).delete() }
    dao.delete(track.key)
  }

  suspend fun deleteAll() = withContext(Dispatchers.IO) {
    jobs.values.forEach { it.cancel() }
    dir.listFiles()?.forEach { it.delete() }
    dao.deleteAll()
  }

  suspend fun totalBytes(): Long = withContext(Dispatchers.IO) { dao.getAll().sumOf { it.sizeBytes } }

  private fun safeName(key: String) = key.replace(Regex("[^A-Za-z0-9._-]"), "_")
}
