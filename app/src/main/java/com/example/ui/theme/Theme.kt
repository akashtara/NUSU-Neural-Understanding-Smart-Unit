package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = NusuElectricBlue,
    secondary = NusuNeonCyan,
    tertiary = NusuPurpleGlow,
    background = NusuDeepSpaceBlack,
    surface = NusuDarkNavy,
    onPrimary = NusuPureWhite,
    onSecondary = NusuPureWhite,
    onTertiary = NusuPureWhite,
    onBackground = NusuPureWhite,
    onSurface = NusuPureWhite,
  )

@Composable
fun MyApplicationTheme(
  // We force dark theme for NUSU Cyber UI
  darkTheme: Boolean = true,
  // Dynamic color disabled to keep the Cyber UI look
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val activeTheme by ThemeManager.currentTheme.collectAsState()

  val reactiveColorScheme = darkColorScheme(
    primary = activeTheme.primaryColor,
    secondary = activeTheme.secondaryColor,
    tertiary = activeTheme.accentColor,
    background = activeTheme.backgroundColor,
    surface = activeTheme.cardBackgroundColor,
    onPrimary = NusuPureWhite,
    onSecondary = NusuPureWhite,
    onTertiary = NusuPureWhite,
    onBackground = NusuPureWhite,
    onSurface = NusuPureWhite,
  )

  CompositionLocalProvider(LocalNusuTheme provides activeTheme) {
    MaterialTheme(
      colorScheme = reactiveColorScheme,
      typography = Typography,
      content = content
    )
  }
}
