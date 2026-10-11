// CANONICAL COPY — edit only in pepe-ecosystem/, then run pepe-ecosystem/sync.sh.
package xyz.pepe.glacierclient.ecosystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

/**
 * Cross-app switcher: every installed sibling app, one tap to open. Apps that aren't installed
 * are simply not listed — no dead rows. Drop into any Settings/About page.
 */
@Composable
fun PepeAppsSection(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val apps = remember { PepeEcosystem.installedSiblings(context) }
  if (apps.isEmpty()) return
  Column(modifier) {
    Text(
      "More Pepe apps",
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).semantics { heading() },
    )
    apps.forEach { app ->
      val icon = remember(app) {
        runCatching { context.packageManager.getApplicationIcon(app.packageName).toBitmap(96, 96).asImageBitmap() }.getOrNull()
      }
      ListItem(
        headlineContent = { Text(app.label) },
        supportingContent = { Text("Open", color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = icon?.let { { Image(it, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))) } },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(role = Role.Button, onClickLabel = "Open ${app.label}") {
          PepeEcosystem.send(context, PepeEcosystem.Open(app))
        },
      )
    }
  }
}
