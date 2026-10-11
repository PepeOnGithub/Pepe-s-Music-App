package xyz.pepe.glacierclient.musicapp.ui.util

import android.content.Context

/** Clears the app's real cache directories — Coil's disk image cache and the M3U export scratch
 * folder — not the playlists/likes database, which lives in separate persistent storage. */
fun clearAppCache(context: Context) {
  runCatching { context.cacheDir.deleteRecursively() }
  runCatching { context.externalCacheDir?.deleteRecursively() }
}
