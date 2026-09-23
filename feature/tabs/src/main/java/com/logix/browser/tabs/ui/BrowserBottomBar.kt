package com.logix.browser.tabs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Bottom docked navigation bar: back, forward, glowing accent home,
 * refresh, and menu with a live tab-count badge. Blends into the Android
 * system navigation bar via [navigationBarsPadding].
 */
@Composable
fun BrowserBottomBar(
    accent: Color,
    tabCount: Int,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onHome: () -> Unit,
    onRefresh: () -> Unit,
    onShowMenu: () -> Unit,
    modifier: Modifier = Modifier,
    incognito: Boolean = false,
) {
    NavigationBar(
        modifier = modifier.navigationBarsPadding(),
        tonalElevation = 0.dp,
    ) {
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
            icon = {
                BadgedBox(
                    badge = {
                        if (incognito) {
                            Badge(containerColor = accent) {
                                Icon(
                                    Icons.Default.VisibilityOff,
                                    contentDescription = "Gizli mod",
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        }
                    },
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .shadow(12.dp, CircleShape, ambientColor = accent, spotColor = accent)
                            .clip(CircleShape)
                            .background(accent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "Ana sayfa",
                            tint = Color.White,
                        )
                    }
                }
            },
        )
        NavigationBarItem(
            selected = false,
            onClick = onRefresh,
            icon = { Icon(Icons.Default.Refresh, contentDescription = "Yenile") },
        )
        NavigationBarItem(
            selected = false,
            onClick = onShowMenu,
            icon = {
                BadgedBox(badge = { Badge { Text(tabCount.toString()) } }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menü")
                }
            },
        )
    }
}
