@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package xyz.pepe.glacierclient.musicapp.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle

val AppTypography = Typography()

/** Hero clock digits: emphasized display style with tabular figures so digits never jitter. */
@Composable
fun clockDigitStyle(): TextStyle =
  MaterialTheme.typography.displayLargeEmphasized.copy(fontFeatureSettings = "tnum")

@Composable
fun tabularStyle(base: TextStyle): TextStyle = base.copy(fontFeatureSettings = "tnum")
