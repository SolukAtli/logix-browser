package com.logix.browser.omnibox.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logix.browser.search.SearchEngine
import com.logix.browser.search.SearchEngineId

/**
 * Chrome tarzı adres çubuğu: tek hap yüzey, motor logosu, şeffaf metin
 * alanı, sesli/görsel kısayollar ve yanda sekme sayacı.
 */
@Composable
fun OmniboxBar(
    query: String,
    onQueryChange: (String) -> Unit,
    engines: List<SearchEngine>,
    selectedEngine: SearchEngine,
    onSelectEngine: (SearchEngineId) -> Unit,
    onGo: () -> Unit,
    tabCount: Int,
    onShowTabs: () -> Unit,
    modifier: Modifier = Modifier,
    incognito: Boolean = false,
    onMic: (() -> Unit)? = null,
    onCamera: (() -> Unit)? = null,
) {
    var showEngineSheet by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier
                    .height(52.dp)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (incognito) {
                    Icon(
                        Icons.Default.VisibilityOff,
                        contentDescription = "Gizli mod",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { showEngineSheet = true }
                        .padding(8.dp),
                ) {
                    EngineBrandIcon(selectedEngine.id, size = 26.dp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { onGo() }),
                    decorationBox = { inner ->
                        Box {
                            if (query.isEmpty()) {
                                Text(
                                    "Ara veya URL girin",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            inner()
                        }
                    },
                )
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Temizle")
                    }
                } else {
                    if (onMic != null) {
                        IconButton(onClick = onMic, modifier = Modifier.size(44.dp)) {
                            Icon(Icons.Default.Mic, contentDescription = "Sesli arama")
                        }
                    }
                    if (onCamera != null) {
                        IconButton(onClick = onCamera, modifier = Modifier.size(44.dp)) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Görsel arama")
                        }
                    }
                }
            }
        }
        // Sekme sayacı: Chrome'daki kare rozet.
        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(48.dp)
                .border(
                    2.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(14.dp),
                )
                .clip(RoundedCornerShape(14.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onShowTabs() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = tabCount.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }

    if (showEngineSheet) {
        EnginePickerSheet(
            engines = engines,
            selectedId = selectedEngine.id,
            onSelect = {
                onSelectEngine(it)
                showEngineSheet = false
            },
            onDismiss = { showEngineSheet = false },
        )
    }
}
