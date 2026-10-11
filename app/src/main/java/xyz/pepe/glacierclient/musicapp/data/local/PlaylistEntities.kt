package xyz.pepe.glacierclient.musicapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import xyz.pepe.glacierclient.musicapp.domain.model.Source
import xyz.pepe.glacierclient.musicapp.domain.model.Track

@Entity(tableName = "playlists")
data class PlaylistEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val createdAt: Long,
  val updatedAt: Long,
)

/** One track in one playlist, at a given [position] — a track can appear in many playlists. */
@Entity(tableName = "playlist_tracks")
data class PlaylistTrackEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val playlistId: Long,
  val position: Int,
  val source: String,
  val sourceId: String,
  val title: String,
  val artist: String,
  val artworkUrl: String?,
  val durationMs: Long?,
  val shareUrl: String?,
) {
  fun toTrack() = Track(
    source = runCatching { Source.valueOf(source) }.getOrDefault(Source.YOUTUBE_MUSIC),
    sourceId = sourceId,
    title = title,
    artist = artist,
    artworkUrl = artworkUrl,
    durationMs = durationMs,
    isPreview = false,
    shareUrl = shareUrl,
  )

  companion object {
    fun from(playlistId: Long, position: Int, track: Track) = PlaylistTrackEntity(
      playlistId = playlistId,
      position = position,
      source = track.source.name,
      sourceId = track.sourceId,
      title = track.title,
      artist = track.artist,
      artworkUrl = track.artworkUrl,
      durationMs = track.durationMs,
      shareUrl = track.shareUrl,
    )
  }
}
