// presentation/theme/Theme.kt
package com.example.t_learnappmobile.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val YellowPrimary = Color(0xFFFFC107)
val YellowDark = Color(0xFFFFB300)
val RedError = Color(0xFFF44336)
val RedDark = Color(0xFFD32F2F)
val BrownColor = Color(0xFF795548)
val BrownLight = Color(0xFF8D6E63)
val BlueColor = Color(0xFF1976D2)
val BlueLight = Color(0xFF42A5F5)
val LightGray = Color(0xFFE0E0E0)
val MediumGray = Color(0xFF9E9E9E)
val DarkGray = Color(0xFF424242)
val BackgroundLight = Color(0xFFFAFAFA)
val BackgroundDark = Color(0xFF121212)
val SurfaceLight = Color.White
val SurfaceDark = Color(0xFF1E1E1E)

private val LightColorScheme = lightColorScheme(
    primary = YellowPrimary,
    secondary = BlueColor,
    tertiary = BrownColor,
    background = BackgroundLight,
    surface = SurfaceLight,
    error = RedError,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = YellowPrimary,
    secondary = BlueLight,
    tertiary = BrownLight,
    background = BackgroundDark,
    surface = SurfaceDark,
    error = RedError,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.White
)

@Composable
fun TLearnAppMobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}