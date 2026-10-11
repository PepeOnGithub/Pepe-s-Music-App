package xyz.pepe.glacierclient.musicapp.ui.util

import android.content.Context
import android.content.Intent
import xyz.pepe.glacierclient.musicapp.domain.model.Track

/** Shares the track's real source URL (YouTube Music watch link, Audius page, etc.) — never a
 * fabricated deep link, since only YouTube Music tracks in this app carry a [Track.shareUrl]. */
fun shareTrack(context: Context, track: Track) {
  val text = if (track.shareUrl != null) "${track.title} — ${track.artist}\n${track.shareUrl}" else "${track.title} — ${track.artist}"
  val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
  context.startActivity(Intent.createChooser(intent, "Share song").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
