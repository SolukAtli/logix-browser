package com.logix.browser.tabs.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Bottom navigation/tab bar: successor of the legacy web UI's `bottomBar`.
 */
@Composable
fun BrowserBottomBar(
    tabCount: Int,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onHome: () -> Unit,
    onRefresh: () -> Unit,
    onShowTabs: () -> Unit,
    onShowSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        NavigationBarItem(
            selected = false,
            onClick = onBack,
            icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri") },
        )
        NavigationBarItem(
            selected = false,
            onClick = onForward,
            icon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "İleri") },
        )
        NavigationBarItem(
            selected = false,
            onClick = onHome,
            icon = { Icon(Icons.Default.Home, contentDescription = "Ana sayfa") },
        )
        NavigationBarItem(
            selected = false,
            onClick = onRefresh,
            icon = { Icon(Icons.Default.Refresh, contentDescription = "Yenile") },
        )
        NavigationBarItem(
            selected = false,
            onClick = onShowTabs,
            icon = {
                BadgedBox(badge = { Badge { Text(tabCount.toString()) } }) {
                    Text("▤")
                }
            },
        )
        NavigationBarItem(
            selected = false,
            onClick = onShowSettings,
            icon = { Icon(Icons.Default.Settings, contentDescription = "Ayarlar") },
        )
    }
}
