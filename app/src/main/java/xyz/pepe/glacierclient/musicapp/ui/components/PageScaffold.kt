@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package xyz.pepe.glacierclient.musicapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.pepe.glacierclient.musicapp.R

/**
 * Full-screen page with a collapsing [LargeFlexibleTopAppBar]. Every top-level page uses this so
 * they all share the same scroll behaviour and look.
 */
@Composable
fun PageScaffold(
  title: String,
  modifier: Modifier = Modifier,
  icon: (@Composable () -> Unit)? = null,
  onBack: (() -> Unit)? = null,
  actions: @Composable RowScope.() -> Unit = {},
  floatingActionButton: @Composable () -> Unit = {},
  subtitle: String? = null,
  compact: Boolean = false,
  content: @Composable (PaddingValues) -> Unit,
) {
  val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  Scaffold(
    modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      if (compact) {
        androidx.compose.material3.TopAppBar(
          title = {},
          navigationIcon = { if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back)) } },
          actions = actions,
          colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        )
      } else LargeFlexibleTopAppBar(
        title = {
          if (icon != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              icon()
              Text(title)
            }
          } else {
            Text(title)
          }
        },
        subtitle = if (subtitle != null) { { Text(subtitle) } } else { {} },
        navigationIcon = {
          if (onBack != null) {
            IconButton(onClick = onBack) {
              Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
            }
          }
        },
        actions = actions,
        scrollBehavior = scroll,
      )
    },
    floatingActionButton = floatingActionButton,
    content = content,
  )
}
