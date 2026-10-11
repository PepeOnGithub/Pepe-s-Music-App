package xyz.pepe.glacierclient.musicapp.domain.util

import xyz.pepe.glacierclient.musicapp.domain.model.RepeatMode
import kotlin.random.Random

object QueueOps {
  /** Shuffles [queue] keeping the item at [currentIndex] first; original order is preserved for unshuffle. */
  fun <T> shuffle(queue: List<T>, currentIndex: Int, seed: Long): List<T> {
    if (queue.isEmpty()) return queue
    val current = queue[currentIndex]
    val rest = queue.filterIndexed { i, _ -> i != currentIndex }.shuffled(Random(seed))
    return listOf(current) + rest
  }

  fun next(index: Int, size: Int, repeat: RepeatMode): Int? {
    if (size == 0) return null
    return when (repeat) {
      RepeatMode.ONE -> index
      RepeatMode.ALL -> (index + 1) % size
      RepeatMode.OFF -> if (index + 1 < size) index + 1 else null
    }
  }

  /** Previous restarts the current track if playback is more than 3s in; otherwise moves back. */
  fun previous(index: Int, positionMs: Long, size: Int, repeat: RepeatMode): Int {
    if (positionMs > 3000) return index
    if (size == 0) return index
    return when (repeat) {
      RepeatMode.ONE -> index
      RepeatMode.ALL -> if (index - 1 >= 0) index - 1 else size - 1
      RepeatMode.OFF -> if (index - 1 >= 0) index - 1 else 0
    }
  }

  /** Moves an item and returns the new queue plus the adjusted current index. */
  fun <T> move(queue: List<T>, currentIndex: Int, from: Int, to: Int): Pair<List<T>, Int> {
    val mutable = queue.toMutableList()
    val item = mutable.removeAt(from)
    mutable.add(to, item)
    val newCurrent = when {
      currentIndex == from -> to
      from < currentIndex && to >= currentIndex -> currentIndex - 1
      from > currentIndex && to <= currentIndex -> currentIndex + 1
      else -> currentIndex
    }
    return mutable to newCurrent
  }

  /** Removes an item and returns the new queue plus the adjusted current index. */
  fun <T> remove(queue: List<T>, currentIndex: Int, index: Int): Pair<List<T>, Int> {
    val mutable = queue.toMutableList()
    mutable.removeAt(index)
    val newCurrent = when {
      index < currentIndex -> currentIndex - 1
      index == currentIndex -> currentIndex.coerceAtMost(mutable.lastIndex)
      else -> currentIndex
    }
    return mutable to newCurrent
  }
}
