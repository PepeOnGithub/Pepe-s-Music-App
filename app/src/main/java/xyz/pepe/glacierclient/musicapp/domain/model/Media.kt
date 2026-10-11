package xyz.pepe.glacierclient.musicapp.domain.model

/** Every source here streams full tracks (or live radio), never 30s previews. */
enum class Source { YOUTUBE_MUSIC, AUDIUS, INTERNET_ARCHIVE, CCMIXTER, RADIO_BROWSER, LOCAL_DOWNLOAD }

data class Track(
  val source: Source,
  val sourceId: String,
  val title: String,
  val artist: String,
  val artistId: String? = null,
  val album: String? = null,
  val artworkUrl: String? = null,
  val durationMs: Long? = null,
  val streamUrl: String? = null,
  val isPreview: Boolean = false,
  val license: String? = null,
  val shareUrl: String? = null,
) {
  val key get() = "${source.name}:$sourceId"
}

data class Station(
  val uuid: String,
  val name: String,
  val streamUrl: String,
  val faviconUrl: String? = null,
  val country: String? = null,
  val countryCode: String? = null,
  val tags: List<String> = emptyList(),
  val codec: String? = null,
  val bitrate: Int? = null,
  val homepage: String? = null,
) {
  /** Live radio plays through the same [Track]/[PlayerController] pipeline as everything else. */
  fun toTrack() = Track(
    source = Source.RADIO_BROWSER,
    sourceId = uuid,
    title = name,
    artist = country ?: "Live radio",
    artworkUrl = faviconUrl,
    streamUrl = streamUrl,
    isPreview = false,
    shareUrl = homepage,
  )
}

data class Artist(val source: Source, val id: String, val name: String, val imageUrl: String? = null, val mbid: String? = null)

data class Album(val source: Source, val id: String, val title: String, val artist: String, val coverUrl: String? = null, val year: Int? = null)

data class Playlist(val id: Long, val name: String, val trackCount: Int, val coverUrls: List<String> = emptyList(), val updatedAt: Long = 0L)

data class QueueItem(val track: Track, val addedAt: Long = System.currentTimeMillis())
