package com.logix.browser.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.logix.browser.search.SearchEngine
import com.logix.browser.settings.BrowserSettings

/**
 * Settings screen: successor of the legacy web UI's settings panel
 * (theme, search engine, privacy toggles).
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text("Tema")
        Spacer(Modifier.height(8.dp))
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
        Spacer(Modifier.height(16.dp))
        Text("Arama Motoru")
        engines.forEach { engine ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = engine.id.key == selectedEngineKey,
                    onClick = { onEngineChange(engine.id.key) },
                )
                Text(engine.displayName)
            }
        }
        Spacer(Modifier.height(16.dp))
        SettingSwitch(
            label = "Reklam Engelleme",
            checked = settings.adBlockEnabled,
            onCheckedChange = onAdBlockChange,
        )
        SettingSwitch(
            label = "İzleyici Koruması",
            checked = settings.trackerBlockEnabled,
            onCheckedChange = onTrackerBlockChange,
        )
        SettingSwitch(
            label = "Masaüstü Site",
            checked = settings.desktopSite,
            onCheckedChange = onDesktopSiteChange,
        )
    }
}

@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
