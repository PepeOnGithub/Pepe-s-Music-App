package xyz.pepe.glacierclient.musicapp

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import xyz.pepe.glacierclient.musicapp.di.AppContainer

class MusicApplication : Application(), SingletonImageLoader.Factory {
  lateinit var container: AppContainer
    private set

  override fun onCreate() {
    super.onCreate()
    container = AppContainer(this)
    Thread { container.catalog.warmUp() }.start()
    container.startBackgroundWork()
  }

  override fun newImageLoader(context: PlatformContext): ImageLoader =
    ImageLoader.Builder(context)
      .components { add(OkHttpNetworkFetcherFactory(callFactory = { container.okHttp })) }
      .build()
}
