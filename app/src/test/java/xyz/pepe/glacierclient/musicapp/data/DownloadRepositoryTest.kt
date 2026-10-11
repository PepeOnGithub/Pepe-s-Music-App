package xyz.pepe.glacierclient.musicapp.data

import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import xyz.pepe.glacierclient.musicapp.data.local.DownloadDao
import xyz.pepe.glacierclient.musicapp.data.local.DownloadEntity
import xyz.pepe.glacierclient.musicapp.data.repository.DownloadProgress
import xyz.pepe.glacierclient.musicapp.data.repository.DownloadRepository
import xyz.pepe.glacierclient.musicapp.domain.model.Source
import xyz.pepe.glacierclient.musicapp.domain.model.Track

private class FakeDao : DownloadDao {
  val rows = MutableStateFlow<List<DownloadEntity>>(emptyList())
  override suspend fun upsert(d: DownloadEntity) { rows.value = rows.value.filterNot { it.key == d.key } + d }
  override fun observeAll(): Flow<List<DownloadEntity>> = rows
  override suspend fun getAll() = rows.value
  override suspend fun get(key: String) = rows.value.firstOrNull { it.key == key }
  override suspend fun delete(key: String) { rows.value = rows.value.filterNot { it.key == key } }
  override suspend fun deleteAll() { rows.value = emptyList() }
}

class DownloadRepositoryTest {
  private lateinit var dir: File
  private lateinit var dao: FakeDao
  private val track = Track(Source.AUDIUS, "abc", "Song", "Artist", streamUrl = "https://x.test/a.mp3")

  @Before fun setUp() { dir = createTempDir("pepe-dl"); dao = FakeDao() }
  @After fun tearDown() { dir.deleteRecursively() }

  private fun client(body: ByteArray, declaredLength: Long = body.size.toLong(), code: Int = 200) = OkHttpClient.Builder()
    .addInterceptor(Interceptor { chain ->
      Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("m")
        .body(object : okhttp3.ResponseBody() {
          override fun contentType() = "audio/mpeg".toMediaType()
          override fun contentLength() = declaredLength
          override fun source() = body.toResponseBody().source()
        }).build()
    }).build()

  private fun repo(http: OkHttpClient, allowed: () -> Boolean = { true }) = DownloadRepository(
    dao, dir, http, resolveUrl = { it.streamUrl }, allowedNow = allowed,
    scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
  )

  private fun awaitIdle(r: DownloadRepository, key: String) = runBlocking {
    withTimeout(5_000) { while (r.progress.value[key] is DownloadProgress.Queued || r.progress.value[key] is DownloadProgress.Active || (r.progress.value[key] == null && dao.rows.value.isEmpty() && dir.list().isNullOrEmpty())) delay(20) }
    delay(100)
  }

  @Test fun completedDownloadIsSavedAndLocatable() {
    val r = repo(client(ByteArray(2048) { 1 }))
    r.download(track)
    awaitIdle(r, track.key)
    val f = runBlocking { r.localFile(track) }
    assertNotNull(f)
    assertEquals(2048L, f!!.length())
    assertEquals(1, dao.rows.value.size)
    assertTrue(dir.listFiles()!!.none { it.name.endsWith(".part") })
  }

  @Test fun truncatedDownloadLeavesNoFileAndFails() {
    val r = repo(client(ByteArray(100), declaredLength = 5000))
    r.download(track)
    runBlocking { withTimeout(5_000) { while (r.progress.value[track.key] !is DownloadProgress.Failed) delay(20) } }
    assertTrue(dao.rows.value.isEmpty())
    assertTrue(dir.listFiles()!!.isEmpty())
  }

  @Test fun httpErrorFailsWithoutSaving() {
    val r = repo(client(ByteArray(10), code = 403))
    r.download(track)
    runBlocking { withTimeout(5_000) { while (r.progress.value[track.key] !is DownloadProgress.Failed) delay(20) } }
    assertTrue(dao.rows.value.isEmpty())
  }

  @Test fun missingFileDropsItsDeadRow() {
    val r = repo(client(ByteArray(10)))
    runBlocking { dao.upsert(DownloadEntity(track.key, "AUDIUS", "abc", "Song", "Artist", null, null, null, "gone.audio", 10, 0)) }
    assertNull(runBlocking { r.localFile(track) })
    assertTrue(dao.rows.value.isEmpty())
  }

  @Test fun deleteRemovesFileAndRow() {
    val r = repo(client(ByteArray(64)))
    r.download(track)
    awaitIdle(r, track.key)
    runBlocking { r.delete(track) }
    assertTrue(dao.rows.value.isEmpty())
    assertFalse(dir.listFiles()!!.any { it.name.endsWith(".audio") })
  }

  @Test fun wifiOnlyBlocksDownloadsOnMeteredConnections() {
    var wifi = false
    val r = repo(client(ByteArray(64)), allowed = { wifi })
    r.download(track)
    assertTrue(r.progress.value[track.key] is DownloadProgress.Failed)
    assertTrue(dao.rows.value.isEmpty())
    // Back on Wi-Fi: dismissing the failure and retrying works.
    wifi = true
    r.dismissFailure(track)
    r.download(track)
    awaitIdle(r, track.key)
    assertEquals(1, dao.rows.value.size)
  }

  @Test fun radioAndLocalTracksAreNotDownloadable() {
    val r = repo(client(ByteArray(1)))
    assertFalse(r.canDownload(track.copy(source = Source.RADIO_BROWSER)))
    assertFalse(r.canDownload(track.copy(source = Source.LOCAL_DOWNLOAD)))
    assertTrue(r.canDownload(track))
  }
}
