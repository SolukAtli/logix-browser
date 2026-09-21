package com.logix.browser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.logix.browser.chromiumbridge.ContentViewHost
import com.logix.browser.omnibox.OmniboxViewModel
import com.logix.browser.omnibox.ui.OmniboxBar
import com.logix.browser.settings.SettingsViewModel
import com.logix.browser.settings.ui.SettingsScreen
import com.logix.browser.tabs.TabsViewModel
import com.logix.browser.tabs.ui.BrowserBottomBar
import com.logix.browser.tabs.ui.TabsSheet
import com.logix.browser.ui.theme.LogixTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Faz-2 shell: Omnibox (top) + active [ContentViewHost] (center) +
 * navigation/tab bar (bottom). State comes from the feature ViewModels.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsVm: SettingsViewModel = hiltViewModel()
            val settings by settingsVm.settings.collectAsStateWithLifecycle()
            LogixTheme(darkTheme = settings.theme != "light") {
                BrowserScreen(settingsVm = settingsVm)
            }
        }
    }
}

@Composable
private fun BrowserScreen(
    tabsVm: TabsViewModel = hiltViewModel(),
    omniVm: OmniboxViewModel = hiltViewModel(),
    settingsVm: SettingsViewModel,
) {
    val tabs by tabsVm.tabs.collectAsStateWithLifecycle()
    val activeTab by tabsVm.activeTab.collectAsStateWithLifecycle()
    val selectedEngine by omniVm.selectedEngine.collectAsStateWithLifecycle()
    val settings by settingsVm.settings.collectAsStateWithLifecycle()
    val selectedSettingsEngine by settingsVm.selectedEngine.collectAsStateWithLifecycle()

    var showTabs by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(activeTab?.id, activeTab?.url) {
        omniVm.onQueryChange(activeTab?.url.orEmpty())
    }

    Scaffold(
        topBar = {
            OmniboxBar(
                query = omniVm.query,
                onQueryChange = omniVm::onQueryChange,
                engines = omniVm.availableEngines,
                selectedEngine = selectedEngine,
                onSelectEngine = omniVm::selectEngine,
                onGo = { omniVm.resolve()?.let { tabsVm.openInActiveTab(it) } },
            )
        },
        bottomBar = {
            BrowserBottomBar(
                tabCount = tabs.size,
                onBack = { tabsVm.goBack() },
                onForward = { tabsVm.goForward() },
                onHome = { tabsVm.createTab() },
                onRefresh = { tabsVm.refresh() },
                onShowTabs = { showTabs = true },
                onShowSettings = { showSettings = true },
            )
        },
    ) { padding ->
        val url = activeTab?.url
        if (url.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LOGIX", style = MaterialTheme.typography.displayMedium)
                    Text(
                        "Aramak için yukarı yazın",
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        } else {
            ContentViewHost(
                url = url,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                onEngineReady = tabsVm::bindEngine,
            )
        }
    }

    if (showTabs) {
        TabsSheet(
            tabs = tabs,
            activeTabId = activeTab?.id,
            onSelect = { tabsVm.selectTab(it); showTabs = false },
            onClose = tabsVm::closeTab,
            onNewTab = { tabsVm.createTab(); showTabs = false },
            onDismiss = { showTabs = false },
        )
    }

    if (showSettings) {
        Dialog(onDismissRequest = { showSettings = false }) {
            SettingsScreen(
                settings = settings,
                engines = settingsVm.availableEngines,
                selectedEngineKey = selectedSettingsEngine.id.key,
                onThemeChange = settingsVm::setTheme,
                onEngineChange = settingsVm::setSearchEngine,
                onAdBlockChange = settingsVm::setAdBlock,
                onTrackerBlockChange = settingsVm::setTrackerBlock,
                onDesktopSiteChange = settingsVm::setDesktopSite,
            )
        }
    }
}
