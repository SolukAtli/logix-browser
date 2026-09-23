package com.logix.browser.settings.ui

import androidx.compose.ui.graphics.Color

/**
 * User-selectable accent palette. [BrowserSettings.accent] holds either a
 * preset [AccentOption.key] or a custom `#RRGGBB` hex string.
 */
data class AccentOption(
    val key: String,
    val label: String,
    val color: Color,
)

object AccentPalette {

    val Orange = AccentOption("orange", "Turuncu", Color(0xFFFF5722))
    val Green = AccentOption("green", "Yeşil", Color(0xFF8BC34A))
    val Purple = AccentOption("purple", "Mor", Color(0xFF7C3AED))
    val Blue = AccentOption("blue", "Mavi", Color(0xFF2196F3))
    val Red = AccentOption("red", "Kırmızı", Color(0xFFF44336))
    val Pink = AccentOption("pink", "Pembe", Color(0xFFE91E63))
    val Cyan = AccentOption("cyan", "Camgöbeği", Color(0xFF00BCD4))
    val Yellow = AccentOption("yellow", "Sarı", Color(0xFFFFC107))

    val PRESETS: List<AccentOption> =
        listOf(Orange, Green, Purple, Blue, Red, Pink, Cyan, Yellow)

    const val DEFAULT_KEY = "blue"

    private val HEX_REGEX = Regex("^#?([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")

    fun colorFor(value: String): Color {
        PRESETS.firstOrNull { it.key == value }?.let { return it.color }
        return parseHex(value) ?: Blue.color
    }

    fun labelFor(value: String): String {
        PRESETS.firstOrNull { it.key == value }?.let { return it.label }
        return if (parseHex(value) != null) "Özel" else Blue.label
    }

    /** Returns null when [value] is not a valid hex color. */
    fun parseHex(value: String): Color? {
        val match = HEX_REGEX.matchEntire(value.trim()) ?: return null
        return try {
            val hex = match.groupValues[1]
            val argb = if (hex.length == 6) "FF$hex" else hex
            Color(argb.toLong(16))
        } catch (e: NumberFormatException) {
            null
        }
    }
}
