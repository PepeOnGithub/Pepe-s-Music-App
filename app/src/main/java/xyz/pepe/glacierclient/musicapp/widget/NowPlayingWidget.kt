package xyz.pepe.glacierclient.musicapp.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.pepe.glacierclient.musicapp.MainActivity
import xyz.pepe.glacierclient.musicapp.MusicApplication
import xyz.pepe.glacierclient.musicapp.di.AppContainer

private fun containerOf(context: Context): AppContainer = (context.applicationContext as MusicApplication).container

/** Home-screen widget: the current (or last played) track with previous / play-pause / next. */
class NowPlayingWidget : GlanceAppWidget() {
  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val container = containerOf(context)
    val state = container.player.state.value
    val track = state.current ?: container.history.recent.value.firstOrNull()
    val playing = state.isPlaying
    provideContent {
      Column(
        modifier = GlanceModifier.fillMaxSize().background(ColorProvider(Color(0xFF1E1B24))).padding(12.dp)
          .clickable(actionStartActivity(android.content.Intent(context, MainActivity::class.java))),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          track?.title ?: "Nothing playing",
          style = TextStyle(color = ColorProvider(Color.White), fontWeight = FontWeight.Bold, fontSize = 16.sp),
          maxLines = 1,
        )
        Text(
          track?.artist ?: "Open Pepe's Music",
          style = TextStyle(color = ColorProvider(Color(0xFFCAC4D0)), fontSize = 13.sp),
          maxLines = 1,
        )
        Spacer(GlanceModifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Button("Prev", actionRunCallback<PreviousAction>())
          Spacer(GlanceModifier.width(8.dp))
          Button(if (playing) "Pause" else "Play", actionRunCallback<PlayPauseAction>())
          Spacer(GlanceModifier.width(8.dp))
          Button("Next", actionRunCallback<NextAction>())
        }
      }
    }
  }
}

class NowPlayingWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = NowPlayingWidget()
}

class PlayPauseAction : ActionCallback {
  override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
    withContext(Dispatchers.Main) { containerOf(context).player.togglePlayPause() }
  }
}

class NextAction : ActionCallback {
  override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
    withContext(Dispatchers.Main) { containerOf(context).player.next() }
  }
}

class PreviousAction : ActionCallback {
  override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
    withContext(Dispatchers.Main) { containerOf(context).player.previous() }
  }
}
