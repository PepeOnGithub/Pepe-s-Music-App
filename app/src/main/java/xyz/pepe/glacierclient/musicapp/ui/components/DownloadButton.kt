package xyz.pepe.glacierclient.musicapp.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import xyz.pepe.glacierclient.musicapp.data.repository.DownloadProgress
import xyz.pepe.glacierclient.musicapp.di.AppContainer
import xyz.pepe.glacierclient.musicapp.domain.model.Track

/** One button, four states: download / progress (tap cancels) / downloaded (tap removes) / failed (tap retries). */
@Composable
fun DownloadButton(container: AppContainer, track: Track, modifier: Modifier = Modifier) {
  val repo = container.downloads
  if (!repo.canDownload(track)) return
  val saved by repo.downloads.collectAsState(initial = emptyList())
  val progress by repo.progress.collectAsState()
  val scope = rememberCoroutineScope()
  val isSaved = saved.any { it.key == track.key }
  val p = progress[track.key]
  IconButton(
    modifier = modifier,
    onClick = {
      when {
        p is DownloadProgress.Queued || p is DownloadProgress.Active -> repo.cancel(track)
        p is DownloadProgress.Failed -> { repo.dismissFailure(track); repo.download(track) }
        isSaved -> scope.launch { repo.delete(track) }
        else -> repo.download(track)
      }
    },
  ) {
    when {
      p is DownloadProgress.Active && p.fraction != null ->
        CircularProgressIndicator(progress = { p.fraction }, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
      p is DownloadProgress.Queued || p is DownloadProgress.Active ->
        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
      p is DownloadProgress.Failed -> Icon(Icons.Rounded.ErrorOutline, contentDescription = "Download failed — tap to retry", tint = MaterialTheme.colorScheme.error)
      isSaved -> Icon(Icons.Rounded.CheckCircle, contentDescription = "Downloaded — tap to remove", tint = MaterialTheme.colorScheme.primary)
      else -> Icon(Icons.Rounded.Download, contentDescription = "Download for offline")
    }
  }
}
