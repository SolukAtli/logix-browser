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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Badge
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.logix.browser.tabs.SiteSettingsViewModel
import com.logix.browser.tabs.TabsViewModel
import com.logix.browser.tabs.ui.BookmarksSheet
import com.logix.browser.tabs.ui.BrowserBottomBar
import com.logix.browser.tabs.ui.HistorySheet
import com.logix.browser.tabs.ui.NtpHome
import com.logix.browser.tabs.ui.ReaderSheet
import com.logix.browser.tabs.ui.SiteSettingsSheet
import com.logix.browser.tabs.ui.TabsSheet
import com.logix.browser.ui.theme.LogixTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Browser shell: drawer menu + TabStrip + Omnibox (top/bottom) + active
 * [ContentViewHost] veya [NtpHome] + alt navigasyon. Sessiz UI: snackbar
 * yalnızca çıkış onayı için kullanılır.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Diğer uygulamalardan gelen bağlantı (VIEW/BROWSABLE). */
    var externalUrl by mutableStateOf<String?>(null)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        consumeExternalIntent(intent)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeExternalIntent(intent)
    }

    private fun consumeExternalIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.dataString?.takeIf { it.startsWith("http") }?.let {
                externalUrl = it
            }
        }
    }

    fun takeExternalUrl(): String? {
        val url = externalUrl
        externalUrl = null
        return url
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
    siteVm: SiteSettingsViewModel = hiltViewModel(),
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
    val shieldTotals by settingsVm.shieldTotals.collectAsStateWithLifecycle()
    val onboarded by settingsVm.onboarded.collectAsStateWithLifecycle()
    val siteOverrides by siteVm.overrides.collectAsStateWithLifecycle()
    val filterRefreshing by settingsVm.filterRefreshing.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showTabs by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showBookmarks by rememberSaveable { mutableStateOf(false) }
    var findOpen by rememberSaveable { mutableStateOf(false) }
    var findQuery by rememberSaveable { mutableStateOf("") }
    var findResult by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var permissionPrompt by remember {
        mutableStateOf<Triple<String, List<String>, (Boolean) -> Unit>?>(null)
    }
    var showSiteSettings by rememberSaveable { mutableStateOf(false) }
    var readerText by remember { mutableStateOf<Pair<String, String?>?>(null) }

    fun pageHost(): String = runCatching {
        java.net.URI(activeTab?.url.orEmpty()).host.orEmpty()
    }.getOrDefault("")
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

    // Diğer uygulamadan gelen bağlantıyı aktif sekmede aç.
    val hostActivity = context as? MainActivity
    LaunchedEffect(hostActivity?.externalUrl) {
        hostActivity?.takeExternalUrl()?.let { goTo(it) }
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

    // Yer imi dışa aktar (HTML dosyası).
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/html"),
    ) { uri ->
        if (uri != null) {
            runCatching {
                val html = buildString {
                    appendLine("<!DOCTYPE NETSCAPE-Bookmark-file-1>")
                    appendLine("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">")
                    appendLine("<TITLE>Yer İmleri</TITLE><H1>Yer İmleri</H1><DL><p>")
                    bookmarksVm.bookmarks.value.forEach { bm ->
                        val title = bm.title.replace("&", "&amp;").replace("<", "&lt;")
                        appendLine("<DT><A HREF=\"${bm.url}\">$title</A>")
                    }
                    appendLine("</DL><p>")
                }
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(html.toByteArray())
                }
            }
        }
    }

    // Yer imi içe aktar (tarayıcı HTML dosyası).
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                val html = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.readText().orEmpty()
                bookmarksVm.importHtml(html)
            }
        }
    }

    // Arka plan sesi + açılışta güncelleme denetimi (günde bir).
    LaunchedEffect(settings.backgroundAudio) {
        tabsVm.setKeepAudio(settings.backgroundAudio)
    }
    LaunchedEffect(Unit) {
        val dayMs = 24L * 60L * 60L * 1000L
        if (System.currentTimeMillis() - settings.updateLastCheck > dayMs) {
            settingsVm.markUpdateChecked()
            checkUpdate()
        }
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

    // Hızlı temizleme LaunchedEffect YOK: faz değişimi efekti iptal edip
    // yazıyı takılı bırakıyordu; akış drawer tıklamasında tek coroutine'de.

    // Back-stack priority (mutually exclusive):
    // 1. open sheet/dialog, 2. web history, 3. close tab,
    // 4. double-press to exit.
    val sheetsOpen = showSettings || showTabs || showHistory || showBookmarks || findOpen
    BackHandler(enabled = sheetsOpen) {
        when {
            showSettings -> showSettings = false
            showHistory -> showHistory = false
            showBookmarks -> showBookmarks = false
            findOpen -> {
                findOpen = false
                findQuery = ""
                tabsVm.findAll(null)
            }
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
                    label = "İndirilenler",
                    icon = Icons.Default.Download,
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS),
                            )
                        }
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
                DrawerEntry(
                    label = "Sayfada Ara",
                    icon = Icons.Default.Search,
                    onClick = {
                        scope.launch { drawerState.close() }
                        findResult = null
                        findOpen = true
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
                        val host = pageHost()
                        if (host.isNotEmpty()) {
                            DrawerEntry(
                                label = "Bu Site İçin",
                                icon = Icons.Default.Tune,
                                badge = if (siteOverrides.containsKey(host)) "Özel" else null,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    showSiteSettings = true
                                },
                            )
                        }
                        DrawerEntry(
                            label = "Okuyucu Modu",
                            icon = Icons.AutoMirrored.Filled.Article,
                            onClick = {
                                scope.launch { drawerState.close() }
                                tabsVm.readArticle { t, text -> readerText = t to text }
                            },
                        )
                        DrawerEntry(
                            label = "Sayfayı Çevir",
                            icon = Icons.Default.Translate,
                            onClick = {
                                val encoded = java.net.URLEncoder.encode(url, "UTF-8")
                                goTo("https://translate.google.com/translate?sl=auto&tl=tr&u=$encoded")
                            },
                        )
                        DrawerEntry(
                            label = "Kaynağı Görüntüle",
                            icon = Icons.Default.Code,
                            onClick = { goTo("view-source:$url") },
                        )
                    }
                }
                if (tabs.size > 1) {
                    DrawerEntry(
                        label = "Tüm Sekmeleri Yer İmle",
                        icon = Icons.Default.Bookmarks,
                        onClick = {
                            bookmarksVm.addAll(tabs.mapNotNull { tab ->
                                tab.url?.let { it to tab.title }
                            })
                        },
                    )
                }
                DrawerEntry(
                    label = "Hızlı Temizle",
                    icon = Icons.Default.Delete,
                    onClick = {
                        scope.launch { drawerState.close() }
                        scope.launch {
                            quickClearPhase = 1
                            settingsVm.quickClearAll()
                            quickClearPhase = 2
                            delay(1300)
                            quickClearPhase = 0
                        }
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
                DrawerEntry(
                    label = "Çıkış",
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    onClick = { (context as? ComponentActivity)?.finish() },
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
                            suggestions = omniVm.suggestions,
                            onPickSuggestion = {
                                omniVm.onQueryChange(it)
                                omniVm.resolve()?.let { url -> tabsVm.openInActiveTab(url) }
                            },
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
                        // Menü düğmesi aç/kapa: açık menüde basınca kapanır.
                        onShowMenu = {
                            scope.launch {
                                if (drawerState.isOpen) drawerState.close()
                                else drawerState.open()
                            }
                        },
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
                if (findOpen) {
                    FindBar(
                        query = findQuery,
                        onQueryChange = {
                            findQuery = it
                            tabsVm.findAll(it.ifBlank { null })
                        },
                        resultText = findResult?.let { (active, total) ->
                            if (total == 0) "0/0" else "${active + 1}/$total"
                        },
                        onNext = { tabsVm.findNext(true) },
                        onPrevious = { tabsVm.findNext(false) },
                        onClose = {
                            findOpen = false
                            findQuery = ""
                            findResult = null
                            tabsVm.findAll(null)
                        },
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
                            // Önce sitenin istediği seçici, olmazsa genel dosya seçici.
                            val intent = runCatching { params?.createIntent() }.getOrNull()
                                ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                    type = "*/*"
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                }
                            runCatching { fileChooserLauncher.launch(intent) }
                                .onFailure {
                                    fileChooserCallback?.onReceiveValue(null)
                                    fileChooserCallback = null
                                }
                        },
                        onFindResult = { active, total -> findResult = active to total },
                        onPermissionRequest = { origin, resources, decide ->
                            if (resources.isEmpty()) {
                                permissionPrompt = null
                            } else {
                                permissionPrompt = Triple(origin, resources, decide)
                            }
                        },
                        siteJsEnabled = siteOverrides[pageHost()]?.javaScript,
                        siteAdBlock = siteOverrides[pageHost()]?.adBlock,
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

    permissionPrompt?.let { (origin, resources, decide) ->
        AlertDialog(
            onDismissRequest = {
                decide(false)
                permissionPrompt = null
            },
            title = { Text("Site izni") },
            text = {
                Text(
                    "$origin şunları istiyor:\n" + resources.joinToString("\n") {
                        when {
                            it.contains("VIDEO", ignoreCase = true) -> "• Kamera"
                            it.contains("AUDIO", ignoreCase = true) -> "• Mikrofon"
                            else -> "• $it"
                        }
                    } + "\n\nSadece bu sayfadayken geçerli olur.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    decide(true)
                    permissionPrompt = null
                }) {
                    Text("İzin ver")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    decide(false)
                    permissionPrompt = null
                }) {
                    Text("Reddet")
                }
            },
        )
    }

    if (deathTriggered) {        AlertDialog(
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
        Dialog(onDismissRequest = {}) {
            val pop by animateFloatAsState(
                targetValue = if (quickClearPhase == 2) 1f else 0.6f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
                label = "quick-clear-pop",
            )
            Card(shape = RoundedCornerShape(28.dp)) {
                Column(
                    modifier = Modifier.padding(horizontal = 36.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                if (quickClearPhase == 2) {
                                    AccentPalette.colorFor(settings.accent)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (quickClearPhase == 1) {
                            CircularProgressIndicator()
                        } else {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .size(40.dp)
                                    .scale(pop),
                            )
                        }
                    }
                    Text(
                        if (quickClearPhase == 1) "Temizleniyor…" else "Temizlendi",
                        style = MaterialTheme.typography.titleMedium,
                    )
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
            onExport = {
                showBookmarks = false
                runCatching { exportLauncher.launch("logix-yer-imleri.html") }
            },
            onImport = {
                showBookmarks = false
                runCatching { importLauncher.launch(arrayOf("text/html")) }
            },
        )
    }

    if (showSiteSettings) {
        val host = pageHost()
        val override = siteOverrides[host]
        SiteSettingsSheet(
            host = host.ifBlank { "?" },
            javaScript = override?.javaScript,
            adBlock = override?.adBlock,
            globalJs = true,
            globalAdBlock = settings.adBlockEnabled,
            onJsChange = { siteVm.setJavaScript(host, it) },
            onAdBlockChange = { siteVm.setAdBlock(host, it) },
            onReset = { siteVm.reset(host) },
            onDismiss = { showSiteSettings = false },
        )
    }

    readerText?.let { (title, text) ->
        ReaderSheet(
            title = title,
            text = text,
            onDismiss = { readerText = null },
        )
    }

    if (!onboarded) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Logix'e hoş geldin") },
            text = {
                Text(
                    "• Verilerin cihazında kalır, hesap gerekmez.\n" +
                        "• Kalkanlar reklam ve izleyicileri engeller.\n" +
                        "• Ölüm Anahtarı'nı açmadan önce uyarıyı oku — geri dönüşü yoktur.",
                )
            },
            confirmButton = {
                TextButton(onClick = settingsVm::setOnboarded) {
                    Text("Başla")
                }
            },
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
                blockedAds = shieldTotals.ads,
                blockedTrackers = shieldTotals.trackers,
                backgroundAudio = settings.backgroundAudio,
                onBackgroundAudioChange = settingsVm::setBackgroundAudio,
                filterRefreshing = filterRefreshing,
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
    onFindResult: (Int, Int) -> Unit,
    onPermissionRequest: (String, List<String>, (Boolean) -> Unit) -> Unit,
    siteJsEnabled: Boolean?,
    siteAdBlock: Boolean?,
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
            onFindResult = onFindResult,
            onPermissionRequest = onPermissionRequest,
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
            siteJsEnabled = siteJsEnabled,
            siteAdBlock = siteAdBlock,
        )
    }
}

@Composable
private fun FindBar(
    query: String,
    onQueryChange: (String) -> Unit,
    resultText: String?,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Search, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onNext() }),
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                "Sayfada ara",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        inner()
                    }
                },
            )
            resultText?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Önceki")
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Sonraki")
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Kapat")
            }
        }
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
