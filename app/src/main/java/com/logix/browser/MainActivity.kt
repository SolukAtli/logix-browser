package com.logix.browser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.WindowManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Badge
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.logix.browser.chromiumbridge.ContentViewHost
import com.logix.browser.chromiumbridge.ShieldsConfig
import com.logix.browser.chromiumbridge.UserAgents
import com.logix.browser.omnibox.OmniboxViewModel
import com.logix.browser.omnibox.ui.OmniboxBar
import com.logix.browser.settings.SettingsViewModel
import com.logix.browser.settings.ui.AccentPalette
import com.logix.browser.settings.ui.SettingsScreen
import com.logix.browser.tabs.BookmarksViewModel
import com.logix.browser.tabs.HistoryViewModel
import com.logix.browser.tabs.TabsViewModel
import com.logix.browser.tabs.ui.BookmarksSheet
import com.logix.browser.tabs.ui.BrowserBottomBar
import com.logix.browser.tabs.ui.HistorySheet
import com.logix.browser.tabs.ui.NtpHome
import com.logix.browser.tabs.ui.TabsSheet
import com.logix.browser.ui.theme.LogixTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Browser shell: drawer menu + TabStrip + Omnibox (top/bottom) + active
 * [ContentViewHost] veya [NtpHome] + alt navigasyon. Sessiz UI: snackbar
 * yalnızca çıkış onayı için kullanılır.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsVm: SettingsViewModel = hiltViewModel()
            val settings by settingsVm.settings.collectAsStateWithLifecycle()
            LogixTheme(
                darkTheme = settings.theme != "light",
                amoled = settings.amoled,
                accent = AccentPalette.colorFor(settings.accent),
                incognito = settings.incognito,
            ) {
                BrowserScreen(settingsVm = settingsVm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserScreen(
    tabsVm: TabsViewModel = hiltViewModel(),
    omniVm: OmniboxViewModel = hiltViewModel(),
    settingsVm: SettingsViewModel,
    historyVm: HistoryViewModel = hiltViewModel(),
    bookmarksVm: BookmarksViewModel = hiltViewModel(),
) {
    val tabs by tabsVm.tabs.collectAsStateWithLifecycle()
    val activeTab by tabsVm.activeTab.collectAsStateWithLifecycle()
    val canGoBack by tabsVm.canGoBack.collectAsStateWithLifecycle()
    val selectedEngine by omniVm.selectedEngine.collectAsStateWithLifecycle()
    val settings by settingsVm.settings.collectAsStateWithLifecycle()
    val selectedSettingsEngine by settingsVm.selectedEngine.collectAsStateWithLifecycle()
    val history by historyVm.recent.collectAsStateWithLifecycle()
    val bookmarks by bookmarksVm.bookmarks.collectAsStateWithLifecycle()
    val deathTriggered by settingsVm.deathResetTriggered.collectAsStateWithLifecycle()
    val thumbnails by tabsVm.thumbnails.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showTabs by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showBookmarks by rememberSaveable { mutableStateOf(false) }
    var quickClearPhase by remember { mutableIntStateOf(0) } // 0 kapalı, 1 çalışıyor, 2 bitti
    var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Idle) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var lastBackPress by rememberSaveable { mutableStateOf(0L) }

    fun message(text: String) {
        scope.launch { snackbar.showSnackbar(text) }
    }

    fun goTo(url: String) {
        tabsVm.openInActiveTab(url)
    }

    fun openExternal(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    fun checkUpdate() {
        scope.launch {
            updateStatus = UpdateStatus.Checking
            val release = runCatching { UpdateChecker.latest() }.getOrNull()
            updateStatus = when {
                release == null || release.version.isBlank() ->
                    UpdateStatus.Failed("Bağlantı kurulamadı, sonra tekrar dene.")
                UpdateChecker.isNewer(release.version, BuildConfig.VERSION_NAME) ->
                    UpdateStatus.Available(release.version, release.notes, release.downloadUrl)
                else -> UpdateStatus.Latest
            }
        }
    }

    // Sesli arama: sistem konuşma tanıyıcı, sonucu omnibox'a yaz + otomatik git.
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spoken.isNullOrBlank()) {
            omniVm.onQueryChange(spoken)
            omniVm.resolve()?.let { tabsVm.openInActiveTab(it) }
        }
    }
    fun launchVoice() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Söyleyin")
        }
        runCatching { voiceLauncher.launch(intent) }
    }

    // Görsel arama: galeriden resim seç, Lens ana sayfasını aç.
    // Yerel dosya doğrudan yüklenemediği için kullanıcı yüklemeyi Lens'te tamamlar.
    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            tabsVm.openInActiveTab("https://lens.google.com/")
        }
    }
    fun launchImageSearch() {
        runCatching { imageLauncher.launch("image/*") }
    }

    // Dosya yükleme: sayfadaki <input type=file> için sistem seçici.
    var fileChooserCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileChooserLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        fileChooserCallback?.onReceiveValue(uris)
        fileChooserCallback = null
    }

    // Incognito: block screenshots/screen recording.
    LaunchedEffect(settings.incognito) {
        val window = (context as? ComponentActivity)?.window
        if (settings.incognito) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // Hızlı temizleme animasyonu: çalış → bitti → kendiliğinden kapan.
    LaunchedEffect(quickClearPhase) {
        if (quickClearPhase == 1) {
            settingsVm.quickClearAll { quickClearPhase = 2 }
            kotlinx.coroutines.delay(1100)
            quickClearPhase = 0
        }
    }

    // Back-stack priority (mutually exclusive):
    // 1. open sheet/dialog, 2. web history, 3. close tab,
    // 4. double-press to exit.
    val sheetsOpen = showSettings || showTabs || showHistory || showBookmarks
    BackHandler(enabled = sheetsOpen) {
        when {
            showSettings -> showSettings = false
            showHistory -> showHistory = false
            showBookmarks -> showBookmarks = false
            else -> showTabs = false
        }
    }
    BackHandler(enabled = !sheetsOpen && canGoBack) {
        tabsVm.goBack()
    }
    BackHandler(enabled = !sheetsOpen && !canGoBack && tabs.size > 1) {
        activeTab?.id?.let { tabsVm.closeTab(it) }
    }
    BackHandler(enabled = !sheetsOpen && !canGoBack && tabs.size <= 1) {
        val now = System.currentTimeMillis()
        if (now - lastBackPress < 2000L) {
            (context as? ComponentActivity)?.finish()
        } else {
            lastBackPress = now
            message("Çıkmak için tekrar geri basın")
        }
    }

    LaunchedEffect(activeTab?.id, activeTab?.url) {
        omniVm.onQueryChange(activeTab?.url.orEmpty())
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Kenardan kaydırma kapalı: web sayfasında gezerken menünün
        // istemsiz açılması engellenir, menü düğmeyle açılır.
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "LOGIX",
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    modifier = Modifier.padding(16.dp),
                )
                DrawerEntry(
                    label = "Sekmeler",
                    icon = Icons.Default.Tab,
                    badge = tabs.size.toString(),
                    onClick = {
                        scope.launch { drawerState.close() }
                        showTabs = true
                    },
                )
                DrawerEntry(
                    label = "Yer İmleri",
                    icon = Icons.Default.Bookmark,
                    badge = bookmarks.size.takeIf { it > 0 }?.toString(),
                    onClick = {
                        scope.launch { drawerState.close() }
                        showBookmarks = true
                    },
                )
                DrawerEntry(
                    label = "Geçmiş",
                    icon = Icons.Default.History,
                    badge = history.size.takeIf { it > 0 }?.toString(),
                    onClick = {
                        scope.launch { drawerState.close() }
                        showHistory = true
                    },
                )
                DrawerEntry(
                    label = "Masaüstü Site",
                    icon = Icons.Default.DesktopWindows,
                    badge = if (settings.desktopSite) "Açık" else "Kapalı",
                    onClick = {
                        settingsVm.setDesktopSite(!settings.desktopSite)
                    },
                )
                run {
                    val url = activeTab?.url.orEmpty()
                    val title = activeTab?.title.orEmpty()
                    val isBookmarked = bookmarks.any { it.url == url }
                    if (url.isNotEmpty()) {
                        DrawerEntry(
                            label = if (isBookmarked) "Yer İmini Kaldır" else "Yer İmine Ekle",
                            icon = if (isBookmarked) Icons.Default.BookmarkRemove else Icons.Default.BookmarkAdd,
                            onClick = {
                                bookmarksVm.toggle(url, title, isBookmarked)
                            },
                        )
                    }
                }
                DrawerEntry(
                    label = "Hızlı Temizle",
                    icon = Icons.Default.Delete,
                    onClick = {
                        scope.launch { drawerState.close() }
                        quickClearPhase = 1
                    },
                )
                DrawerEntry(
                    label = "Hızlı Karanlık Mod",
                    icon = Icons.Default.DarkMode,
                    onClick = {
                        val next = if (settings.theme == "dark") "light" else "dark"
                        settingsVm.setTheme(next)
                    },
                )
                DrawerEntry(
                    label = "Gizli Mod",
                    icon = Icons.Default.VisibilityOff,
                    badge = if (settings.incognito) "Açık" else null,
                    onClick = {
                        val next = !settings.incognito
                        settingsVm.setIncognito(next)
                        if (next) {
                            tabsVm.clearActiveEngineHistory()
                        }
                    },
                )
                DrawerEntry(
                    label = "Bu Sayfayı Paylaş",
                    icon = Icons.Default.Share,
                    onClick = {
                        val url = activeTab?.url
                        if (!url.isNullOrEmpty()) shareUrl(context, url)
                    },
                )
                DrawerEntry(
                    label = "URL Kopyala",
                    icon = Icons.Default.ContentCopy,
                    onClick = {
                        val url = activeTab?.url
                        if (!url.isNullOrEmpty()) copyUrl(context, url)
                    },
                )
                DrawerEntry(
                    label = "Ayarlar",
                    icon = Icons.Default.Settings,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showSettings = true
                    },
                )
            }
        },
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                if (settings.barPosition != "bottom") {
                    OmniboxBar(
                        query = omniVm.query,
                        onQueryChange = omniVm::onQueryChange,
                        engines = omniVm.availableEngines,
                        selectedEngine = selectedEngine,
                        onSelectEngine = omniVm::selectEngine,
                        onGo = { omniVm.resolve()?.let { tabsVm.openInActiveTab(it) } },
                        tabCount = tabs.size,
                        onShowTabs = { showTabs = true },
                        incognito = settings.incognito,
                        onMic = ::launchVoice,
                        onCamera = ::launchImageSearch,
                    )
                }
            },
            bottomBar = {
                androidx.compose.foundation.layout.Column {
                    if (settings.barPosition == "bottom") {
                        OmniboxBar(
                            query = omniVm.query,
                            onQueryChange = omniVm::onQueryChange,
                            engines = omniVm.availableEngines,
                            selectedEngine = selectedEngine,
                            onSelectEngine = omniVm::selectEngine,
                            onGo = { omniVm.resolve()?.let { tabsVm.openInActiveTab(it) } },
                            tabCount = tabs.size,
                            onShowTabs = { showTabs = true },
                            incognito = settings.incognito,
                            onMic = ::launchVoice,
                            onCamera = ::launchImageSearch,
                        )
                    }
                    BrowserBottomBar(
                        accent = AccentPalette.colorFor(settings.accent),
                        tabCount = tabs.size,
                        onBack = { tabsVm.goBack() },
                        onForward = { tabsVm.goForward() },
                        onHome = { tabsVm.createTab() },
                        onRefresh = { tabsVm.refresh() },
                        onShowMenu = { scope.launch { drawerState.open() } },
                        incognito = settings.incognito,
                    )
                }
            },
        ) { padding ->
            val url = activeTab?.url
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                if (loadProgress in 1..99) {
                    LinearProgressIndicator(
                        progress = { loadProgress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = AccentPalette.colorFor(settings.accent),
                    )
                }
                PullToRefreshBox(
                    isRefreshing = false,
                    onRefresh = { tabsVm.refresh() },
                    state = rememberPullToRefreshState(),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    NtpBody(
                        url = url,
                        omniVm = omniVm,
                        tabsVm = tabsVm,
                        settingsVm = settingsVm,
                        historyVm = historyVm,
                        onProgressChanged = { loadProgress = it },
                        showSettings = { showSettings = true },
                        showTabs = { showTabs = true },
                        showHistory = { showHistory = true },
                        showBookmarks = { showBookmarks = true },
                        onMic = ::launchVoice,
                        onCamera = ::launchImageSearch,
                        onFileChooserRequest = { callback, params ->
                            fileChooserCallback?.onReceiveValue(null)
                            fileChooserCallback = callback
                            val intent = runCatching { params?.createIntent() }.getOrNull()
                            if (intent != null) {
                                runCatching { fileChooserLauncher.launch(intent) }
                                    .onFailure {
                                        fileChooserCallback?.onReceiveValue(null)
                                        fileChooserCallback = null
                                    }
                            } else {
                                fileChooserCallback?.onReceiveValue(null)
                                fileChooserCallback = null
                            }
                        },
                        tabCount = tabs.size,
                    )
                }
            }
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
            thumbnails = thumbnails,
        )
    }

    LaunchedEffect(showTabs) {
        if (showTabs) tabsVm.refreshThumbnail()
    }

    if (deathTriggered) {
        AlertDialog(
            onDismissRequest = settingsVm::acknowledgeDeathReset,
            title = { Text("Ölüm Anahtarı Tetiklendi") },
            text = {
                Text(
                    "Tarayıcı uzun süre açılmadığı için koruma devreye girdi: " +
                        "geçmiş, yer imleri, sekmeler, çerezler ve oturumlar silindi.",
                )
            },
            confirmButton = {
                TextButton(onClick = settingsVm::acknowledgeDeathReset) {
                    Text("Tamam")
                }
            },
        )
    }

    if (quickClearPhase > 0) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            androidx.compose.material3.Card(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (quickClearPhase == 1) {
                        CircularProgressIndicator()
                        Text("Temizleniyor…")
                    } else {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp),
                        )
                        Text("Temizlendi")
                    }
                }
            }
        }
    }

    if (showHistory) {
        HistorySheet(
            entries = history,
            onOpen = { goTo(it); showHistory = false },
            onDelete = historyVm::delete,
            onClearAll = historyVm::clearAll,
            onDismiss = { showHistory = false },
        )
    }

    if (showBookmarks) {
        BookmarksSheet(
            bookmarks = bookmarks,
            onOpen = { goTo(it); showBookmarks = false },
            onRemove = bookmarksVm::remove,
            onClearAll = bookmarksVm::clearAll,
            onDismiss = { showBookmarks = false },
        )
    }

    if (showSettings) {
        ModalBottomSheet(onDismissRequest = { showSettings = false }) {
            SettingsScreen(
                settings = settings,
                engines = settingsVm.availableEngines,
                selectedEngineKey = selectedSettingsEngine.id.key,
                onThemeChange = settingsVm::setTheme,
                onEngineChange = settingsVm::setSearchEngine,
                onAdBlockChange = settingsVm::setAdBlock,
                onTrackerBlockChange = settingsVm::setTrackerBlock,
                onDesktopSiteChange = settingsVm::setDesktopSite,
                onFilterAutoUpdateChange = settingsVm::setFilterAutoUpdate,
                onRefreshFilters = settingsVm::refreshFilters,
                onAccentChange = settingsVm::setAccent,
                onAmoledChange = settingsVm::setAmoled,
                onCookiesChange = settingsVm::setCookiesAccepted,
                onHttpsOnlyChange = settingsVm::setHttpsOnly,
                onDeathResetChange = settingsVm::setDeathResetEnabled,
                onDeathResetDaysChange = settingsVm::setDeathResetDays,
                onBarPositionChange = settingsVm::setBarPosition,
                onUserAgentChange = settingsVm::setUserAgent,
                onTextScaleChange = settingsVm::setTextScale,
                onQuickClearHistoryChange = settingsVm::setQuickClearHistory,
                onQuickClearCookiesChange = settingsVm::setQuickClearCookies,
                onQuickClearCacheChange = settingsVm::setQuickClearCache,
                onQuickClearBookmarksChange = settingsVm::setQuickClearBookmarks,
                appVersion = BuildConfig.VERSION_NAME,
                updateBusy = updateStatus is UpdateStatus.Checking,
                updateLabel = when (val s = updateStatus) {
                    is UpdateStatus.Latest -> "En güncel sürümü kullanıyorsun."
                    is UpdateStatus.Available -> "Yeni sürüm var: ${s.version}"
                    is UpdateStatus.Failed -> s.reason
                    else -> null
                },
                showUpdateDownload = updateStatus is UpdateStatus.Available,
                onCheckUpdate = ::checkUpdate,
                onDownloadUpdate = {
                    (updateStatus as? UpdateStatus.Available)
                        ?.downloadUrl?.let(::openExternal)
                },
            )
        }
    }
}

@Composable
private fun NtpBody(
    url: String?,
    omniVm: OmniboxViewModel,
    tabsVm: TabsViewModel,
    settingsVm: SettingsViewModel,
    historyVm: HistoryViewModel,
    onProgressChanged: (Int) -> Unit,
    showSettings: () -> Unit,
    showTabs: () -> Unit,
    showHistory: () -> Unit,
    showBookmarks: () -> Unit,
    onMic: () -> Unit,
    onCamera: () -> Unit,
    onFileChooserRequest: (
        ValueCallback<Array<Uri>>?,
        WebChromeClient.FileChooserParams?,
    ) -> Unit,
    tabCount: Int,
    modifier: Modifier = Modifier,
) {
    val settings by settingsVm.settings.collectAsStateWithLifecycle()
    if (url.isNullOrEmpty()) {
        NtpHome(
            query = omniVm.query,
            onQueryChange = omniVm::onQueryChange,
            onSearch = { omniVm.resolve()?.let { tabsVm.openInActiveTab(it) } },
            onBookmarks = showBookmarks,
            onHistory = showHistory,
            onTheme = {
                val next = if (settings.theme == "dark") "light" else "dark"
                settingsVm.setTheme(next)
            },
            onSettings = showSettings,
            onMic = onMic,
            onCamera = onCamera,
            tabCount = tabCount,
            onShowTabs = showTabs,
            modifier = modifier,
        )
    } else {
        // Masaüstü anahtarı açıksa UA zorla masaüstüne döner.
        val desktopActive = settings.desktopSite || settings.userAgent == "desktop"
        ContentViewHost(
            url = url,
            modifier = modifier.fillMaxSize(),
            onEngineReady = tabsVm::bindEngine,
            onNavigationStateChanged = tabsVm::onNavigationStateChanged,
            onProgressChanged = onProgressChanged,
            onPageVisited = historyVm::logVisit,
            onFileChooserRequest = onFileChooserRequest,
            userAgent = if (desktopActive) UserAgents.DESKTOP else UserAgents.forKey(settings.userAgent),
            textScale = settings.textScale,
            incognito = settings.incognito,
            cookiesAccepted = settings.cookiesAccepted,
            shields = ShieldsConfig(
                adBlock = settings.adBlockEnabled,
                trackerBlock = settings.trackerBlockEnabled,
                httpsOnly = settings.httpsOnly,
            ),
            desktopMode = desktopActive,
        )
    }
}

@Composable
private fun DrawerEntry(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
) {
    androidx.compose.material3.NavigationDrawerItem(
        label = { Text(label) },
        icon = { Icon(icon, contentDescription = null) },
        badge = badge?.let { { Badge { Text(it) } } },
        selected = false,
        onClick = onClick,
        modifier = modifier.padding(horizontal = 12.dp),
    )
}

private fun shareUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
    }
    context.startActivity(Intent.createChooser(intent, "Paylaş"))
}

private fun copyUrl(context: Context, url: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("url", url))
}
