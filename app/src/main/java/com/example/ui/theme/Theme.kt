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

val MonarchColorScheme = darkColorScheme(
    primary = MonarchPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF292344),
    onPrimaryContainer = Color(0xFFDED8FF),

    secondary = Color(0xFF8F99AE),
    onSecondary = Color(0xFF10131A),
    secondaryContainer = Color(0xFF222836),
    onSecondaryContainer = Color(0xFFDCE1EC),

    tertiary = MonarchBlue,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF1D304F),
    onTertiaryContainer = Color(0xFFD7E5FF),

    background = MonarchBackground,
    onBackground = MonarchTextPrimary,

    surface = MonarchSurface,
    onSurface = MonarchTextPrimary,

    surfaceVariant = MonarchSurfaceElevated,
    onSurfaceVariant = MonarchTextSecondary,

    outline = Color(0xFF3A4358),
    outlineVariant = MonarchBorder,

    error = MonarchError,
    onError = Color.White,
    errorContainer = Color(0xFF45252B),
    onErrorContainer = Color(0xFFFFD9DD)
)

private val ShadowMonarchColorScheme = MonarchColorScheme


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
