package com.logix.browser.omnibox.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.logix.browser.search.SearchEngineId

/**
 * Vector brand marks for the engine picker (no raster assets needed).
 * Simplified but recognizable renditions of each brand glyph.
 */
@Composable
fun EngineBrandIcon(
    id: SearchEngineId,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
) {
    when (id) {
        SearchEngineId.YANDEX -> Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(size / 5))
                .background(Color(0xFFFC3F1D)),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val stroke = size.toPx() * 0.16f
                val w = this.size.width
                val h = this.size.height
                drawLine(
                    Color.White,
                    Offset(w * 0.28f, h * 0.22f),
                    Offset(w * 0.50f, h * 0.55f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    Color.White,
                    Offset(w * 0.72f, h * 0.22f),
                    Offset(w * 0.50f, h * 0.55f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    Color.White,
                    Offset(w * 0.50f, h * 0.55f),
                    Offset(w * 0.50f, h * 0.80f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        }
        SearchEngineId.BRAVE -> Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(size / 5))
                .background(Color(0xFFFB542B)),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = this.size.width
                val h = this.size.height
                val cx = w / 2f
                val cy = h * 0.54f
                val face = Path().apply {
                    moveTo(cx - w * 0.30f, h * 0.30f)
                    lineTo(cx - w * 0.34f, h * 0.12f)
                    lineTo(cx - w * 0.16f, h * 0.22f)
                    lineTo(cx, h * 0.14f)
                    lineTo(cx + w * 0.16f, h * 0.22f)
                    lineTo(cx + w * 0.34f, h * 0.12f)
                    lineTo(cx + w * 0.30f, h * 0.30f)
                    cubicTo(cx + w * 0.34f, h * 0.62f, cx + w * 0.18f, h * 0.82f, cx, h * 0.82f)
                    cubicTo(cx - w * 0.18f, h * 0.82f, cx - w * 0.34f, h * 0.62f, cx - w * 0.30f, h * 0.30f)
                    close()
                }
                drawPath(face, Color.White)
                drawCircle(Color(0xFFFB542B), w * 0.045f, Offset(cx - w * 0.11f, h * 0.46f))
                drawCircle(Color(0xFFFB542B), w * 0.045f, Offset(cx + w * 0.11f, h * 0.46f))
                drawPath(
                    Path().apply {
                        moveTo(cx - w * 0.07f, h * 0.60f)
                        lineTo(cx + w * 0.07f, h * 0.60f)
                        lineTo(cx, h * 0.70f)
                        close()
                    },
                    Color(0xFFFB542B),
                )
            }
        }
        else -> Canvas(modifier = modifier.size(size)) {
            when (id) {
                SearchEngineId.GOOGLE -> drawGoogleG()
                SearchEngineId.BING -> drawBingRibbon()
                SearchEngineId.DUCKDUCKGO -> drawDuckHead()
                else -> Unit
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGoogleG() {
    val d = size.minDimension
    val stroke = d * 0.24f
    val radius = d / 2f - stroke / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    val red = Color(0xFFEA4335)
    val yellow = Color(0xFFFBBC05)
    val green = Color(0xFF34A853)
    val blue = Color(0xFF4285F4)
    drawArc(red, -90f, 115f, false, center - Offset(radius, radius), Size(radius * 2, radius * 2), style = Stroke(stroke))
    drawArc(blue, 25f, 70f, false, center - Offset(radius, radius), Size(radius * 2, radius * 2), style = Stroke(stroke))
    drawArc(green, 95f, 110f, false, center - Offset(radius, radius), Size(radius * 2, radius * 2), style = Stroke(stroke))
    drawArc(yellow, 205f, 65f, false, center - Offset(radius, radius), Size(radius * 2, radius * 2), style = Stroke(stroke))
    drawRoundRect(
        blue,
        topLeft = Offset(center.x - stroke * 0.2f, center.y - stroke / 2f),
        size = Size(radius + stroke * 0.7f, stroke),
        cornerRadius = CornerRadius(stroke / 2f, stroke / 2f),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBingRibbon() {
    val w = size.width
    val h = size.height
    val teal = Color(0xFF0E9D9D)
    val tealDark = Color(0xFF067070)
    val ribbon = Path().apply {
        moveTo(w * 0.22f, h * 0.06f)
        lineTo(w * 0.68f, h * 0.06f)
        lineTo(w * 0.44f, h * 0.46f)
        lineTo(w * 0.82f, h * 0.94f)
        lineTo(w * 0.34f, h * 0.94f)
        lineTo(w * 0.56f, h * 0.48f)
        close()
    }
    drawPath(ribbon, teal)
    val fold = Path().apply {
        moveTo(w * 0.44f, h * 0.46f)
        lineTo(w * 0.56f, h * 0.48f)
        lineTo(w * 0.34f, h * 0.94f)
        lineTo(w * 0.24f, h * 0.72f)
        close()
    }
    drawPath(fold, tealDark)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDuckHead() {
    val d = size.minDimension
    val center = Offset(size.width / 2f, size.height / 2f)
    drawCircle(Color(0xFFDE5833), d * 0.42f, center)
    val beak = Path().apply {
        moveTo(center.x + d * 0.10f, center.y - d * 0.02f)
        lineTo(center.x + d * 0.42f, center.y + d * 0.10f)
        lineTo(center.x + d * 0.10f, center.y + d * 0.20f)
        close()
    }
    drawPath(beak, Color(0xFFFDD20A))
    drawCircle(Color.White, d * 0.11f, center + Offset(-d * 0.08f, -d * 0.12f))
    drawCircle(Color.Black, d * 0.05f, center + Offset(-d * 0.08f, -d * 0.12f))
}
