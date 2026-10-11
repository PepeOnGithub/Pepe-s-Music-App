package xyz.pepe.glacierclient.musicapp.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class PlaylistWithCount(@Embedded val playlist: PlaylistEntity, val trackCount: Int)

@Dao
interface PlaylistDao {
  @Insert
  suspend fun insertPlaylist(playlist: PlaylistEntity): Long

  @Query("UPDATE playlists SET name = :name, updatedAt = :updatedAt WHERE id = :playlistId")
  suspend fun renamePlaylist(playlistId: Long, name: String, updatedAt: Long)

  @Query("DELETE FROM playlists WHERE id = :playlistId")
  suspend fun deletePlaylist(playlistId: Long)

  @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
  suspend fun clearPlaylistTracks(playlistId: Long)

  @Query(
    "SELECT p.*, COUNT(t.id) AS trackCount FROM playlists p " +
      "LEFT JOIN playlist_tracks t ON t.playlistId = p.id GROUP BY p.id ORDER BY p.updatedAt DESC",
  )
  fun observePlaylistsWithCount(): Flow<List<PlaylistWithCount>>

  @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC")
  fun observeTracks(playlistId: Long): Flow<List<PlaylistTrackEntity>>

  @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC")
  suspend fun getTracks(playlistId: Long): List<PlaylistTrackEntity>

  @Query("SELECT * FROM playlist_tracks WHERE artworkUrl IS NOT NULL AND position < 8 ORDER BY playlistId ASC, position ASC")
  fun observeCoverRows(): Flow<List<PlaylistTrackEntity>>

  @Query("SELECT * FROM playlists WHERE id = :playlistId")
  suspend fun getPlaylist(playlistId: Long): PlaylistEntity?

  @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_tracks WHERE playlistId = :playlistId")
  suspend fun nextPosition(playlistId: Long): Int

  @Insert
  suspend fun insertTrack(track: PlaylistTrackEntity)

  @Query("DELETE FROM playlist_tracks WHERE id = :trackRowId")
  suspend fun deleteTrack(trackRowId: Long)

  @Query("SELECT EXISTS(SELECT 1 FROM playlist_tracks WHERE playlistId = :playlistId AND source = :source AND sourceId = :sourceId)")
  suspend fun containsTrack(playlistId: Long, source: String, sourceId: String): Boolean

  @Transaction
  suspend fun reorder(playlistId: Long, orderedRowIds: List<Long>) {
    orderedRowIds.forEachIndexed { index, rowId -> updateTrackPosition(rowId, index) }
    touch(playlistId, System.currentTimeMillis())
  }

  @Query("UPDATE playlist_tracks SET position = :position WHERE id = :rowId")
  suspend fun updateTrackPosition(rowId: Long, position: Int)

  @Query("UPDATE playlists SET updatedAt = :updatedAt WHERE id = :playlistId")
  suspend fun touch(playlistId: Long, updatedAt: Long)
}
