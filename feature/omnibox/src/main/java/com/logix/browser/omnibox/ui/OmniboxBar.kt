package com.logix.browser.omnibox.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.logix.browser.search.SearchEngine
import com.logix.browser.search.SearchEngineId

/**
 * Modern adres çubuğu: yuvarlak hap alan, gölgeli yüzey, motor seçici,
 * sesli/görsel arama kısayolları ve accent git butonu.
 */
@Composable
fun OmniboxBar(
    query: String,
    onQueryChange: (String) -> Unit,
    engines: List<SearchEngine>,
    selectedEngine: SearchEngine,
    onSelectEngine: (SearchEngineId) -> Unit,
    onGo: () -> Unit,
    modifier: Modifier = Modifier,
    incognito: Boolean = false,
    onMic: (() -> Unit)? = null,
    onCamera: (() -> Unit)? = null,
) {
    var showEngineSheet by remember { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.primary
    val fill = MaterialTheme.colorScheme.surfaceContainerHigh

    Surface(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(28.dp),
        color = fill,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (incognito) {
                Icon(
                    Icons.Default.VisibilityOff,
                    contentDescription = "Gizli mod",
                    tint = accent,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            TextButton(onClick = { showEngineSheet = true }) {
                EngineBrandIcon(selectedEngine.id, size = 26.dp)
            }
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                placeholder = {
                    Text(
                        "Ara veya URL girin",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.6f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.4f),
                    focusedBorderColor = accent,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                ),
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Temizle")
                        }
                    } else {
                        androidx.compose.foundation.layout.Row {
                            if (onMic != null) {
                                IconButton(
                                    onClick = onMic,
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = "Sesli arama")
                                }
                            }
                            if (onCamera != null) {
                                IconButton(
                                    onClick = onCamera,
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = "Görsel arama")
                                }
                            }
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onGo() }),
            )
            IconButton(
                onClick = onGo,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = accent),
                modifier = Modifier
                    .padding(start = 4.dp, end = 2.dp)
                    .size(44.dp),
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Git",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
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
