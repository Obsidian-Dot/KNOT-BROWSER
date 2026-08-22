@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.wormhole.browser.ui.browser

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.zIndex
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wormhole.browser.R
import com.wormhole.browser.core.ai.TranslateLanguage
import com.wormhole.browser.core.browser.AiRequestState
import com.wormhole.browser.core.browser.BrowserEvent
import com.wormhole.browser.core.browser.BrowserViewModel
import com.wormhole.browser.core.downloads.DownloadRepository
import com.wormhole.browser.core.security.BiometricAuthenticator
import com.wormhole.browser.core.browser.ExternalIntentLauncher
import com.wormhole.browser.core.browser.SpaceAccent
import com.wormhole.browser.ui.ai.AiSheet
import com.wormhole.browser.ui.downloads.DownloadsSheet
import com.wormhole.browser.ui.library.LibrarySheet
import com.wormhole.browser.ui.settings.SettingsScreen
import com.wormhole.browser.ui.theme.WormHoleMotion
import com.wormhole.browser.ui.theme.bouncyClickable

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel = viewModel(),
    geckoSessionPool: com.wormhole.browser.core.gecko.GeckoSessionPool,
    onWebViewVisibleChanged: (Boolean) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentEngine by viewModel.searchEngine.collectAsState()
    val dynamicBackgroundEnabled by viewModel.dynamicBackgroundEnabled.collectAsState()
    val geminiApiKey by viewModel.geminiApiKey.collectAsState()
    val assistantState by viewModel.assistantState.collectAsState()
    val aiWorking = assistantState is AiRequestState.Loading
    val translateState by viewModel.translateState.collectAsState()
    val shortcuts by viewModel.shortcuts.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val hasStoredRecentSearches by viewModel.hasStoredRecentSearches.collectAsState()
    val trackerBlockingEnabled by viewModel.trackerBlockingEnabled.collectAsState()
    val adBlockingEnabled by viewModel.adBlockingEnabled.collectAsState()
    val popupBlockingEnabled by viewModel.popupBlockingEnabled.collectAsState()
    val webDarkModeEnabled by viewModel.webDarkModeEnabled.collectAsState()

    LaunchedEffect(webDarkModeEnabled) {
        // Firefox website appearance: prefers-color-scheme follows the app.
        com.wormhole.browser.core.gecko.GeckoRuntimeHolder.setContentPrefersDark(webDarkModeEnabled)
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val fragmentActivity = context as? androidx.fragment.app.FragmentActivity

    var isCommandBarOpen by remember { mutableStateOf(false) }
    var commandBarQuery by remember { mutableStateOf("") }

    var commandBarMode by remember { mutableStateOf(CommandBarMode.SEARCH) }

    var aiAnswerQuery by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }
    var showOpenSourceLicenses by remember { mutableStateOf(false) }
    var showPasskeys by remember { mutableStateOf(false) }
    var showExtensions by remember { mutableStateOf(false) }

    var isPasskeysAuthenticated by remember { mutableStateOf(false) }
    var showDownloads by remember { mutableStateOf(false) }
    var showLibrary by remember { mutableStateOf(false) }
    var libraryInitialTab by remember { mutableIntStateOf(0) }
    var isFindInPageOpen by remember { mutableStateOf(false) }
    var isPageToolsMenuOpen by remember { mutableStateOf(false) }

    var isHomeToolsMenuOpen by remember { mutableStateOf(false) }
    var isDesktopSiteEnabled by remember { mutableStateOf(false) }
    var pageZoomPercent by remember { mutableStateOf(100) }
    var isTranslateLanguageSheetOpen by remember { mutableStateOf(false) }
    var isAssistantSheetOpen by remember { mutableStateOf(false) }

    var isAiOpen by remember { mutableStateOf(false) }
    var isTabSwitcherOpen by remember { mutableStateOf(false) }
    var thumbnailCaptureRequest by remember { mutableStateOf(0) }
    LaunchedEffect(isTabSwitcherOpen) {
        if (isTabSwitcherOpen) thumbnailCaptureRequest++
    }

    var isIncognitoConsentPending by remember { mutableStateOf(false) }

    var pendingIncognitoSpaceId by remember { mutableStateOf<String?>(null) }
    val requestNewIncognitoTab: (String) -> Unit = { spaceId ->
        pendingIncognitoSpaceId = spaceId
        isIncognitoConsentPending = true
    }
    var isTranslateSheetOpen by remember { mutableStateOf(false) }

    var pendingDownload by remember { mutableStateOf<BrowserEvent.DownloadRequested?>(null) }
    var webViewRecoveryRevision by remember { mutableStateOf(0) }

    // Tracks the most recent load failure per tab. GeckoView's onLoadError fires
    // when a navigation fails outright (DNS, TLS, connection refused, blocked
    // content, etc.) -- without this, the page never paints and the toolbar
    // still looks "loaded", leaving a blank/dark screen with no explanation.
    val tabLoadErrors = remember { mutableStateMapOf<String, String>() }

    var pendingSslError by remember { mutableStateOf<BrowserEvent.SslErrorOccurred?>(null) }

    var pendingMediaPermission by remember { mutableStateOf<BrowserEvent.MediaPermissionRequested?>(null) }
    var pendingGeolocationPermission by remember { mutableStateOf<BrowserEvent.GeolocationPermissionRequested?>(null) }

    var mediaSiteConsent by remember { mutableStateOf<BrowserEvent.MediaPermissionRequested?>(null) }
    var geolocationSiteConsent by remember { mutableStateOf<BrowserEvent.GeolocationPermissionRequested?>(null) }

    BackHandler(
        enabled = isAiOpen || showSettings || showPasskeys || showDownloads ||
            showLibrary || isTabSwitcherOpen || isCommandBarOpen || isTranslateSheetOpen ||
            isTranslateLanguageSheetOpen || isAssistantSheetOpen || isFindInPageOpen ||
            isPageToolsMenuOpen || isHomeToolsMenuOpen || showExtensions,
    ) {
        when {
            isAiOpen -> isAiOpen = false
            showPasskeys -> showPasskeys = false
            showExtensions -> showExtensions = false
            showSettings -> showSettings = false
            showDownloads -> showDownloads = false
            showLibrary -> showLibrary = false
            isTabSwitcherOpen -> isTabSwitcherOpen = false
            isTranslateLanguageSheetOpen -> isTranslateLanguageSheetOpen = false
            isTranslateSheetOpen -> isTranslateSheetOpen = false
            isAssistantSheetOpen -> isAssistantSheetOpen = false
            isFindInPageOpen -> isFindInPageOpen = false
            isPageToolsMenuOpen -> isPageToolsMenuOpen = false
            isHomeToolsMenuOpen -> isHomeToolsMenuOpen = false
            isCommandBarOpen -> isCommandBarOpen = false
        }
    }

    val activeWebViewCanGoBack = uiState.activeTab?.canGoBack == true
    BackHandler(enabled = activeWebViewCanGoBack) {
        uiState.activeTab?.id?.let { geckoSessionPool.get(it)?.goBack() }
    }

    var downloadToast by remember { mutableStateOf<com.wormhole.browser.core.downloads.DownloadStartResult?>(null) }

    var permissionRequestedDownload by remember { mutableStateOf<BrowserEvent.DownloadRequested?>(null) }
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val download = permissionRequestedDownload
        permissionRequestedDownload = null
        if (granted && download != null) {
            coroutineScope.launch {
                try {
                    downloadToast = DownloadRepository.start(
                        context = context,
                        url = download.url,
                        userAgent = download.userAgent,
                        contentDisposition = download.contentDisposition,
                        mimeType = download.mimeType,
                    )
                } catch (_: Throwable) {
                    downloadToast = null
                }
            }
        }

    }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val pending = pendingMediaPermission
        pendingMediaPermission = null
        if (pending == null) return@rememberLauncherForActivityResult
        if (grants.values.all { it }) {

            mediaSiteConsent = pending
        } else {
            pending.onDeny()
        }
    }

    val geolocationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val pending = pendingGeolocationPermission
        pendingGeolocationPermission = null
        if (pending == null) return@rememberLauncherForActivityResult
        if (grants.values.all { it }) {
            geolocationSiteConsent = pending
        } else {
            pending.onDeny()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {  }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {  }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val hasNotificationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasNotificationPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(dynamicBackgroundEnabled) {
        if (dynamicBackgroundEnabled) {
            val hasLocationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasLocationPermission) {
                locationPermissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BrowserEvent.LaunchExternalApp -> ExternalIntentLauncher.launch(context, event.uri)
                is BrowserEvent.DownloadRequested -> pendingDownload = event
                is BrowserEvent.BlobDownloadReady -> {

                    downloadToast = DownloadRepository.saveBase64(
                        context = context,
                        fileName = event.fileName,
                        mimeType = event.mimeType,
                        base64Data = event.base64Data,
                    )
                }
                is BrowserEvent.BlobDownloadFailed -> Unit
                is BrowserEvent.LoadError -> {
                    tabLoadErrors[event.tabId] = event.message
                }
                is BrowserEvent.SslErrorOccurred -> pendingSslError = event
                is BrowserEvent.MediaPermissionRequested -> {
                    val neededPermissions = event.resources.mapNotNull { resource ->
                        when (resource) {
                            android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE -> Manifest.permission.CAMERA
                            android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE -> Manifest.permission.RECORD_AUDIO
                            else -> null
                        }
                    }.distinct()
                    val alreadyGranted = neededPermissions.isNotEmpty() && neededPermissions.all {
                        androidx.core.content.ContextCompat.checkSelfPermission(context, it) ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                    }
                    when {
                        neededPermissions.isEmpty() -> event.onDeny()
                        alreadyGranted -> mediaSiteConsent = event
                        else -> {
                            pendingMediaPermission = event
                            mediaPermissionLauncher.launch(neededPermissions.toTypedArray())
                        }
                    }
                }
                is BrowserEvent.GeolocationPermissionRequested -> {
                    val neededPermissions = arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    )
                    val alreadyGranted = neededPermissions.all {
                        androidx.core.content.ContextCompat.checkSelfPermission(context, it) ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                    }
                    if (alreadyGranted) {
                        geolocationSiteConsent = event
                    } else {
                        pendingGeolocationPermission = event
                        geolocationPermissionLauncher.launch(neededPermissions)
                    }
                }
                is BrowserEvent.RendererCrashed -> {

                    geckoSessionPool.remove(event.tabId)
                    webViewRecoveryRevision++
                }
            }
        }
    }

    val activeTab = uiState.activeTab

    val isWebViewVisible = activeTab != null && activeTab.url.isNotBlank() && !isTabSwitcherOpen
    LaunchedEffect(isWebViewVisible) { onWebViewVisibleChanged(isWebViewVisible) }

    LaunchedEffect(activeTab?.id) { isDesktopSiteEnabled = false }
    LaunchedEffect(activeTab?.id) {
        pageZoomPercent = 100
    }
    // Clear any stale load-error banner for this tab as soon as a fresh
    // navigation starts (reload, new URL, link tap, etc).
    LaunchedEffect(activeTab?.id, activeTab?.isLoading) {
        if (activeTab != null && activeTab.isLoading) {
            tabLoadErrors.remove(activeTab.id)
        }
    }

    // Gecko finder when Find opens.
    var findInPageController by remember { mutableStateOf<com.wormhole.browser.core.gecko.FindController?>(null) }
    LaunchedEffect(isFindInPageOpen, activeTab?.id) {
        if (!isFindInPageOpen) {
            findInPageController?.stop()
            findInPageController = null
            return@LaunchedEffect
        }
        repeat(30) {
            val tabId = activeTab?.id
            val session = tabId?.let { geckoSessionPool.get(it) }
            if (session != null) {
                val controller = findInPageController
                    ?: com.wormhole.browser.core.gecko.GeckoFindController(session)
                findInPageController = controller
                controller.start()
                return@LaunchedEffect
            }
            kotlinx.coroutines.delay(50)
        }
    }
    DisposableEffect(activeTab?.id) {
        onDispose {
            findInPageController?.stop()
            findInPageController = null
        }
    }

    val density = LocalDensity.current
    var bottomBarHeightPx by remember { mutableStateOf(0) }
    val bottomBarHeight = with(density) { bottomBarHeightPx.toDp() }

    val topInsetPx = WindowInsets.statusBars
        .getTop(density)
        .coerceAtLeast(
            WindowInsets.displayCutout.getTop(density),
        )
    // System navigation bar stays visible and must never be covered by the WebView.
    val navBarBottomPx = WindowInsets.navigationBars.getBottom(density)

    // Iceraven/Mozilla dynamic bottom toolbar (ViewYTranslator + clipping model):
    // - Bar follows scroll 1:1 while dragging.
    // - On finger-up it snaps fully shown or hidden; Gecko clipping follows so
    //   the webpage (ChatGPT composer, etc.) snaps with the chrome.
    val dynamicToolbar = remember { com.wormhole.browser.core.webview.DynamicToolbarController() }
    val toolbarScrollScope = rememberCoroutineScope()
    var toolbarOffsetPx by remember { mutableFloatStateOf(0f) }
    var toolbarScrollY by remember { mutableIntStateOf(0) }
    var toolbarSettleJob by remember { mutableStateOf<Job?>(null) }
    var toolbarSnapTarget by remember { mutableFloatStateOf(Float.NaN) }

    fun cancelToolbarSettle() {
        toolbarSettleJob?.cancel()
        toolbarSettleJob = null
        toolbarSnapTarget = Float.NaN
    }

    fun animateToolbarTo(target: Float) {
        if (bottomBarHeightPx <= 0) {
            toolbarOffsetPx = 0f
            return
        }
        val bounded = target.coerceIn(0f, bottomBarHeightPx.toFloat())
        if (toolbarSettleJob?.isActive == true && kotlin.math.abs(toolbarSnapTarget - bounded) < 0.5f) {
            return
        }
        val start = toolbarOffsetPx
        if (kotlin.math.abs(start - bounded) < 0.5f) {
            toolbarOffsetPx = bounded
            dynamicToolbar.syncTranslation(bounded)
            return
        }
        cancelToolbarSettle()
        toolbarSnapTarget = bounded
        toolbarSettleJob = toolbarScrollScope.launch {
            try {
                animate(
                    initialValue = start,
                    targetValue = bounded,
                    animationSpec = WormHoleMotion.chrome(),
                ) { value, _ ->
                    toolbarOffsetPx = value
                    dynamicToolbar.syncTranslation(value)
                }
            } finally {
                toolbarOffsetPx = bounded
                dynamicToolbar.syncTranslation(bounded)
                toolbarSnapTarget = Float.NaN
            }
        }
    }

    fun applyToolbarDrag(scrollDeltaY: Int, scrollY: Int) {
        toolbarScrollY = scrollY
        dynamicToolbar.syncTranslation(toolbarOffsetPx)
        val next = dynamicToolbar.onScrollDelta(scrollDeltaY, scrollY)
        if (dynamicToolbar.lastIgnored) return

        if (scrollDeltaY < 0) {
            // Scroll up: one committed slide-in. Do not 1:1 track bounce.
            animateToolbarTo(0f)
            return
        }

        cancelToolbarSettle()
        toolbarOffsetPx = next
    }

    fun snapToolbarToRest() {
        if (bottomBarHeightPx <= 0) return
        val target = dynamicToolbar.snapTarget(toolbarScrollY)
        animateToolbarTo(target)
        dynamicToolbar.endGesture()
    }

    LaunchedEffect(bottomBarHeightPx) {
        dynamicToolbar.updateToolbarHeight(bottomBarHeightPx)
        toolbarOffsetPx = toolbarOffsetPx.coerceIn(0f, bottomBarHeightPx.toFloat().coerceAtLeast(0f))
    }
    LaunchedEffect(activeTab?.id) {
        cancelToolbarSettle()
        dynamicToolbar.forceExpand()
        toolbarOffsetPx = 0f
    }
    LaunchedEffect(isPageToolsMenuOpen, isHomeToolsMenuOpen, isFindInPageOpen, isCommandBarOpen) {
        if (isPageToolsMenuOpen || isHomeToolsMenuOpen || isFindInPageOpen || isCommandBarOpen) {
            cancelToolbarSettle()
            val start = toolbarOffsetPx
            dynamicToolbar.forceExpand()
            if (start <= 0.5f) {
                toolbarOffsetPx = 0f
            } else {
                toolbarSettleJob = toolbarScrollScope.launch {
                    animate(
                        initialValue = start,
                        targetValue = 0f,
                        animationSpec = WormHoleMotion.chrome(),
                    ) { value, _ ->
                        toolbarOffsetPx = value
                        dynamicToolbar.syncTranslation(value)
                    }
                    toolbarOffsetPx = 0f
                    dynamicToolbar.syncTranslation(0f)
                }
            }
        }
    }

    // Mozilla EngineViewClippingBehavior equivalent for WebView:
    // bottom inset = visible toolbar height so position:fixed bottom UI (ChatGPT, Copilot)
    // sits above the app bar. Floored at system nav so content never draws under it.
    // Iceraven EngineViewClippingBehavior: clipping = -toolbar.translationY
    val estimatedToolbarPx = with(density) { 110.dp.toPx() }.toInt() + navBarBottomPx
    val dynamicToolbarMaxHeightPx = bottomBarHeightPx.takeIf { it > 0 } ?: estimatedToolbarPx

    Box(
        modifier = Modifier
            .fillMaxSize()

            .background(com.wormhole.browser.ui.theme.WormHoleBarBackground),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(with(density) { topInsetPx.toDp() })
                .background(Color(0xFF2C2C2C))
                .zIndex(30f),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (aiWorking) {
                        Modifier.border(2.dp, Color.White)
                    } else Modifier
                ),
        ) {

        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (activeTab != null) {
                // Do not wrap GeckoView in AnimatedContent/scale — that destroys the
                // TextureView surface and leaves a permanent white page.
                if (activeTab.url.isNotBlank()) {
                        val applyToolbarScroll: (Int, Int) -> Unit = applyScroll@{ scrollDeltaY, scrollY ->
                            if (isPageToolsMenuOpen || isFindInPageOpen || isCommandBarOpen) return@applyScroll
                            applyToolbarDrag(scrollDeltaY, scrollY)
                        }
                        WormHoleGeckoViewHost(
                            tab = activeTab,
                            sessionPool = geckoSessionPool,
                            callbacks = viewModel,
                            dynamicToolbarMaxHeightPx = dynamicToolbarMaxHeightPx,
                            toolbarTranslationYPx = toolbarOffsetPx,
                            minReservedBottomPx = navBarBottomPx,
                            topClippingPx = 0,
                            popupBlockingEnabled = popupBlockingEnabled,
                            thumbnailCaptureRequest = thumbnailCaptureRequest,
                            onScroll = onScroll@{ scrollDeltaY, scrollY, isScrollable ->
                                if (!isScrollable && scrollY <= 8) {
                                    cancelToolbarSettle()
                                    dynamicToolbar.forceExpand()
                                    toolbarOffsetPx = 0f
                                    return@onScroll
                                }
                                applyToolbarScroll(scrollDeltaY, scrollY)
                            },
                            onScrollSettled = { snapToolbarToRest() },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = with(density) { topInsetPx.toDp() }),
                        )

                        val activeError = tabLoadErrors[activeTab.id]
                        if (activeError != null && !activeTab.isLoading) {
                            LoadErrorOverlay(
                                message = activeError,
                                onRetry = {
                                    tabLoadErrors.remove(activeTab.id)
                                    geckoSessionPool.get(activeTab.id)?.reload()
                                },
                            )
                        }
                    } else if (activeTab.isIncognito) {
                        IncognitoHomeSurface(
                            tabCount = uiState.tabs.count { it.isIncognito },
                            onSearchClick = {
                                commandBarQuery = ""
                                commandBarMode = CommandBarMode.SEARCH
                                isCommandBarOpen = true
                            },
                            onTabSwitcherClick = { isTabSwitcherOpen = true },
                            onMenuClick = { isHomeToolsMenuOpen = true },
                            isMenuOpen = isHomeToolsMenuOpen,
                            onMenuDismiss = { isHomeToolsMenuOpen = false },
                            onDownloadsClick = { isHomeToolsMenuOpen = false; showDownloads = true },
                            onLibraryClick = { isHomeToolsMenuOpen = false; libraryInitialTab = 0; showLibrary = true },
                            onPasswordsClick = { isHomeToolsMenuOpen = false; showPasskeys = true },
                            onSettingsClick = { isHomeToolsMenuOpen = false; showSettings = true },
                            onExtensionsClick = { isHomeToolsMenuOpen = false; showExtensions = true },
                            onNewIncognitoTabClick = {
                                isHomeToolsMenuOpen = false
                                requestNewIncognitoTab(uiState.activeSpaceId)
                            },
                            onAssistantClick = { isHomeToolsMenuOpen = false; isAiOpen = true },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        NewTabSurface(
                            activeSpace = uiState.activeSpace,
                            shortcuts = shortcuts,
                            history = viewModel.history.collectAsState().value,
                            onCommandBarRequested = {
                                commandBarQuery = ""
                                commandBarMode = CommandBarMode.SEARCH
                                isCommandBarOpen = true
                            },
                            onShortcutClick = { shortcut ->
                                activeTab?.let { tab ->
                                    geckoSessionPool.requestLoad(tab.id, shortcut.url)
                                    viewModel.updateTabUrl(tab.id, shortcut.url)
                                }
                            },
                            onShortcutRemove = { shortcut -> viewModel.removeShortcut(shortcut.url) },
                            onAddShortcut = { title, url -> viewModel.addShortcut(title, url) },
                            onTrendingSearch = { term ->
                                val tab = activeTab ?: viewModel.newTab(spaceId = uiState.activeSpaceId)
                                val resolved = viewModel.resolveInput(term)
                                viewModel.updateTabUrl(tab.id, resolved)
                                geckoSessionPool.requestLoad(tab.id, resolved)
                            },
                            onAskWormHoleClick = { isAiOpen = true },
                            onHistoryClick = { entry ->
                                val tab = activeTab ?: viewModel.newTab(spaceId = uiState.activeSpaceId)
                                geckoSessionPool.requestLoad(tab.id, entry.url)
                                viewModel.updateTabUrl(tab.id, entry.url)
                            },
                            searchEngine = currentEngine,
                            tabCount = uiState.tabs.size,
                            onTabSwitcherClick = { isTabSwitcherOpen = true },
                            canGoBack = activeTab?.canGoBack == true,
                            canGoForward = activeTab?.canGoForward == true,
                            onBackClick = {
                                activeTab?.let { tab ->
                                    geckoSessionPool.get(tab.id)?.goBack()
                                }
                            },
                            onForwardClick = {
                                activeTab?.let { tab ->
                                    geckoSessionPool.get(tab.id)?.goForward()
                                }
                            },
                            isMenuOpen = isHomeToolsMenuOpen,
                            onMenuButtonClick = { isHomeToolsMenuOpen = true },
                            onMenuDismiss = { isHomeToolsMenuOpen = false },
                            onDownloadsClick = { isHomeToolsMenuOpen = false; showDownloads = true },
                            onLibraryClick = { isHomeToolsMenuOpen = false; libraryInitialTab = 0; showLibrary = true },
                            onPasswordsClick = { isHomeToolsMenuOpen = false; showPasskeys = true },
                            onSettingsClick = { isHomeToolsMenuOpen = false; showSettings = true },
                            onExtensionsClick = { isHomeToolsMenuOpen = false; showExtensions = true },
                            onNewIncognitoTabClick = {
                                isHomeToolsMenuOpen = false
                                requestNewIncognitoTab(uiState.activeSpaceId)
                            },

                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                if (activeTab?.isLoading == true) {

                    LinearProgressIndicator(
                        progress = { activeTab.loadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(2.dp),
                        color = uiState.activeSpace?.accent?.color ?: MaterialTheme.colorScheme.primary,
                    )
                }

                if (isFindInPageOpen) {
                    val controller = findInPageController
                    if (controller != null) {
                        FindInPageBar(
                            controller = controller,
                            onClose = {
                                controller.stop()
                                isFindInPageOpen = false
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = bottomBarHeight + 12.dp)
                                .fillMaxWidth()
                                .zIndex(8f),
                        )
                    }
                }

                // Always compose the bar while a page is open so bottomBarHeightPx stays
                // accurate. Visibility is driven purely by toolbarOffset (hide = slide down).
                if (activeTab?.url?.isNotBlank() == true) {
                BottomBar(
                    isMenuOpen = isPageToolsMenuOpen,
                    isDesktopSiteEnabled = isDesktopSiteEnabled,
                    tabCount = uiState.tabs.size,
                    displayUrl = activeTab?.displayUrl.orEmpty(),
                    isSecure = activeTab?.isSecure == true,
                    onAddressBarClick = {
                        commandBarQuery = activeTab?.url.orEmpty()
                        commandBarMode = CommandBarMode.SEARCH
                        isCommandBarOpen = true
                    },
                    onBackClick = {

                        activeTab?.let { tab ->
                            geckoSessionPool.get(tab.id)?.goBack()
                        }
                    },
                    onForwardClick = {
                        activeTab?.let { tab ->
                            geckoSessionPool.get(tab.id)?.goForward()
                        }
                    },
                    onReloadClick = { activeTab?.let { geckoSessionPool.get(it.id)?.reload() } },
                    onStopLoadingClick = { activeTab?.let { geckoSessionPool.get(it.id)?.stop() } },
                    isLoading = activeTab?.isLoading == true,
                    canGoBack = activeTab?.canGoBack == true,
                    canGoForward = activeTab?.canGoForward == true,
                    onTabSwitcherClick = { isTabSwitcherOpen = true },
                    onNewTabFromBarClick = {
                        viewModel.newTab(spaceId = uiState.activeSpaceId)
                    },
                    onHomeClick = {
                        activeTab?.let { tab ->
                            viewModel.goHome(tab.id)
                            geckoSessionPool.goHome(tab.id)
                            cancelToolbarSettle()
                            dynamicToolbar.forceExpand()
                            toolbarOffsetPx = 0f
                        }
                    },
                    onMenuButtonClick = { isPageToolsMenuOpen = true },
                    onMenuDismiss = { isPageToolsMenuOpen = false },
                    onDownloadsClick = {
                        showDownloads = true
                    },
                    onLibraryClick = {
                        libraryInitialTab = 0
                        showLibrary = true
                    },
                    onHistoryClick = {
                        libraryInitialTab = 1
                        showLibrary = true
                    },
                    onPasswordsClick = {
                        showPasskeys = true
                    },
                    onBookmarkClick = {
                        val tab = activeTab
                        if (tab == null || tab.url.isBlank()) {
                            android.widget.Toast.makeText(context, "Nothing to bookmark", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addBookmark(tab)
                            android.widget.Toast.makeText(context, "Bookmark saved", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    onAddShortcutClick = {
                        val tab = activeTab
                        if (tab == null || tab.url.isBlank()) {
                            android.widget.Toast.makeText(context, "Nothing to pin", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addShortcut(tab.title.ifBlank { tab.url }, tab.url)
                            android.widget.Toast.makeText(context, "Added to Shortcuts", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDuplicateTabClick = {
                        val tab = activeTab
                        if (tab == null) {
                            android.widget.Toast.makeText(context, "No tab to duplicate", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.duplicateTab(tab)
                            // Load the same URL into the new tab's session once it appears
                            android.widget.Toast.makeText(context, "Tab duplicated", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    onReopenClosedTabClick = {
                        val hadClosed = uiState.recentlyClosedTabs.isNotEmpty()
                        viewModel.reopenClosedTab()
                        android.widget.Toast.makeText(
                            context,
                            if (hadClosed) "Tab restored" else "No recently closed tabs",
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                    },
                    onNewIncognitoTabClick = {
                        requestNewIncognitoTab(uiState.activeSpaceId)
                    },
                    onRequestDesktopSiteClick = {
                        val tab = activeTab
                        if (tab != null) {
                        val enable = !isDesktopSiteEnabled
                        val applied = geckoSessionPool.setDesktopMode(tab.id, enable)
                        isDesktopSiteEnabled = enable
                        // Also push viewport hint via bridge for the current page
                        val session = geckoSessionPool.get(tab.id)
                        if (session != null) {
                            coroutineScope.launch {
                                runCatching {
                                    com.wormhole.browser.core.gecko.GeckoExtensionBridge.send(
                                        session,
                                        "set_desktop_viewport",
                                        mapOf("desktop" to enable.toString()),
                                    )
                                }
                            }
                        }
                        android.widget.Toast.makeText(
                            context,
                            if (enable) "Desktop site requested" else "Mobile site requested",
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                        }
                    },
                    onTranslateClick = {
                        if (activeTab?.url.isNullOrBlank()) {
                            android.widget.Toast.makeText(context, "Open a page to translate", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            isTranslateLanguageSheetOpen = true
                        }
                    },
                    onFindInPageClick = {
                        if (activeTab?.url.isNullOrBlank()) {
                            android.widget.Toast.makeText(context, "Open a page first", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            isFindInPageOpen = true
                        }
                    },
                    pageZoomPercent = pageZoomPercent,
                    onZoomChange = { percent ->
                        pageZoomPercent = percent
                        val factor = (percent / 100.0).toString()
                        val session = activeTab?.id?.let { geckoSessionPool.get(it) }
                        if (session != null) {
                            coroutineScope.launch {
                                runCatching {
                                    com.wormhole.browser.core.gecko.GeckoExtensionBridge.send(
                                        session,
                                        "set_zoom",
                                        mapOf("factor" to factor),
                                    )
                                }
                            }
                        }
                    },
                    onAssistantClick = {
                        isAiOpen = true
                    },
                    onSettingsClick = {
                        showSettings = true
                    },
                    onExtensionsClick = {
                        showExtensions = true
                    },
                    onShareClick = {
                        val url = activeTab?.url.orEmpty()
                        if (url.isBlank()) {
                            android.widget.Toast.makeText(context, "Nothing to share", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            runCatching {
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, url)
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, activeTab?.title.orEmpty())
                                }
                                context.startActivity(
                                    android.content.Intent.createChooser(shareIntent, "Share link").apply {
                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    },
                                )
                            }.onFailure {
                                android.widget.Toast.makeText(context, "Could not open share sheet", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onCopyLinkClick = {
                        val url = activeTab?.url.orEmpty()
                        if (url.isBlank()) {
                            android.widget.Toast.makeText(context, "Nothing to copy", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            runCatching {
                                val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                                clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("Link", url))
                                android.widget.Toast.makeText(context, "Link copied", android.widget.Toast.LENGTH_SHORT).show()
                            }.onFailure {
                                android.widget.Toast.makeText(context, "Copy failed", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .graphicsLayer { translationY = toolbarOffsetPx }
                        .onSizeChanged { bottomBarHeightPx = it.height },
                )
                }

                CommandBar(
                    isOpen = isCommandBarOpen,
                    query = commandBarQuery,
                    mode = commandBarMode,
                    onModeChange = { commandBarMode = it },
                    onQueryChange = { commandBarQuery = it },
                    onSearchOmnibox = { query -> viewModel.searchOmnibox(query) },
                    onFetchSearchSuggestions = { query -> viewModel.fetchSearchSuggestions(query) },
                    searchEngine = currentEngine,
                    recentSearches = recentSearches,
                    shortcuts = shortcuts,
                    onFillQuery = { commandBarQuery = it },
                    onClearRecentSearches = { viewModel.clearRecentSearches() },
                    onShortcutClick = { shortcut ->
                        val tab = activeTab ?: viewModel.newTab(spaceId = uiState.activeSpaceId)
                        geckoSessionPool.requestLoad(tab.id, shortcut.url)
                        viewModel.updateTabUrl(tab.id, shortcut.url)
                        isCommandBarOpen = false
                    },
                    onAddShortcut = { title, url -> viewModel.addShortcut(title, url) },
                    hasStoredRecentSearches = hasStoredRecentSearches,
                    onSubmit = { input ->
                        if (input.isBlank()) return@CommandBar
                        when (commandBarMode) {
                            CommandBarMode.SEARCH -> {
                                viewModel.recordTypedQueryIfSearch(input)
                                val tab = activeTab ?: viewModel.newTab(spaceId = uiState.activeSpaceId)
                                val resolved = viewModel.resolveInput(input)
                                viewModel.updateTabUrl(tab.id, resolved)
                                geckoSessionPool.requestLoad(tab.id, resolved)
                            }
                            CommandBarMode.AI -> {
                                viewModel.recordTypedQueryIfSearch(input)
                                aiAnswerQuery = input
                            }
                        }
                        isCommandBarOpen = false
                    },
                    onDismiss = { isCommandBarOpen = false },
                    modifier = Modifier.fillMaxSize(),
                )

                if (isTranslateLanguageSheetOpen) {
                    TranslateLanguageSheet(
                        onLanguageSelected = { language: TranslateLanguage ->
                            isTranslateLanguageSheetOpen = false
                            val tab = activeTab
                            val session = tab?.id?.let { geckoSessionPool.get(it) }
                            if (session == null) {
                                viewModel.resetTranslateState()
                            } else {
                                isTranslateSheetOpen = true
                                viewModel.setTranslateLoading()
                                coroutineScope.launch {
                                    val pageText = com.wormhole.browser.core.gecko.GeckoJs.evaluate(
                                        session,
                                        "(function(){try{return (document.body&&document.body.innerText)||'';}catch(e){return ''}})()",
                                    )
                                    viewModel.translatePage(pageText, language)
                                }
                            }
                        },
                        onDismiss = { isTranslateLanguageSheetOpen = false },
                    )
                }

                if (isAssistantSheetOpen) {
                    AiResultSheet(
                        title = "Page summary",
                        state = assistantState,
                        onDismiss = {
                            isAssistantSheetOpen = false
                            viewModel.resetAssistantState()
                        },
                    )
                }

                if (isTranslateSheetOpen) {
                    AiResultSheet(
                        title = "Translation",
                        state = translateState,
                        onDismiss = {
                            isTranslateSheetOpen = false
                            viewModel.resetTranslateState()
                        },
                    )
                }

                if (isIncognitoConsentPending) {
                    IncognitoConsentDialog(
                        onAgree = {
                            isIncognitoConsentPending = false
                            viewModel.newTab(
                                spaceId = pendingIncognitoSpaceId ?: uiState.activeSpaceId,
                                incognito = true,
                            )
                            pendingIncognitoSpaceId = null
                        },
                        onDecline = {
                            isIncognitoConsentPending = false

                            viewModel.newTab(
                                spaceId = pendingIncognitoSpaceId ?: uiState.activeSpaceId,
                                incognito = false,
                            )
                            pendingIncognitoSpaceId = null
                        },
                    )
                }

                pendingDownload?.let { download ->
                    DownloadConfirmSheet(
                        fileName = DownloadRepository.guessFileName(download.url, download.contentDisposition, download.mimeType),
                        sourceUrl = download.url,
                        contentLength = download.contentLength,
                        mimeType = download.mimeType,
                        onConfirm = {
                            pendingDownload = null
                            val needsPermission = DownloadRepository.needsStoragePermission() &&
                                androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (needsPermission) {
                                permissionRequestedDownload = download
                                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            } else {

                                coroutineScope.launch {
                                    try {
                                        downloadToast = DownloadRepository.start(
                                            context = context,
                                            url = download.url,
                                            userAgent = download.userAgent,
                                            contentDisposition = download.contentDisposition,
                                            mimeType = download.mimeType,
                                        )
                                        if (downloadToast == null) {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Couldn't start download",
                                                android.widget.Toast.LENGTH_SHORT,
                                            ).show()
                                        }
                                    } catch (e: Throwable) {
                                        android.widget.Toast.makeText(
                                            context,
                                            e.message ?: "Download failed",
                                            android.widget.Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }
                            }
                        },
                        onDismiss = { pendingDownload = null },
                    )
                }

                pendingSslError?.let { sslError ->
                    SslWarningSheet(
                        url = sslError.url,
                        primaryErrorCode = sslError.primaryErrorCode,
                        onGoBack = {
                            pendingSslError = null
                            sslError.onCancel()
                        },
                    )
                }

                mediaSiteConsent?.let { request ->
                    val grantableResources = request.resources.filter { resource ->
                        when (resource) {
                            android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE,
                            android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE -> true
                            else -> false
                        }
                    }
                    val kinds = grantableResources.mapNotNull { resource ->
                        when (resource) {
                            android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE -> SitePermissionKind.CAMERA
                            android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE -> SitePermissionKind.MICROPHONE
                            else -> null
                        }
                    }.distinct()
                    SitePermissionSheet(
                        origin = request.origin,
                        kinds = kinds.ifEmpty { listOf(SitePermissionKind.CAMERA) },
                        onAllow = {
                            mediaSiteConsent = null

                            request.onGrant(grantableResources)
                        },
                        onDeny = {
                            mediaSiteConsent = null
                            request.onDeny()
                        },
                    )
                }

                geolocationSiteConsent?.let { request ->
                    SitePermissionSheet(
                        origin = request.origin,
                        kinds = listOf(SitePermissionKind.LOCATION),
                        onAllow = {
                            geolocationSiteConsent = null

                            request.onAllow(false)
                        },
                        onDeny = {
                            geolocationSiteConsent = null
                            request.onDeny()
                        },
                    )
                }

                DownloadToast(
                    fileName = downloadToast?.fileName.orEmpty(),
                    mimeType = downloadToast?.mimeType,
                    visible = downloadToast != null,
                    onClick = {
                        downloadToast = null
                        showDownloads = true
                    },
                    onDismiss = { downloadToast = null },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = bottomBarHeight),
                )
            }
        }

        AnimatedVisibility(
            visible = isAiOpen,
            enter = fadeIn(animationSpec = WormHoleMotion.settled()),
            exit = fadeOut(animationSpec = WormHoleMotion.settled()),
        ) {
            AiSheet(
                apiKey = geminiApiKey,
                activeTab = activeTab,
                viewModel = viewModel,
                geckoSessionPool = geckoSessionPool,
                onSummarise = {
                    isAiOpen = false
                    val tab = activeTab
                    val session = tab?.id?.let { geckoSessionPool.get(it) }
                    if (session == null) {
                        viewModel.resetAssistantState()
                    } else {
                        isAssistantSheetOpen = true
                        viewModel.setAssistantLoading()
                        coroutineScope.launch {
                            val pageText = com.wormhole.browser.core.gecko.GeckoJs.evaluate(
                                session,
                                "(function(){try{return (document.body&&document.body.innerText)||'';}catch(e){return ''}})()",
                            )
                            viewModel.summarizePage(pageText)
                        }
                    }
                },
                onTranslate = {
                    isAiOpen = false
                    isTranslateLanguageSheetOpen = true
                },
                onDismiss = { isAiOpen = false },
            )
        }

        val currentAiAnswerQuery = aiAnswerQuery
        if (currentAiAnswerQuery != null) {
            LaunchedEffect(currentAiAnswerQuery) {
                viewModel.askWormHole(currentAiAnswerQuery)
            }
            val aiAnswerState by viewModel.aiAnswerState.collectAsState()
            AiAnswerScreen(
                query = currentAiAnswerQuery,
                state = aiAnswerState,
                onBack = {
                    aiAnswerQuery = null
                    viewModel.resetAiAnswerState()
                },
                onAskFollowUp = {

                    aiAnswerQuery = null
                    viewModel.resetAiAnswerState()
                    commandBarQuery = currentAiAnswerQuery
                    commandBarMode = CommandBarMode.AI
                    isCommandBarOpen = true
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (isTabSwitcherOpen) {
            TabSwitcherOverlay(
                tabs = uiState.visibleTabs,
                activeTabId = uiState.activeTabId,
                onTabSelected = {
                    viewModel.selectTab(it)
                    isTabSwitcherOpen = false
                },
                onTabClosed = { tabId ->

                    geckoSessionPool.remove(tabId)
                    viewModel.closeTab(tabId)
                },
                onNewTab = {
                    viewModel.newTab(spaceId = uiState.activeSpaceId)
                    isTabSwitcherOpen = false
                },
                onNewIncognitoTab = {
                    requestNewIncognitoTab(uiState.activeSpaceId)
                    isTabSwitcherOpen = false
                },
                onHistory = {
                    isTabSwitcherOpen = false
                    showLibrary = true
                },
                onClose = { isTabSwitcherOpen = false },
                onCloseAllTabs = { closeIncognito ->
                    val idsToClose = uiState.visibleTabs
                        .filter { it.isIncognito == closeIncognito }
                        .map { it.id }
                    idsToClose.forEach { geckoSessionPool.remove(it) }
                    viewModel.closeAllTabsInSpace(uiState.activeSpaceId, incognitoOnly = closeIncognito)
                },
            )
        }

        AnimatedVisibility(
            visible = showSettings,
            enter = fadeIn(animationSpec = WormHoleMotion.settled()) +
                slideInHorizontally(animationSpec = WormHoleMotion.bouncy(), initialOffsetX = { it / 3 }),
            exit = fadeOut(animationSpec = WormHoleMotion.settled()) +
                slideOutHorizontally(animationSpec = WormHoleMotion.bouncy(), targetOffsetX = { it / 3 }),
        ) {
            SettingsScreen(
                currentEngine = currentEngine,
                onEngineSelected = viewModel::setSearchEngine,
                geminiApiKey = geminiApiKey,
                onGeminiApiKeyChanged = viewModel::setGeminiApiKey,
                trackerBlockingEnabled = trackerBlockingEnabled,
                onTrackerBlockingChanged = viewModel::setTrackerBlockingEnabled,
                adBlockingEnabled = adBlockingEnabled,
                onAdBlockingChanged = viewModel::setAdBlockingEnabled,
                popupBlockingEnabled = popupBlockingEnabled,
                onPopupBlockingChanged = viewModel::setPopupBlockingEnabled,
                webDarkModeEnabled = webDarkModeEnabled,
                onWebDarkModeChanged = viewModel::setWebDarkModeEnabled,
                onPasskeysClick = { showPasskeys = true },
                onExtensionsClick = { showExtensions = true },
                onClearBrowsingData = { viewModel.clearAllBrowsingData() },
                onPrivacyPolicyClick = { showPrivacyPolicy = true },
                onTermsClick = { showTerms = true },
                onOpenSourceLicensesClick = { showOpenSourceLicenses = true },
                onBack = { showSettings = false },
                hasDiagnosticReport = com.wormhole.browser.core.crash.CrashHandler.hasReports(context),
                onShareDiagnosticReport = {
                    val report = com.wormhole.browser.core.crash.CrashHandler.latestReport(context)
                    if (!report.isNullOrBlank()) {
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, report)
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "WormHole diagnostic report")
                        }
                        runCatching {
                            context.startActivity(
                                android.content.Intent.createChooser(shareIntent, "Share diagnostic report"),
                            )
                        }
                    }
                },
            )
        }

        AnimatedVisibility(
            visible = showPrivacyPolicy,
            enter = fadeIn(animationSpec = WormHoleMotion.settled()) +
                slideInHorizontally(animationSpec = WormHoleMotion.bouncy(), initialOffsetX = { it / 3 }),
            exit = fadeOut(animationSpec = WormHoleMotion.settled()) +
                slideOutHorizontally(animationSpec = WormHoleMotion.bouncy(), targetOffsetX = { it / 3 }),
        ) {
            com.wormhole.browser.ui.settings.PrivacyPolicyScreen(onBack = { showPrivacyPolicy = false })
        }

        AnimatedVisibility(
            visible = showTerms,
            enter = fadeIn(animationSpec = WormHoleMotion.settled()) +
                slideInHorizontally(animationSpec = WormHoleMotion.bouncy(), initialOffsetX = { it / 3 }),
            exit = fadeOut(animationSpec = WormHoleMotion.settled()) +
                slideOutHorizontally(animationSpec = WormHoleMotion.bouncy(), targetOffsetX = { it / 3 }),
        ) {
            com.wormhole.browser.ui.settings.TermsOfServiceScreen(onBack = { showTerms = false })
        }

        AnimatedVisibility(
            visible = showOpenSourceLicenses,
            enter = fadeIn(animationSpec = WormHoleMotion.settled()) +
                slideInHorizontally(animationSpec = WormHoleMotion.bouncy(), initialOffsetX = { it / 3 }),
            exit = fadeOut(animationSpec = WormHoleMotion.settled()) +
                slideOutHorizontally(animationSpec = WormHoleMotion.bouncy(), targetOffsetX = { it / 3 }),
        ) {
            com.wormhole.browser.ui.settings.OpenSourceLicensesScreen(onBack = { showOpenSourceLicenses = false })
        }

        LaunchedEffect(showPasskeys) {
            if (!showPasskeys) {
                isPasskeysAuthenticated = false
                return@LaunchedEffect
            }
            val activity = fragmentActivity
            if (activity == null || !BiometricAuthenticator.isAvailable(activity)) {

                isPasskeysAuthenticated = true
                return@LaunchedEffect
            }
            BiometricAuthenticator.authenticate(
                activity = activity,
                title = "Unlock Passkeys",
                subtitle = "Verify it's you to manage passkeys and passwords",
                onSuccess = { isPasskeysAuthenticated = true },
                onFailure = { showPasskeys = false },
                // There's no in-place "retry" UI for a locked-but-open Passkeys sheet
                // today, so backing out of the prompt still closes it -- but the user
                // can immediately reopen it (unlike before, this is now only reachable
                // via genuine cancellation vs. a failed biometric attempt, which future
                // UI can distinguish, e.g. showing a "try again" state instead).
                onCancelled = { showPasskeys = false },
            )
        }
        AnimatedVisibility(
            visible = showPasskeys && isPasskeysAuthenticated,
            enter = fadeIn(animationSpec = WormHoleMotion.settled()) +
                slideInHorizontally(animationSpec = WormHoleMotion.bouncy(), initialOffsetX = { it / 3 }),
            exit = fadeOut(animationSpec = WormHoleMotion.settled()) +
                slideOutHorizontally(animationSpec = WormHoleMotion.bouncy(), targetOffsetX = { it / 3 }),
        ) {
            com.wormhole.browser.ui.settings.PasskeysScreen(onBack = { showPasskeys = false })
        }

        AnimatedVisibility(
            visible = showExtensions,
            enter = fadeIn(animationSpec = WormHoleMotion.settled()) +
                slideInHorizontally(animationSpec = WormHoleMotion.bouncy(), initialOffsetX = { it / 3 }),
            exit = fadeOut(animationSpec = WormHoleMotion.settled()) +
                slideOutHorizontally(animationSpec = WormHoleMotion.bouncy(), targetOffsetX = { it / 3 }),
        ) {
            com.wormhole.browser.ui.settings.ExtensionsScreen(onBack = { showExtensions = false })
        }

        AnimatedVisibility(
            visible = showDownloads,
            enter = fadeIn(animationSpec = WormHoleMotion.settled()),
            exit = fadeOut(animationSpec = WormHoleMotion.settled()),
        ) {
            DownloadsSheet(onDismiss = { showDownloads = false })
        }

        AnimatedVisibility(
            visible = showLibrary,
            enter = fadeIn(animationSpec = WormHoleMotion.settled()) +
                slideInHorizontally(animationSpec = WormHoleMotion.bouncy(), initialOffsetX = { it / 3 }),
            exit = fadeOut(animationSpec = WormHoleMotion.settled()) +
                slideOutHorizontally(animationSpec = WormHoleMotion.bouncy(), targetOffsetX = { it / 3 }),
        ) {
            LibrarySheet(
                bookmarks = viewModel.bookmarks.collectAsState().value,
                history = viewModel.history.collectAsState().value,
                initialTab = libraryInitialTab,
                onDismiss = { showLibrary = false },
                onOpen = { url ->
                    activeTab?.let { tab ->
                        geckoSessionPool.requestLoad(tab.id, url)
                        viewModel.updateTabUrl(tab.id, url)
                    }
                    showLibrary = false
                },
                onRemoveBookmark = viewModel::removeBookmark,
                onClearHistory = viewModel::clearHistory,
            )
        }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BottomBar(
    tabCount: Int,
    displayUrl: String,
    isSecure: Boolean,
    onAddressBarClick: () -> Unit,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    onReloadClick: () -> Unit,
    onStopLoadingClick: () -> Unit,
    isLoading: Boolean,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isMenuOpen: Boolean,
    isDesktopSiteEnabled: Boolean,
    onTabSwitcherClick: () -> Unit,
    onNewTabFromBarClick: () -> Unit,
    onHomeClick: () -> Unit,
    onMenuButtonClick: () -> Unit,
    onMenuDismiss: () -> Unit,
    onDownloadsClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onHistoryClick: () -> Unit = onLibraryClick,
    onPasswordsClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onAddShortcutClick: () -> Unit,
    onDuplicateTabClick: () -> Unit,
    onReopenClosedTabClick: () -> Unit,
    onNewIncognitoTabClick: () -> Unit,
    onRequestDesktopSiteClick: () -> Unit,
    onTranslateClick: () -> Unit,
    onFindInPageClick: () -> Unit,
    onAssistantClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onExtensionsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    onShareClick: () -> Unit = {},
    onCopyLinkClick: () -> Unit = {},
    pageZoomPercent: Int = 100,
    onZoomChange: (Int) -> Unit = {},
) {

    val adaptiveIconColor = com.wormhole.browser.ui.theme.WormHoleBarContent
    val accent = Color.White

    Box(modifier = modifier) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(com.wormhole.browser.ui.theme.WormHoleBarBackground)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .navigationBarsPadding(),
    ) {
        // Single-row bar: back, forward, search/AI pill, tab count, menu.
        // Flush edge-to-edge tray, rounded only at the top corners.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = if (canGoBack) accent else adaptiveIconColor.copy(alpha = 0.35f),
                modifier = Modifier
                    .size(22.dp)
                    .bouncyClickable(onClick = onBackClick),
            )

            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Forward",
                tint = if (canGoForward) accent else adaptiveIconColor.copy(alpha = 0.35f),
                modifier = Modifier
                    .size(22.dp)
                    .bouncyClickable(onClick = onForwardClick),
            )

            // Search / address pill. The small leading icon opens the AI sheet directly;
            // tapping the rest of the pill opens the normal search/address bar.
            Surface(
                shape = RoundedCornerShape(50),
                color = com.wormhole.browser.ui.theme.WormHoleSurface.Fill,
                border = com.wormhole.browser.ui.theme.WormHoleSurface.border(),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .bouncyClickable(onClick = onAddressBarClick)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.ic_wormhole_glyph),
                        contentDescription = "Ask AI",
                        tint = accent,
                        modifier = Modifier
                            .size(16.dp)
                            .bouncyClickable(onClick = onAssistantClick),
                    )
                    if (isSecure) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Secure connection",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                    Text(
                        text = displayUrl.ifBlank { "Search or type URL" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (displayUrl.isBlank()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(30.dp)
                    .border(
                        width = 1.6.dp,
                        color = adaptiveIconColor,
                        shape = RoundedCornerShape(7.dp),
                    )
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onTabSwitcherClick,
                        onLongClick = onNewTabFromBarClick,
                        onClickLabel = "Tab switcher, ${tabCount.coerceAtLeast(1)} " +
                            if (tabCount.coerceAtLeast(1) == 1) "tab open" else "tabs open",
                        onLongClickLabel = "New tab",
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tabCount.coerceAtLeast(1).toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = adaptiveIconColor,
                )
            }

            Icon(
                Icons.Default.Menu,
                contentDescription = "Menu",
                tint = adaptiveIconColor,
                modifier = Modifier
                    .size(22.dp)
                    .bouncyClickable(onClick = onMenuButtonClick),
            )
        }
    }
    PageToolsMenu(
        isExpanded = isMenuOpen,
        onReloadClick = onReloadClick,
        isDesktopSiteEnabled = isDesktopSiteEnabled,
        onDismiss = onMenuDismiss,
        onDownloadsClick = onDownloadsClick,
        onLibraryClick = onLibraryClick,
        onHistoryClick = onHistoryClick,
        onPasswordsClick = onPasswordsClick,
        onBookmarkClick = onBookmarkClick,
        onAddShortcutClick = onAddShortcutClick,
        onDuplicateTabClick = onDuplicateTabClick,
        onReopenClosedTabClick = onReopenClosedTabClick,
        onNewIncognitoTabClick = onNewIncognitoTabClick,
        onRequestDesktopSiteClick = onRequestDesktopSiteClick,
        onTranslateClick = onTranslateClick,
        onFindInPageClick = onFindInPageClick,
        onAssistantClick = onAssistantClick,
        onSettingsClick = onSettingsClick,
        onExtensionsClick = onExtensionsClick,
        onShareClick = onShareClick,
        onCopyLinkClick = onCopyLinkClick,
        pageZoomPercent = pageZoomPercent,
        onZoomChange = onZoomChange,
    )
    }
}

@Composable
private fun TabSwitcherOverlay(
    tabs: List<com.wormhole.browser.core.browser.Tab>,
    activeTabId: String?,
    onTabSelected: (String) -> Unit,
    onTabClosed: (String) -> Unit,
    onNewTab: () -> Unit,
    onNewIncognitoTab: () -> Unit,
    onHistory: () -> Unit,
    onClose: () -> Unit,
    onCloseAllTabs: (incognito: Boolean) -> Unit = {},
) {
    var incognito by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var isOverflowMenuOpen by remember { mutableStateOf(false) }
    var showCloseAllConfirm by remember { mutableStateOf(false) }
    val visibleTabs = tabs.filter { it.isIncognito == incognito }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = com.wormhole.browser.ui.theme.WormHoleBarBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CircularIconButton(
                    icon = Icons.Default.History,
                    contentDescription = "History",
                    onClick = onHistory,
                )

                Surface(
                    shape = RoundedCornerShape(27.dp),
                    color = Color(0xFF141414),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier

                        .weight(1f, fill = false)
                        .widthIn(max = 270.dp)
                        .height(54.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TabModeChip(
                            text = "${tabs.count { !it.isIncognito }} Tabs",
                            selected = !incognito,
                            onClick = { incognito = false },
                            modifier = Modifier.weight(1f),
                        )
                        TabModeChip(
                            text = "Incognito",
                            selected = incognito,
                            onClick = { incognito = true },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                CircularIconButton(
                    icon = Icons.Default.MoreVert,
                    contentDescription = "Tab options",
                    onClick = { isOverflowMenuOpen = true },
                )
                androidx.compose.material3.DropdownMenu(
                    expanded = isOverflowMenuOpen,
                    onDismissRequest = { isOverflowMenuOpen = false },
                ) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(if (selectionMode) "Cancel select" else "Select tabs") },
                        onClick = {
                            selectionMode = !selectionMode
                            isOverflowMenuOpen = false
                        },
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(if (incognito) "Close all incognito tabs" else "Close all tabs") },
                        enabled = visibleTabs.isNotEmpty(),
                        onClick = {
                            isOverflowMenuOpen = false
                            showCloseAllConfirm = true
                        },
                    )
                }
            }

            if (selectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { selectionMode = false }) {
                        Text("Cancel")
                    }
                }
            }

            if (visibleTabs.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(com.wormhole.browser.ui.theme.WormHoleSurface.Fill),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (incognito) Icons.Default.Shield else Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Text(
                        if (incognito) "No Incognito tabs" else "No open tabs",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                    Text(
                        if (incognito) "Open a private tab with the + button below."
                        else "Open a new tab with the + button below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            } else {
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(visibleTabs, key = { it.id }) { tab ->
                        TabGridCard(
                            tab = tab,
                            active = tab.id == activeTabId,
                            onClick = { onTabSelected(tab.id) },
                            onClose = { onTabClosed(tab.id) },

                            modifier = Modifier.animateItem(
                                placementSpec = WormHoleMotion.bouncy(),
                            ),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = { selectionMode = !selectionMode }) {
                    Text("Select")
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .size(56.dp)
                        .bouncyClickable(
                            onClick = if (incognito) onNewIncognitoTab else onNewTab,
                        ),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = if (incognito) "New incognito tab" else "New tab",
                            tint = MaterialTheme.colorScheme.background,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }

                TextButton(onClick = onClose) {
                    Text("Done")
                }
            }
        }
    }

    if (showCloseAllConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCloseAllConfirm = false },
            title = { Text(if (incognito) "Close all incognito tabs?" else "Close all tabs?") },
            text = {
                Text(
                    if (incognito) {
                        "This closes every incognito tab. This can't be undone."
                    } else {
                        "This closes every open tab in this space. This can't be undone."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showCloseAllConfirm = false
                    onCloseAllTabs(incognito)
                }) { Text("Close all") }
            },
            dismissButton = {
                TextButton(onClick = { showCloseAllConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun TabModeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(23.dp),
        color = if (selected) Color.White else Color.Transparent,
        modifier = modifier
            .height(46.dp)
            .bouncyClickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = if (selected) MaterialTheme.colorScheme.background
                else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun TabGridCard(
    tab: com.wormhole.browser.core.browser.Tab,
    active: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {

    val entranceScale = remember { androidx.compose.animation.core.Animatable(0.72f) }
    LaunchedEffect(Unit) {
        entranceScale.animateTo(1f, animationSpec = WormHoleMotion.bouncy())
    }

    var isClosing by remember { mutableStateOf(false) }
    val closeScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isClosing) 0.75f else 1f,
        animationSpec = WormHoleMotion.settled(),
        label = "tabCardCloseScale",
    )
    val closeAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isClosing) 0f else 1f,
        animationSpec = WormHoleMotion.settled(),
        label = "tabCardCloseAlpha",
        finishedListener = { if (isClosing) onClose() },
    )

    val thumbnail = com.wormhole.browser.core.webview.TabThumbnailCache.get(tab.id)
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (active) 3.dp else 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = entranceScale.value * closeScale
                scaleY = entranceScale.value * closeScale
                alpha = closeAlpha
            }
            .bouncyClickable(onClick = onClick),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(178.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                if (thumbnail != null) {
                    androidx.compose.foundation.Image(
                        bitmap = thumbnail.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            tab.title.firstOrNull()?.uppercase() ?: "K",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(34.dp)
                        .bouncyClickable(onClick = { isClosing = true }),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close tab",
                            tint = MaterialTheme.colorScheme.background,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val favicon = com.wormhole.browser.core.webview.FaviconCache.get(tab.url)
                    if (favicon != null) {
                        androidx.compose.foundation.Image(
                            bitmap = favicon.asImageBitmap(),
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(end = 6.dp),
                        )
                    }
                    Text(
                        tab.title.ifBlank { "New tab" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    tab.displayUrl.ifBlank { "New tab" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun CircularIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    spin: Boolean = false,
) {
    CircularIconButtonShell(contentDescription, onClick, spin) { tint ->
        Icon(icon, contentDescription = contentDescription, tint = tint)
    }
}

@Composable
private fun CircularIconButton(
    painter: androidx.compose.ui.graphics.painter.Painter,
    contentDescription: String,
    onClick: () -> Unit,
    spin: Boolean = false,
) {
    CircularIconButtonShell(contentDescription, onClick, spin) { tint ->
        Icon(painter, contentDescription = contentDescription, tint = tint)
    }
}

@Composable
private fun CircularIconButtonShell(
    contentDescription: String,
    onClick: () -> Unit,
    spin: Boolean,
    content: @Composable (tint: Color) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) WormHoleMotion.PRESS_SCALE else 1f,
        animationSpec = WormHoleMotion.snappy(),
        label = "circularIconButtonScale",
    )
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .size(46.dp)
            .scale(scale)
            .rotate(rotation.value)
            .clip(CircleShape)
            .background(com.wormhole.browser.ui.theme.WormHoleSurface.Fill)
            .border(1.dp, com.wormhole.browser.ui.theme.WormHoleSurface.HairlineBorder, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (spin) {
                        scope.launch {
                            rotation.animateTo(90f, animationSpec = WormHoleMotion.bouncy())
                            rotation.animateTo(0f, animationSpec = WormHoleMotion.bouncy())
                        }
                    }
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        content(MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * Shown over the (otherwise blank) content area when GeckoView's onLoadError
 * fires -- e.g. no connection, DNS failure, TLS error, or blocked content.
 * Without this, a failed navigation leaves nothing but the app's background
 * color behind a normal-looking toolbar, with no indication anything went wrong.
 */
@Composable
private fun LoadErrorOverlay(
    message: String,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.wormhole.browser.ui.theme.WormHoleBarBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "This page didn't load",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = com.wormhole.browser.ui.theme.WormHoleSurface.Fill,
                border = com.wormhole.browser.ui.theme.WormHoleSurface.border(),
                modifier = Modifier.bouncyClickable(onClick = onRetry),
            ) {
                Text(
                    text = "Retry",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }
    }
}
