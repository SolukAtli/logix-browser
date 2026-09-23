package com.logix.browser.tabs.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.logix.browser.database.TabState

/**
 * Yatay sekme şeridi: üst veya alt bara gömülür.
 * Aktif sekme accent çerçeveli, diğerleri sade chiptir.
 */
@Composable
fun TabStripBar(
    tabs: List<TabState>,
    activeTabId: String?,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
    onNewTab: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tabs.size <= 1 && tabs.firstOrNull()?.url.isNullOrEmpty()) {
        // Tek boş sekmede şeridi gizle, kalabalık yapmasın.
        return
    }
    Row(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(tabs, key = { it.id }) { tab ->
                val selected = tab.id == activeTabId
                FilterChip(
                    selected = selected,
                    onClick = { onSelect(tab.id) },
                    label = {
                        Text(
                            text = tab.title.ifBlank { tab.url?.take(20) ?: "Yeni Sekme" },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    trailingIcon = if (tabs.size > 1) {
                        {
                            IconButton(
                                onClick = { onClose(tab.id) },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Kapat",
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    } else null,
                    shape = RoundedCornerShape(16.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }
        }
        IconButton(onClick = onNewTab) {
            Icon(Icons.Default.Add, contentDescription = "Yeni sekme")
        }
    }
}
