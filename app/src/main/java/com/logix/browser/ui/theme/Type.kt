package com.logix.browser.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Logix typography: Material3 defaults plus the accent section style used
 * for uppercase settings headings (e.g. "VURGU RENGİ").
 */
val LogixTypography = Typography()

val SectionTitleStyle = TextStyle(
    fontWeight = FontWeight.Bold,
    fontSize = 13.sp,
    letterSpacing = 1.5.sp,
)
