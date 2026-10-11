package xyz.pepe.glacierclient.musicapp.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xyz.pepe.glacierclient.musicapp.data.local.PlaylistDao
import xyz.pepe.glacierclient.musicapp.data.local.PlaylistEntity
import xyz.pepe.glacierclient.musicapp.data.local.PlaylistTrackEntity
import xyz.pepe.glacierclient.musicapp.domain.model.Playlist
import xyz.pepe.glacierclient.musicapp.domain.model.Source
import xyz.pepe.glacierclient.musicapp.domain.model.Track
import xyz.pepe.glacierclient.musicapp.domain.util.PlaylistTransfer
import org.json.JSONArray
import org.json.JSONObject

/** Room-backed playlists — the app's first real relational storage, replacing the flat
 * SharedPreferences+JSON pattern for anything that needs actual queries (ordering, membership
 * checks) rather than just "load the whole list, save the whole list". */
class PlaylistRepository(private val dao: PlaylistDao) {
  fun observePlaylists(): Flow<List<Playlist>> = dao.observePlaylistsWithCount().map { rows ->
    rows.map { r -> Playlist(id = r.playlist.id, name = r.playlist.name, trackCount = r.trackCount, updatedAt = r.playlist.updatedAt) }
  }

  fun observeTracks(playlistId: Long): Flow<List<PlaylistTrackEntity>> = dao.observeTracks(playlistId)

  suspend fun create(name: String): Long {
    val now = System.currentTimeMillis()
    return dao.insertPlaylist(PlaylistEntity(name = name.trim(), createdAt = now, updatedAt = now))
  }

  suspend fun rename(playlistId: Long, name: String) {
    dao.renamePlaylist(playlistId, name.trim(), System.currentTimeMillis())
  }

  suspend fun delete(playlistId: Long) {
    dao.deletePlaylist(playlistId)
    dao.clearPlaylistTracks(playlistId)
  }

  suspend fun addTrack(playlistId: Long, track: Track): Boolean {
    if (dao.containsTrack(playlistId, track.source.name, track.sourceId)) return false
    val position = dao.nextPosition(playlistId)
    dao.insertTrack(PlaylistTrackEntity.from(playlistId, position, track))
    dao.touch(playlistId, System.currentTimeMillis())
    return true
  }

  suspend fun removeTrack(playlistId: Long, rowId: Long) {
    dao.deleteTrack(rowId)
    dao.touch(playlistId, System.currentTimeMillis())
  }

  suspend fun reorder(playlistId: Long, orderedRowIds: List<Long>) = dao.reorder(playlistId, orderedRowIds)

  /** Up to four distinct cover images per playlist, for the 2x2 mosaic thumbnail. */
  fun observeCovers(): Flow<Map<Long, List<String>>> = dao.observeCoverRows().map { rows ->
    rows.groupBy { it.playlistId }.mapValues { (_, list) -> list.mapNotNull { it.artworkUrl }.distinct().take(4) }
  }

  suspend fun tracks(playlistId: Long): List<Track> = dao.getTracks(playlistId).map { it.toTrack() }

  /** Copies a playlist under [newName] and returns the new playlist's id. */
  suspend fun duplicate(playlistId: Long, newName: String): Long {
    val id = create(newName)
    dao.getTracks(playlistId).forEach { addTrack(id, it.toTrack()) }
    return id
  }

  /** Adds every track of [fromId] that [intoId] doesn't have yet; returns how many were added. */
  suspend fun merge(fromId: Long, intoId: Long): Int {
    var added = 0
    dao.getTracks(fromId).forEach { if (addTrack(intoId, it.toTrack())) added++ }
    return added
  }

  suspend fun sort(playlistId: Long, sort: PlaylistTransfer.Sort) {
    val rows = dao.getTracks(playlistId).map { PlaylistTransfer.Quad(it.id, it.title, it.artist, it.durationMs) }
    dao.reorder(playlistId, PlaylistTransfer.sortedIds(rows, sort))
  }

  /** The playlist as portable JSON text (name plus full track details). */
  suspend fun exportJson(playlistId: Long): String {
    val name = dao.getPlaylist(playlistId)?.name ?: "Playlist"
    val arr = JSONArray()
    dao.getTracks(playlistId).forEach { r ->
      arr.put(
        JSONObject().put("source", r.source).put("sourceId", r.sourceId).put("title", r.title).put("artist", r.artist)
          .put("artworkUrl", r.artworkUrl ?: "").put("durationMs", r.durationMs ?: 0L).put("shareUrl", r.shareUrl ?: ""),
      )
    }
    return JSONObject().put("app", "pepes-music").put("name", name).put("tracks", arr).toString(2)
  }

  /**
   * Creates a playlist from JSON produced by [exportJson]. Returns the new id and track count, or
   * null if [text] isn't a playlist export.
   */
  suspend fun importJson(text: String): Pair<Long, Int>? {
    val o = runCatching { JSONObject(text.trim()) }.getOrNull() ?: return null
    val arr = o.optJSONArray("tracks") ?: return null
    val id = create(o.optString("name").ifBlank { "Imported playlist" })
    var n = 0
    for (i in 0 until arr.length()) {
      val t = FavoritesRepository.trackFrom(arr.optJSONObject(i)) ?: continue
      if (addTrack(id, t)) n++
    }
    return id to n
  }

  /**
   * Creates a playlist from pasted M3U text. Links to YouTube videos map straight to a track; the
   * rest are looked up by "artist title" via [lookup]. Entries that can't be matched are skipped.
   */
  suspend fun importM3u(name: String, text: String, lookup: suspend (String) -> Track?): Pair<Long, Int>? {
    val entries = PlaylistTransfer.parseM3u(text)
    if (entries.isEmpty()) return null
    val id = create(name.ifBlank { "Imported playlist" })
    var n = 0
    for (e in entries) {
      val ytId = PlaylistTransfer.youtubeId(e.url)
      val track = if (ytId != null) {
        Track(Source.YOUTUBE_MUSIC, ytId, e.title.ifBlank { "Track" }, e.artist, shareUrl = e.url)
      } else lookup("${e.artist} ${e.title}".trim())
      if (track != null && addTrack(id, track)) n++
    }
    return id to n
  }
}
