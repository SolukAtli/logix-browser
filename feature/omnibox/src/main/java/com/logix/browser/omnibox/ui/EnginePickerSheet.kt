package com.logix.browser.omnibox.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.logix.browser.search.SearchEngine
import com.logix.browser.search.SearchEngineId

private val ConfirmGreen = Color(0xFF4CAF50)

/**
 * Search-engine picker sheet: brand-mark cards with descriptions; the
 * active engine gets an accent border and a green confirmation tick.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnginePickerSheet(
    engines: List<SearchEngine>,
    selectedId: SearchEngineId,
    onSelect: (SearchEngineId) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val cardFill = MaterialTheme.colorScheme.surfaceContainerLow
    val cardBorder = MaterialTheme.colorScheme.outlineVariant
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Arama Motoru",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(Modifier.height(12.dp))
            engines.forEach { engine ->
                val selected = engine.id == selectedId
                ElevatedCard(
                    onClick = { onSelect(engine.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) accent else cardBorder,
                            shape = RoundedCornerShape(20.dp),
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = cardFill),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        EngineBrandIcon(engine.id)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                engine.displayName,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                            Text(
                                descriptionFor(engine.id),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (selected) {
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Seçili",
                                tint = ConfirmGreen,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun descriptionFor(id: SearchEngineId): String = when (id) {
    SearchEngineId.GOOGLE -> "Dünyanın en popüler arama motoru"
    SearchEngineId.BING -> "Microsoft'un arama motoru"
    SearchEngineId.DUCKDUCKGO -> "Gizlilik odaklı arama motoru"
    SearchEngineId.YANDEX -> "Rusya'nın arama motoru"
    SearchEngineId.BRAVE -> "Bağımsız, gizlilik odaklı arama"
}
