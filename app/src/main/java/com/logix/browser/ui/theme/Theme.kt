package com.logix.browser.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AmoledBlack = Color(0xFF000000)
val LogixSurface = Color(0xFF121212)
val LogixSurfaceVariant = Color(0xFF1A1A1A)
val LogixOnSurfaceVariant = Color(0xFFB3B3B3)
val LogixDefaultAccent = Color(0xFF2196F3)

/**
 * Logix theme: AMOLED black background, dark-grey surfaces (pure black in
 * AMOLED mode) and a user-selectable dynamic accent color. Incognito mode
 * switches the whole shell to coal-grey / neon violet-blue tones.
 */
@Composable
fun LogixTheme(
    darkTheme: Boolean = true,
    amoled: Boolean = false,
    accent: Color = LogixDefaultAccent,
    incognito: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkColors = darkColorScheme(
        primary = accent,
        onPrimary = Color.White,
        background = AmoledBlack,
        onBackground = Color.White,
        surface = if (amoled) AmoledBlack else LogixSurface,
        onSurface = Color.White,
        surfaceVariant = if (amoled) AmoledBlack else LogixSurfaceVariant,
        onSurfaceVariant = LogixOnSurfaceVariant,
        surfaceContainerLowest = AmoledBlack,
        surfaceContainerLow = if (amoled) AmoledBlack else LogixSurface,
        surfaceContainer = if (amoled) AmoledBlack else LogixSurfaceVariant,
        surfaceContainerHigh = if (amoled) Color(0xFF0A0A0A) else Color(0xFF222222),
    )
    val lightColors = lightColorScheme(
        primary = accent,
        onPrimary = Color.White,
        background = Color(0xFFFFFFFF),
        onBackground = Color(0xFF1A1A1A),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF1A1A1A),
        surfaceVariant = Color(0xFFF1F1F1),
        onSurfaceVariant = Color(0xFF4A4A4A),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF7F7F7),
        surfaceContainer = Color(0xFFF1F1F1),
        surfaceContainerHigh = Color(0xFFE8E8E8),
    )
    val incognitoColors = darkColorScheme(
        primary = Color(0xFF8B7CFF),
        onPrimary = Color.White,
        background = Color(0xFF0E0E14),
        onBackground = Color.White,
        surface = Color(0xFF1A1B26),
        onSurface = Color.White,
        surfaceVariant = Color(0xFF232433),
        onSurfaceVariant = Color(0xFFB9B9D6),
        surfaceContainerLowest = Color(0xFF0E0E14),
        surfaceContainerLow = Color(0xFF1A1B26),
        surfaceContainer = Color(0xFF232433),
        surfaceContainerHigh = Color(0xFF2C2D40),
    )
    androidx.compose.material3.MaterialTheme(
        colorScheme = when {
            incognito -> incognitoColors
            darkTheme -> darkColors
            else -> lightColors
        },
        typography = LogixTypography,
        content = content,
    )
}
