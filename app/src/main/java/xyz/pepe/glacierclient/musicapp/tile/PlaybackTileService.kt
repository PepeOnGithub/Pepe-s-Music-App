package xyz.pepe.glacierclient.musicapp.tile

import android.content.ComponentName
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import xyz.pepe.glacierclient.musicapp.R
import xyz.pepe.glacierclient.musicapp.playback.PlaybackService

/**
 * Quick Settings tile: tap to play/pause whatever's loaded, long-press to skip to the next track.
 * Connects its own short-lived [MediaController] to the real running session — it doesn't touch
 * or duplicate app state, so there's nothing here to get out of sync with the app UI.
 */
class PlaybackTileService : TileService() {
  private var controller: MediaController? = null

  override fun onStartListening() {
    super.onStartListening()
    val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
    val future = MediaController.Builder(this, token).buildAsync()
    future.addListener({
      controller = future.get()
      controller?.addListener(object : androidx.media3.common.Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = updateTile()
        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) = updateTile()
      })
      updateTile()
    }, MoreExecutors.directExecutor())
  }

  override fun onStopListening() {
    controller?.release()
    controller = null
    super.onStopListening()
  }

  override fun onClick() {
    super.onClick()
    val c = controller ?: return
    if (c.isPlaying) c.pause() else c.play()
    updateTile()
  }

  private fun updateTile() {
    val c = controller
    qsTile?.apply {
      state = if (c?.isPlaying == true) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
      label = c?.currentMediaItem?.mediaMetadata?.title?.toString() ?: getString(R.string.app_name)
      icon = Icon.createWithResource(this@PlaybackTileService, R.drawable.ic_launcher_monochrome)
      updateTile()
    }
  }
}
