package xyz.pepe.glacierclient.musicapp.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import android.net.Uri
import xyz.pepe.glacierclient.musicapp.domain.model.Track

/** Builds a Media3 [MediaItem] from a [Track] whose [Track.streamUrl] has already been resolved. */
fun Track.toMediaItem(): MediaItem {
  val metadata = MediaMetadata.Builder()
    .setTitle(title)
    .setArtist(artist)
    .setArtworkUri(artworkUrl?.let { Uri.parse(it) })
    .setIsPlayable(true)
    .build()
  return MediaItem.Builder()
    .setMediaId(key)
    .setUri(streamUrl)
    .setMediaMetadata(metadata)
    .build()
}
