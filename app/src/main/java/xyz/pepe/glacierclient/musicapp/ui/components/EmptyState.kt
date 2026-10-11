@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package xyz.pepe.glacierclient.musicapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Illustration built from two [MaterialShapes] plus an icon; used by every empty list. */
@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.padding(horizontal = 32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
      Box(
        Modifier.size(160.dp).clip(MaterialShapes.Cookie9Sided.toShape())
          .background(MaterialTheme.colorScheme.primaryContainer),
      )
      Box(
        Modifier.size(84.dp).clip(MaterialShapes.Clover4Leaf.toShape())
          .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
      ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
      }
    }
    Text(title, style = MaterialTheme.typography.titleLargeEmphasized, textAlign = TextAlign.Center)
    Text(
      body,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}
