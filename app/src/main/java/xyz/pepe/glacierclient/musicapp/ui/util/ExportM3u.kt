package xyz.pepe.glacierclient.musicapp.ui.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import xyz.pepe.glacierclient.musicapp.domain.model.Track
import java.io.File

/**
 * Exports [tracks] as a real EXTM3U playlist file and opens the share sheet — the standard
 * format every player (VLC, Winamp, Poweramp) can import. Uses each track's real page URL
 * (YouTube Music watch link, Audius permalink, etc.) as the entry, not a fabricated local path,
 * since these are streamed tracks with no file on disk.
 */
fun exportM3u(context: Context, name: String, tracks: List<Track>) {
  if (tracks.isEmpty()) return
  val body = buildString {
    appendLine("#EXTM3U")
    tracks.forEach { t ->
      val seconds = (t.durationMs ?: 0) / 1000
      appendLine("#EXTINF:$seconds,${t.artist} - ${t.title}")
      appendLine(t.shareUrl ?: t.streamUrl ?: "# ${t.title} — no link available")
    }
  }
  val dir = File(context.cacheDir, "exports").apply { mkdirs() }
  val file = File(dir, "${name.replace(Regex("[^A-Za-z0-9_-]"), "_")}.m3u")
  file.writeText(body)
  val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
  val intent = Intent(Intent.ACTION_SEND).apply {
    type = "audio/x-mpegurl"
    putExtra(Intent.EXTRA_STREAM, uri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }
  context.startActivity(Intent.createChooser(intent, "Export playlist").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
