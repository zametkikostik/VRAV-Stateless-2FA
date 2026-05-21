package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme =
  lightColorScheme(
    primary = CyberPurple,
    secondary = CyberTeal,
    tertiary = CyberEmerald,
    background = DarkBg,
    surface = DarkSurface,
    onBackground = Slate100,
    onSurface = Slate100,
    surfaceVariant = DarkSurfaceCard,
    onSurfaceVariant = Slate300
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Use Light Theme as default for High Density
  dynamicColor: Boolean = false, // Disable dynamic color to maintain strict thematic brand consistency
  content: @Composable () -> Unit,
) {
  val colorScheme = LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
