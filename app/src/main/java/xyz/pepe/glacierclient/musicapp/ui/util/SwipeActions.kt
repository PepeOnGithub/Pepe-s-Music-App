package xyz.pepe.glacierclient.musicapp.ui.util

import xyz.pepe.glacierclient.musicapp.di.AppContainer
import xyz.pepe.glacierclient.musicapp.domain.model.Track

/** Swipe-right on a track row: toggle Liked, with an Undo snackbar. */
fun swipeLikeAction(container: AppContainer, track: Track): () -> Unit = {
  val was = container.favorites.liked.value.any { it.key == track.key }
  container.favorites.toggle(track)
  container.messages.tryEmit(
    AppContainer.UndoMessage(if (was) "Removed from Liked" else "Added to Liked") { container.favorites.toggle(track) },
  )
}

/** Swipe-left on a track row: add to the end of the queue, with an Undo snackbar. */
fun swipeQueueAction(container: AppContainer, track: Track): () -> Unit = {
  container.player.addToQueue(track)
  container.messages.tryEmit(
    AppContainer.UndoMessage("Added to queue") {
      val q = container.player.state.value.queue
      val i = q.indexOfLast { it.key == track.key }
      if (i >= 0) container.player.removeFromQueue(i)
    },
  )
}
