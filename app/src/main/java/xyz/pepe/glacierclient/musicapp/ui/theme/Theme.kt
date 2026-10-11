@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package xyz.pepe.glacierclient.musicapp.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val BrandDarkColors = darkColorScheme(
  primary = BrandGreen,
  onPrimary = OnBrandGreen,
  primaryContainer = PrimaryContainer,
  onPrimaryContainer = OnPrimaryContainer,
  secondary = Secondary,
  onSecondary = OnBrandGreen,
  secondaryContainer = SecondaryContainer,
  onSecondaryContainer = OnSecondaryContainer,
  tertiary = Tertiary,
  onTertiary = OnBrandGreen,
  background = Surface,
  surface = Surface,
  surfaceContainerLowest = Surface,
  surfaceContainerLow = SurfaceContainerLow,
  surfaceContainer = SurfaceContainer,
  surfaceContainerHigh = SurfaceContainerHigh,
  surfaceContainerHighest = SurfaceContainerHighest,
  onSurface = OnSurface,
  onBackground = OnSurface,
  onSurfaceVariant = OnSurfaceVariant,
  outlineVariant = OutlineVariant,
  error = ErrorRed,
  surfaceVariant = Color(0xFF262626),
  surfaceDim = Color(0xFF000000),
  surfaceBright = Color(0xFF2C2C2C),
  outline = Color(0xFF8C8C8C),
  inversePrimary = Color(0xFF1C3D1D),
  tertiaryContainer = Color(0xFF5B4300),
  onTertiaryContainer = Color(0xFFFFDF9E),
)

val AppShapes = Shapes(
  extraSmall = RoundedCornerShape(8.dp),
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(20.dp),
  large = RoundedCornerShape(28.dp),
  extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun PepesMusicTheme(
  wallpaperColors: Boolean = false,
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val scheme =
    if (wallpaperColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(context)
    else BrandDarkColors
  MaterialExpressiveTheme(
    colorScheme = scheme,
    motionScheme = MotionScheme.expressive(),
    shapes = AppShapes,
    typography = AppTypography,
    content = content,
  )
}
