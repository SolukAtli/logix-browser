package com.logix.browser.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LogixAccent = Color(0xFF7C3AED)

private val DarkColors = darkColorScheme(
    primary = LogixAccent,
    background = Color(0xFF000000),
    surface = Color(0xFF0C0C0C),
)

private val LightColors = lightColorScheme(
    primary = LogixAccent,
)

/**
 * Logix theme: dark default (legacy web UI parity), light opt-in.
 */
@Composable
fun LogixTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    androidx.compose.material3.MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = LogixTypography,
        content = content,
    )
}
