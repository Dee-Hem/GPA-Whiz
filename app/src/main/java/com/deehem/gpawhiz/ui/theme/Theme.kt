package com.deehem.gpawhiz.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = BlueOnPrimary,
    primaryContainer = BluePrimaryContainer,
    onPrimaryContainer = BlueOnPrimaryContainer,
    secondary = GreenSecondary,
    onSecondary = GreenOnSecondary,
    secondaryContainer = GreenSecondaryContainer,
    onSecondaryContainer = GreenOnSecondaryContainer,
    tertiary = YellowTertiary,
    background = GrayBackground,
    surface = GraySurface,
    onSurface = Color(0xFF1F1F1F),
    surfaceVariant = GraySurfaceVariant,
    onSurfaceVariant = Color(0xFF44474E),
    outline = GrayOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF004494),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = GreenSecondary,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF00532C),
    onSecondaryContainer = Color(0xFFE7F5ED),
    tertiary = YellowTertiary,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF44474E),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099)
)

@Composable
fun MyApplicationTheme(
    themeMode: Int = 0, // 0: System, 1: Light, 2: Dark
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
