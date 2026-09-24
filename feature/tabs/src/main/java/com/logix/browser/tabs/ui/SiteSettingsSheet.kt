package com.logix.browser.tabs.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Bu site için geçersiz kılmalar: anahtar kapalıysa genel ayar,
 * açıksa siteye özel değer kullanılır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteSettingsSheet(
    host: String,
    javaScript: Boolean?,
    adBlock: Boolean?,
    globalJs: Boolean,
    globalAdBlock: Boolean,
    onJsChange: (Boolean?) -> Unit,
    onAdBlockChange: (Boolean?) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Bu site",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                host,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            OverrideRow(
                label = "JavaScript",
                effective = javaScript ?: globalJs,
                overridden = javaScript != null,
                onCheckedChange = { onJsChange(if (it == globalJs) null else it) },
            )
            OverrideRow(
                label = "Reklam engelleme",
                effective = adBlock ?: globalAdBlock,
                overridden = adBlock != null,
                onCheckedChange = { onAdBlockChange(if (it == globalAdBlock) null else it) },
            )
            if (javaScript != null || adBlock != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onReset) {
                    Text("Site ayarlarını sıfırla (geneli kullan)")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun OverrideRow(
    label: String,
    effective: Boolean,
    overridden: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label)
            if (overridden) {
                Text(
                    "Siteye özel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = effective, onCheckedChange = onCheckedChange)
    }
}
