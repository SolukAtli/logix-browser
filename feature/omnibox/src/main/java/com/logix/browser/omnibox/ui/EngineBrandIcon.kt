package com.logix.browser.omnibox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGImageView
import com.logix.browser.search.SearchEngineId

/**
 * Verilen orijinal SVG artwork'leri birebir render eder
 * (`app/src/main/assets/engines/`). Uzerlerinde hicbir oynama yok:
 * dosya neyse ekranda o.
 */
@Composable
fun EngineBrandIcon(
    id: SearchEngineId,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
) {
    val context = LocalContext.current
    val svg = remember(id) {
        runCatching {
            context.assets.open(assetFor(id)).use { SVG.getFromInputStream(it) }
        }.getOrNull()
    }
    if (svg != null) {
        AndroidView(
            factory = { ctx -> SVGImageView(ctx).apply { setSVG(svg) } },
            // Motor değişince aynı view tekrar kullanılır; SVG'yi yenile.
            update = { (it as SVGImageView).setSVG(svg) },
            modifier = modifier.size(size),
        )
    } else {
        // SVG okunamazsa sade harf rozeti (yedek).
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = id.key.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

private fun assetFor(id: SearchEngineId): String = when (id) {
    SearchEngineId.GOOGLE -> "engines/google.svg"
    SearchEngineId.BING -> "engines/bing.svg"
    SearchEngineId.DUCKDUCKGO -> "engines/duckduckgo.svg"
    SearchEngineId.YANDEX -> "engines/yandex.svg"
    SearchEngineId.BRAVE -> "engines/brave.svg"
}
