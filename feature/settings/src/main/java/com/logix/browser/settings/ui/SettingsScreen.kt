package com.logix.browser.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.logix.browser.search.SearchEngine
import com.logix.browser.settings.BrowserSettings

private val GroupShape = RoundedCornerShape(20.dp)

/**
 * Settings screen: grouped rounded cards (theme, accent, engines, privacy,
 * filters) plus the red-accented death-reset danger card.
 */
@Composable
fun SettingsScreen(
    settings: BrowserSettings,
    engines: List<SearchEngine>,
    selectedEngineKey: String,
    onThemeChange: (String) -> Unit,
    onEngineChange: (String) -> Unit,
    onAdBlockChange: (Boolean) -> Unit,
    onTrackerBlockChange: (Boolean) -> Unit,
    onDesktopSiteChange: (Boolean) -> Unit,
    onFilterAutoUpdateChange: (Boolean) -> Unit,
    onRefreshFilters: () -> Unit,
    onAccentChange: (String) -> Unit,
    onAmoledChange: (Boolean) -> Unit,
    onCookiesChange: (Boolean) -> Unit,
    onHttpsOnlyChange: (Boolean) -> Unit,
    onDeathResetChange: (Boolean) -> Unit,
    onDeathResetDaysChange: (Int) -> Unit,
    onBarPositionChange: (String) -> Unit,
    onUserAgentChange: (String) -> Unit,
    onTextScaleChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeathDialog by remember { mutableStateOf(false) }
    var showCustomAccent by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SectionHeader("Görünüm")
        SectionCard {
            Row {
                FilterChip(
                    selected = settings.theme == "dark",
                    onClick = { onThemeChange("dark") },
                    label = { Text("Karanlık") },
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = settings.theme == "light",
                    onClick = { onThemeChange("light") },
                    label = { Text("Aydınlık") },
                )
            }
            SettingSwitch(
                label = "AMOLED Modu",
                checked = settings.amoled,
                onCheckedChange = onAmoledChange,
                icon = Icons.Default.DarkMode,
                iconTint = Color(0xFF9E9E9E),
            )
        }

        SectionHeader("Vurgu Rengi")
        SectionCard {
            AccentGrid(
                current = settings.accent,
                onSelect = onAccentChange,
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { showCustomAccent = true }) {
                Text("Özel renk seç (" + AccentPalette.labelFor(settings.accent) + ")")
            }
        }

        SectionHeader("Arama Motoru")
        SectionCard {
            engines.forEach { engine ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = engine.id.key == selectedEngineKey,
                        onClick = { onEngineChange(engine.id.key) },
                    )
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(engine.displayName)
                }
            }
        }

        SectionHeader("Arama Çubuğu ve Görünüm")
        SectionCard {
            Text("Arama Çubuğu Konumu")
            Spacer(Modifier.height(8.dp))
            Row {
                FilterChip(
                    selected = settings.barPosition == "top",
                    onClick = { onBarPositionChange("top") },
                    label = { Text("Üstte") },
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = settings.barPosition == "bottom",
                    onClick = { onBarPositionChange("bottom") },
                    label = { Text("Altta") },
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("Sayfa Yazı Boyutu (%${(settings.textScale * 100).toInt()})")
            Slider(
                value = settings.textScale,
                onValueChange = onTextScaleChange,
                valueRange = 0.8f..1.5f,
                steps = 6,
            )
        }

        SectionHeader("User-Agent")
        SectionCard {
            Row {
                FilterChip(
                    selected = settings.userAgent == "mobile",
                    onClick = { onUserAgentChange("mobile") },
                    label = { Text("Mobil") },
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = settings.userAgent == "desktop",
                    onClick = { onUserAgentChange("desktop") },
                    label = { Text("Masaüstü") },
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = settings.userAgent == "safari",
                    onClick = { onUserAgentChange("safari") },
                    label = { Text("Safari") },
                )
            }
        }

        SectionHeader("Gizlilik ve Güvenlik")
        SectionCard {
            SettingSwitch(
                label = "Reklam Engelleme",
                checked = settings.adBlockEnabled,
                onCheckedChange = onAdBlockChange,
                icon = Icons.Default.Block,
                iconTint = Color(0xFFF44336),
            )
            SettingSwitch(
                label = "İzleyici Koruması",
                checked = settings.trackerBlockEnabled,
                onCheckedChange = onTrackerBlockChange,
                icon = Icons.Default.VisibilityOff,
                iconTint = Color(0xFF9C27B0),
            )
            SettingSwitch(
                label = "Çerezleri Kabul Et",
                checked = settings.cookiesAccepted,
                onCheckedChange = onCookiesChange,
                icon = Icons.Default.Cookie,
                iconTint = Color(0xFF795548),
            )
            SettingSwitch(
                label = "HTTPS-Only",
                checked = settings.httpsOnly,
                onCheckedChange = onHttpsOnlyChange,
                icon = Icons.Default.Lock,
                iconTint = Color(0xFF4CAF50),
            )
            SettingSwitch(
                label = "Masaüstü Site",
                checked = settings.desktopSite,
                onCheckedChange = onDesktopSiteChange,
                icon = Icons.Default.DesktopWindows,
                iconTint = Color(0xFF2196F3),
            )
        }

        SectionHeader("Filtre Listeleri")
        SectionCard {
            SettingSwitch(
                label = "Otomatik güncelle",
                checked = settings.filterAutoUpdate,
                onCheckedChange = onFilterAutoUpdateChange,
                icon = Icons.Default.Sync,
                iconTint = Color(0xFF00BCD4),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Son güncelleme: " + formatTimestamp(settings.filterUpdatedAt),
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = onRefreshFilters) {
                    Text("Denetle")
                }
            }
        }

        SectionHeader("Tehlikeli Bölge")
        Card(
            modifier = modifier
                .fillMaxWidth()
                .border(2.dp, MaterialTheme.colorScheme.error, GroupShape),
            shape = GroupShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Ölümden Veri Sıfırlama",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
                Text(
                    "Açıkken, belirlenen günden eski geçmiş kayıtları otomatik silinir.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                SettingSwitch(
                    label = if (settings.deathResetEnabled) "Etkin" else "Devre dışı",
                    checked = settings.deathResetEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) showDeathDialog = true
                        else onDeathResetChange(false)
                    },
                    icon = Icons.Default.DeleteForever,
                    iconTint = MaterialTheme.colorScheme.error,
                )
                if (settings.deathResetEnabled) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            onDeathResetDaysChange(settings.deathResetDays - 1)
                        }) {
                            Text("−")
                        }
                        Text("${settings.deathResetDays} gün")
                        IconButton(onClick = {
                            onDeathResetDaysChange(settings.deathResetDays + 1)
                        }) {
                            Text("+")
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (showDeathDialog) {
        var dialogDays by remember { mutableStateOf(settings.deathResetDays.coerceIn(1, 365)) }
        AlertDialog(
            onDismissRequest = { showDeathDialog = false },
            title = { Text("Ölümden Veri Sıfırlama") },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .shadow(
                                    16.dp,
                                    CircleShape,
                                    ambientColor = MaterialTheme.colorScheme.error,
                                    spotColor = MaterialTheme.colorScheme.error,
                                )
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                                RoundedCornerShape(16.dp),
                            )
                            .padding(12.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "DİKKAT: Belirlediğiniz süre boyunca tarayıcıyı hiç açmazsanız; " +
                                    "tüm tarama geçmişiniz, çerezler, kayıtlı veriler ve oturumlar " +
                                    "GERİ DÖNDÜRÜLEMEZ ŞEKİLDE kalıcı olarak imha edilecektir.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        IconButton(onClick = {
                            dialogDays = (dialogDays - 1).coerceAtLeast(1)
                        }) {
                            Text(
                                "−",
                                style = MaterialTheme.typography.headlineMedium,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(16.dp),
                                )
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "$dialogDays Gün",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                        IconButton(onClick = {
                            dialogDays = (dialogDays + 1).coerceAtMost(365)
                        }) {
                            Text(
                                "+",
                                style = MaterialTheme.typography.headlineMedium,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onDeathResetDaysChange(dialogDays)
                    onDeathResetChange(true)
                    showDeathDialog = false
                }) {
                    Text("Sistemi Etkinleştir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeathDialog = false }) {
                    Text("Vazgeç")
                }
            },
        )
    }

    if (showCustomAccent) {
        CustomAccentDialog(
            current = settings.accent,
            onConfirm = {
                onAccentChange(it)
                showCustomAccent = false
            },
            onDismiss = { showCustomAccent = false },
        )
    }
}

@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, GroupShape),
        shape = GroupShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            content()
        }
    }
}

@Composable
private fun AccentGrid(
    current: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AccentPalette.PRESETS.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { option ->
                    val selected = option.key == current ||
                        (AccentPalette.colorFor(current) == option.color &&
                            AccentPalette.PRESETS.none { it.key == current })
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(option.color)
                            .clickable { onSelect(option.key) }
                            .then(
                                if (selected) {
                                    Modifier.border(3.dp, Color.White, CircleShape)
                                } else {
                                    Modifier
                                },
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomAccentDialog(
    current: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var hex by remember { mutableStateOf(current.takeIf { it.startsWith("#") } ?: "#FF5722") }
    val parsed = remember(hex) { AccentPalette.parseHex(hex) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Özel Vurgu Rengi") },
        text = {
            Column {
                OutlinedTextField(
                    value = hex,
                    onValueChange = { hex = it },
                    label = { Text("#RRGGBB") },
                    singleLine = true,
                    isError = parsed == null,
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(parsed ?: Color.Transparent)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let { onConfirm(hex.trim()) } },
                enabled = parsed != null,
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç")
            }
        },
    )
}

private fun formatTimestamp(epochMillis: Long): String {
    if (epochMillis <= 0) return "hiçbir zaman"
    val date = java.util.Date(epochMillis)
    val format = java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault())
    return format.format(date)
}

@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconTint)
            }
            Spacer(Modifier.width(12.dp))
        }
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
