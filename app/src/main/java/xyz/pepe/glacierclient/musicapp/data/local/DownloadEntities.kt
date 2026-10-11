package xyz.pepe.glacierclient.musicapp.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import xyz.pepe.glacierclient.musicapp.domain.model.Source
import xyz.pepe.glacierclient.musicapp.domain.model.Track

/** A track whose audio file is fully saved on this device. Rows exist only for finished downloads. */
@Entity(tableName = "downloads")
data class DownloadEntity(
  @PrimaryKey val key: String,
  val source: String,
  val sourceId: String,
  val title: String,
  val artist: String,
  val artworkUrl: String?,
  val durationMs: Long?,
  val shareUrl: String?,
  val fileName: String,
  val sizeBytes: Long,
  val downloadedAt: Long,
) {
  fun toTrack() = Track(
    source = runCatching { Source.valueOf(source) }.getOrDefault(Source.YOUTUBE_MUSIC),
    sourceId = sourceId, title = title, artist = artist, artworkUrl = artworkUrl,
    durationMs = durationMs, isPreview = false, shareUrl = shareUrl,
  )
}

@Dao
interface DownloadDao {
  @Upsert suspend fun upsert(d: DownloadEntity)
  @Query("SELECT * FROM downloads ORDER BY downloadedAt DESC") fun observeAll(): Flow<List<DownloadEntity>>
  @Query("SELECT * FROM downloads") suspend fun getAll(): List<DownloadEntity>
  @Query("SELECT * FROM downloads WHERE `key` = :key") suspend fun get(key: String): DownloadEntity?
  @Query("DELETE FROM downloads WHERE `key` = :key") suspend fun delete(key: String)
  @Query("DELETE FROM downloads") suspend fun deleteAll()
}
