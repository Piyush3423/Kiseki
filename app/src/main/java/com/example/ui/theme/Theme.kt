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

import androidx.compose.ui.graphics.Color
import com.example.data.repository.ThemeMode

private val ShadowMonarchColorScheme =
  darkColorScheme(
    primary = Color(0xFF8B5CF6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF231438),
    onPrimaryContainer = Color(0xFFE9D5FF),
    secondary = Color(0xFF3B82F6),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color(0xFF93C5FD),
    background = Color(0xFF07080D),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF0E101A),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF181B2E),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E293B)
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondaryContainer = Color(0xFF333842),
    onSecondaryContainer = Color(0xFFE1E2EC),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF191C22),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF282F38),
    onSurfaceVariant = Color(0xFFC2C7CF),
    outline = Color(0xFF43474E)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline
  )

@Composable
fun MyApplicationTheme(
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val darkTheme = when (themeMode) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK, ThemeMode.SHADOW_MONARCH -> true
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
  }

  val colorScheme =
    when {
      themeMode == ThemeMode.SHADOW_MONARCH -> ShadowMonarchColorScheme
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
