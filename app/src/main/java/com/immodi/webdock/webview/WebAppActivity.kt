package com.immodi.webdock.webview

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Message
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.GeolocationPermissions
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.webkit.Profile
import com.immodi.webdock.App
import com.immodi.webdock.AppContainer
import com.immodi.webdock.R
import com.immodi.webdock.domain.OrientationMode
import com.immodi.webdock.domain.WebApp
import com.immodi.webdock.ui.settings.AppSettingsActivity
import com.immodi.webdock.ui.theme.WebDockAppTheme
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class WebAppActivity : ComponentActivity() {

    private val container: AppContainer get() = (application as App).container

    private var appId: String = ""
    private var app: WebApp? by mutableStateOf(null)
    private var webView: WebView? = null
    private var profile: Profile? = null
    private var policy: NavigationPolicy? = null

    private var loadProgress by mutableStateOf(0)
    private var loading by mutableStateOf(false)
    private var errorPage: ErrorPage? by mutableStateOf(null)
    private var jsDialog: JsDialogRequest? by mutableStateOf(null)
    private val jsDialogQueue = ArrayDeque<JsDialogRequest>()
    private var permissionPrompt: PermissionPrompt? by mutableStateOf(null)
    private var locationPrompt: LocationPrompt? by mutableStateOf(null)
    private var showEdgeHandle by mutableStateOf(true)
    private var showSheet by mutableStateOf(false)
    private var clearDataPrompt by mutableStateOf(false)
    private var isFullscreen by mutableStateOf(false)
    private var savedWebViewState: Bundle? = null
    private var swipeRefresh: SwipeRefreshLayout? = null

    private var fileChooserLauncher: ActivityResultLauncher<Intent>? = null
    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var settingsLauncher: ActivityResultLauncher<Intent>? = null

    private var pendingFileChooser: PendingFileChooser? = null
    private var pendingCameraUri: Uri? = null
    private var permissionContinuation: ((Boolean) -> Unit)? = null
    private var cameraAskedThisSession = false

    private val sessionPermissionDecisions = mutableMapOf<String, Boolean>()
    private val sessionLocationDecisions = mutableMapOf<String, Boolean>()

    private val messages = Channel<String>(Channel.BUFFERED)

    private var fullscreenOverlay: FrameLayout? = null
    private var fullscreenView: View? = null
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        appId = intent.data?.lastPathSegment
            ?: intent.getStringExtra(EXTRA_APP_ID)
            ?: run {
                finish()
                return
            }
        savedWebViewState = savedInstanceState?.getBundle(STATE_WEB_VIEW)

        setContent {
            WebDockAppTheme(container) {
                val currentApp = app
                if (currentApp == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    WebAppContent(currentApp)
                }
            }
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    when {
                        fullscreenOverlay != null -> hideFullscreen()
                        jsDialog != null -> cancelJs()
                        webView?.canGoBack() == true -> webView?.goBack()
                        else -> {
                            isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                        }
                    }
                }
            },
        )

        lifecycleScope.launch {
            val loaded = container.webAppRepository.get(appId)
            if (loaded == null) {
                finish()
                return@launch
            }
            app = loaded
            policy = NavigationPolicy(loaded.url, loaded.settings)
            showEdgeHandle = container.settingsRepository.settings.first().showEdgeHandle
            applyWindowChrome(loaded, savedInstanceState == null)
            applySystemBars(loaded)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun WebAppContent(currentApp: WebApp) {
        val snackbarHostState = remember { SnackbarHostState() }
        val fileLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result -> onFileChooserResult(result.resultCode, result.data) }
        val permissions = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { result -> onPermissionResult(result) }
        val settings = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
        if (result.resultCode == Activity.RESULT_OK) recreate()
        }

        SideEffect {
            fileChooserLauncher = fileLauncher
            permissionLauncher = permissions
            settingsLauncher = settings
        }

        LaunchedEffect(Unit) {
            for (message in messages) snackbarHostState.showSnackbar(message)
        }

        val themeColor = currentApp.themeColor ?: currentApp.backgroundColor
        val barColor = themeColor?.let { Color(it) } ?: MaterialTheme.colorScheme.background

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(barColor),
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
                factory = { context -> createHostView(context, currentApp) },
                onRelease = { view ->
                    (view as? SwipeRefreshLayout)?.removeAllViews()
                    webView?.destroy()
                    webView = null
                    swipeRefresh = null
                },
            )

            if (loading) {
                LinearProgressIndicator(
                    progress = { loadProgress.coerceIn(0, 100) / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .systemBarsPadding(),
                )
            }

            errorPage?.let { ErrorOverlay(it) }

            if (showEdgeHandle && !isFullscreen) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(28.dp)
                        .height(72.dp)
                        .clickable { showSheet = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(56.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(3.dp),
                            ),
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
            )

            jsDialog?.let { dialog -> JsDialog(dialog) }
            permissionPrompt?.let { prompt -> PermissionDialog(prompt) }
            locationPrompt?.let { prompt -> LocationDialog(prompt) }

            if (showSheet) {
                WebAppSheet(currentApp, onDismiss = { showSheet = false })
            }

            if (clearDataPrompt) {
                ClearDataDialog(currentApp)
            }
        }
    }

    private fun createHostView(context: Context, currentApp: WebApp): View {
        val view = WebView(context)
        webView = view
        profile = container.profileManager.attachProfile(
            view,
            currentApp.id,
            currentApp.settings.isolateStorage,
        )
        view.configureForApp(currentApp.settings, profile)
        view.webViewClient = WebAppClient()
        view.webChromeClient = WebAppChromeClient()
        view.setDownloadListener(
            DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                if (url != null) handleDownload(url, userAgent, contentDisposition, mimeType)
            },
        )

        if (currentApp.settings.keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        requestedOrientation = when (currentApp.settings.orientation) {
            OrientationMode.AUTO -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            OrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            OrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        val state = savedWebViewState
        savedWebViewState = null
        if (state == null || view.restoreState(state) == null) {
            view.loadUrl(currentApp.url)
        }

        return if (currentApp.settings.pullToRefresh) {
            SwipeRefreshLayout(context).apply {
                swipeRefresh = this
                setOnRefreshListener { view.reload() }
                addView(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
        } else {
            view
        }
    }

    @Suppress("DEPRECATION")
    private fun applyWindowChrome(currentApp: WebApp, setTaskDescription: Boolean) {
        val color = currentApp.themeColor ?: currentApp.backgroundColor
        if (color != null) {
            window.statusBarColor = color
            window.navigationBarColor = color
            val light = ColorUtils.calculateLuminance(color) > 0.5
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = light
                isAppearanceLightNavigationBars = light
            }
        }
        if (setTaskDescription) updateTaskDescription(currentApp)
    }

    private fun updateTaskDescription(currentApp: WebApp) {
        val color = currentApp.themeColor
            ?: currentApp.backgroundColor
            ?: ContextCompat.getColor(this, R.color.ic_launcher_background)
        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) { appIconBitmap(currentApp) }
            @Suppress("DEPRECATION")
            setTaskDescription(ActivityManager.TaskDescription(currentApp.name, bitmap, color))
        }
    }

    private fun appIconBitmap(currentApp: WebApp): Bitmap? {
        currentApp.iconFile?.let { name ->
            BitmapFactory.decodeFile(File(filesDir, "icons/$name").absolutePath)?.let { return it }
        }
        val seed = com.immodi.webdock.domain.UrlNormalizer.hostOf(currentApp.url) ?: currentApp.url
        val size = 192
        val bitmap = createBitmap(size, size)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(com.immodi.webdock.ui.components.avatarColor(seed).toArgb())
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = android.graphics.Color.WHITE
            textSize = size * 0.5f
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val letter = currentApp.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        val baseline = size / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(letter, size / 2f, baseline, paint)
        return bitmap
    }

    private fun applySystemBars(currentApp: WebApp) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (currentApp.settings.immersive || fullscreenOverlay != null) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
            if (!currentApp.settings.keepScreenOn) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    private inner class WebAppClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest,
        ): Boolean {
            val url = request.url.toString()
            if (!request.isForMainFrame) {
                return !isHttpOrHttps(url)
            }
            return handleNavigation(url)
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            if (url != null && url != "about:blank") {
                loading = true
                loadProgress = 0
                errorPage = null
            }
        }

        override fun onPageFinished(view: WebView, url: String?) {
            loading = false
            swipeRefresh?.isRefreshing = false
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError,
        ) {
            if (!request.isForMainFrame) return
            if (error.errorCode == WebViewClient.ERROR_UNKNOWN ||
                error.errorCode == ERROR_CANCELED_CODE
            ) {
                return
            }
            loading = false
            swipeRefresh?.isRefreshing = false
            errorPage = ErrorPage(request.url.toString(), error.description.toString())
        }

        override fun onReceivedHttpError(
            view: WebView,
            request: WebResourceRequest,
            errorResponse: WebResourceResponse,
        ) {
            if (request.isForMainFrame && errorResponse.statusCode >= 400) {
                messages.trySend(getString(R.string.http_error, errorResponse.statusCode))
            }
        }

        override fun onRenderProcessGone(
            view: WebView,
            detail: android.webkit.RenderProcessGoneDetail,
        ): Boolean {
            webView = null
            view.destroy()
            errorPage = ErrorPage(view.url.orEmpty(), getString(R.string.renderer_gone))
            return true
        }

        override fun onReceivedSslError(
            view: WebView,
            handler: SslErrorHandler,
            error: android.net.http.SslError,
        ) {
            handler.cancel()
            loading = false
            swipeRefresh?.isRefreshing = false
            errorPage = ErrorPage(error.url.orEmpty(), getString(R.string.ssl_error_message))
        }
    }

    private inner class WebAppChromeClient : WebChromeClient() {

        override fun onProgressChanged(view: WebView, newProgress: Int) {
            loadProgress = newProgress
            loading = newProgress < 100
        }

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?,
        ): Boolean {
            if (filePathCallback == null || fileChooserParams == null) return false
            pendingFileChooser?.callback?.onReceiveValue(null)
            pendingFileChooser = PendingFileChooser(filePathCallback, fileChooserParams)

            val cameraGranted =
                ContextCompat.checkSelfPermission(this@WebAppActivity, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
            if (cameraGranted || cameraAskedThisSession) {
                launchFileChooser(cameraGranted)
            } else {
                cameraAskedThisSession = true
                requestAndroidPermissions(arrayOf(Manifest.permission.CAMERA)) { granted ->
                    launchFileChooser(granted)
                }
            }
            return true
        }

        override fun onPermissionRequest(request: PermissionRequest) {
            val wanted = request.resources.filter {
                it == PermissionRequest.RESOURCE_VIDEO_CAPTURE ||
                    it == PermissionRequest.RESOURCE_AUDIO_CAPTURE
            }
            if (wanted.isEmpty()) {
                request.deny()
                return
            }

            val origin = request.origin.toString()
            when (sessionPermissionDecisions[origin]) {
                true -> {
                    request.grant(wanted.toTypedArray())
                    return
                }

                false -> {
                    request.deny()
                    return
                }

                null -> Unit
            }

            val androidPermissions = buildList {
                if (PermissionRequest.RESOURCE_VIDEO_CAPTURE in request.resources) {
                    add(Manifest.permission.CAMERA)
                }
                if (PermissionRequest.RESOURCE_AUDIO_CAPTURE in request.resources) {
                    add(Manifest.permission.RECORD_AUDIO)
                }
            }.toTypedArray()

            requestAndroidPermissions(androidPermissions) { granted ->
                if (!granted) {
                    sessionPermissionDecisions[origin] = false
                    request.deny()
                } else {
                    permissionPrompt = PermissionPrompt(origin, wanted.toTypedArray(), request)
                }
            }
        }

        override fun onGeolocationPermissionsShowPrompt(
            origin: String,
            callback: GeolocationPermissions.Callback,
        ) {
            when (sessionLocationDecisions[origin]) {
                true -> {
                    callback.invoke(origin, true, false)
                    return
                }

                false -> {
                    callback.invoke(origin, false, false)
                    return
                }

                null -> Unit
            }

            requestAndroidPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            ) { granted ->
                if (granted) {
                    locationPrompt = LocationPrompt(origin, callback)
                } else {
                    sessionLocationDecisions[origin] = false
                    callback.invoke(origin, false, false)
                }
            }
        }

        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
            showFullscreen(view, callback)
        }

        override fun onHideCustomView() {
            hideFullscreen()
        }

        override fun onJsAlert(
            view: WebView,
            url: String,
            message: String,
            result: JsResult,
        ): Boolean {
            showJs(JsDialogRequest(JsDialogType.ALERT, message, null, result))
            return true
        }

        override fun onJsConfirm(
            view: WebView,
            url: String,
            message: String,
            result: JsResult,
        ): Boolean {
            showJs(JsDialogRequest(JsDialogType.CONFIRM, message, null, result))
            return true
        }

        override fun onJsPrompt(
            view: WebView,
            url: String,
            message: String,
            defaultValue: String?,
            result: JsPromptResult,
        ): Boolean {
            showJs(JsDialogRequest(JsDialogType.PROMPT, message, defaultValue, result))
            return true
        }

        @SuppressLint("SetJavaScriptEnabled")
        override fun onCreateWindow(
            view: WebView,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message,
        ): Boolean {
            val popup = WebView(this@WebAppActivity)
            container.profileManager.attachProfile(
                popup,
                appId,
                app?.settings?.isolateStorage ?: false,
            )
            popup.settings.javaScriptEnabled = true
            popup.settings.domStorageEnabled = true
            popup.webViewClient = object : WebViewClient() {
                private var routed = false

                override fun shouldOverrideUrlLoading(
                    popupView: WebView,
                    request: WebResourceRequest,
                ): Boolean {
                    if (routed) return true
                    routed = true
                    val url = request.url.toString()
                    popupView.stopLoading()
                    popupView.destroy()
                    handlePopupUrl(url)
                    return true
                }

                override fun onRenderProcessGone(
                    popupView: WebView,
                    detail: android.webkit.RenderProcessGoneDetail,
                ): Boolean {
                    popupView.destroy()
                    return true
                }

                override fun onPageStarted(popupView: WebView, url: String?, favicon: Bitmap?) {
                    if (!routed && !url.isNullOrBlank() && url != "about:blank") {
                        routed = true
                        popupView.stopLoading()
                        popupView.destroy()
                        handlePopupUrl(url)
                    }
                }
            }
            (resultMsg.obj as? WebView.WebViewTransport)?.webView = popup
            resultMsg.sendToTarget()
            return true
        }

        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
            if (com.immodi.webdock.BuildConfig.DEBUG) {
                android.util.Log.d(
                    "WebDockWebView",
                    "${consoleMessage.message()} (${consoleMessage.sourceId()}:${consoleMessage.lineNumber()})",
                )
            }
            return true
        }
    }

    private fun isHttpOrHttps(url: String): Boolean {
        val scheme = url.substringBefore(':', "").lowercase()
        return scheme == "http" || scheme == "https"
    }

    private fun handleNavigation(url: String): Boolean {
        if (url.startsWith("webdock://")) return true
        return when (val decision = policy?.decide(url) ?: NavigationDecision.Blocked) {
            NavigationDecision.InApp -> false
            is NavigationDecision.External -> {
                openExternal(decision.url)
                true
            }

            is NavigationDecision.ExternalApp -> {
                openExternalApp(decision.url)
                true
            }

            NavigationDecision.Blocked -> true
        }
    }

    private fun handlePopupUrl(url: String) {
        if (policy?.isInApp(url) == true) {
            webView?.loadUrl(url)
        } else {
            openExternal(url)
        }
    }

    private fun openExternal(url: String) {
        val uri = url.toUri()
        try {
            androidx.browser.customtabs.CustomTabsIntent.Builder().build().launchUrl(this, uri)
        } catch (_: Exception) {
            openExternalApp(url)
        }
    }

    private fun openExternalApp(url: String) {
        try {
            val intent = if (url.startsWith("intent:", ignoreCase = true)) {
                Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            } else {
                Intent(Intent.ACTION_VIEW, url.toUri())
            }
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, R.string.no_app_handles_link, Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareUrl(url: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_url)))
    }

    private fun requestAndroidPermissions(
        permissions: Array<String>,
        onResult: (Boolean) -> Unit,
    ) {
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            onResult(true)
            return
        }
        if (permissionContinuation != null) {
            onResult(false)
            return
        }
        val launcher = permissionLauncher
        if (launcher == null) {
            onResult(false)
            return
        }
        permissionContinuation = onResult
        launcher.launch(missing.toTypedArray())
    }

    private fun onPermissionResult(result: Map<String, Boolean>) {
        val continuation = permissionContinuation
        permissionContinuation = null
        continuation?.invoke(result.values.all { it })
    }

    private fun launchFileChooser(cameraAllowed: Boolean) {
        val pending = pendingFileChooser ?: return
        val params = pending.params
        val allowMultiple = params.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE
        val acceptTypes = params.acceptTypes.filter { it.isNotBlank() }
        val content = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = acceptTypes.firstOrNull() ?: "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple)
            if (acceptTypes.size > 1) putExtra(Intent.EXTRA_MIME_TYPES, params.acceptTypes)
        }
        val chooser = Intent.createChooser(content, getString(R.string.choose_file))
        if (!allowMultiple && cameraAllowed) {
            val directory = File(cacheDir, "captures").apply { mkdirs() }
            val photo = File(directory, "capture-${System.currentTimeMillis()}.jpg")
            val uri = runCatching {
                FileProvider.getUriForFile(this, "$packageName.fileprovider", photo)
            }.getOrNull()
            if (uri != null) {
                pendingCameraUri = uri
                val camera = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, uri)
                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(camera))
            }
        }
        val launcher = fileChooserLauncher
        if (launcher == null) {
            pendingFileChooser = null
            pending.callback.onReceiveValue(null)
        } else {
            launcher.launch(chooser)
        }
    }

    private fun onFileChooserResult(resultCode: Int, data: Intent?) {
        val pending = pendingFileChooser ?: return
        pendingFileChooser = null
        val cameraUri = pendingCameraUri
        pendingCameraUri = null

        if (resultCode != Activity.RESULT_OK) {
            pending.callback.onReceiveValue(null)
            return
        }

        val uris = mutableListOf<Uri>()
        data?.clipData?.let { clip ->
            for (index in 0 until clip.itemCount) {
                clip.getItemAt(index)?.uri?.let(uris::add)
            }
        }
        data?.data?.let(uris::add)
        if (uris.isEmpty() && cameraUri != null) uris.add(cameraUri)
        pending.callback.onReceiveValue(uris.toTypedArray().takeIf { it.isNotEmpty() })
    }

    private fun showFullscreen(view: View, callback: WebChromeClient.CustomViewCallback) {
        if (fullscreenOverlay != null) {
            callback.onCustomViewHidden()
            return
        }
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            addView(view, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        (window.decorView as? ViewGroup)?.addView(
            overlay,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        fullscreenOverlay = overlay
        fullscreenView = view
        fullscreenCallback = callback
        isFullscreen = true
        hideSystemBars()
    }

    private fun hideFullscreen() {
        val overlay = fullscreenOverlay ?: return
        (overlay.parent as? ViewGroup)?.removeView(overlay)
        fullscreenOverlay = null
        fullscreenView = null
        isFullscreen = false
        fullscreenCallback?.onCustomViewHidden()
        fullscreenCallback = null
        app?.let { applySystemBars(it) }
    }

    private fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun showJs(request: JsDialogRequest) {
        if (jsDialog == null) jsDialog = request else jsDialogQueue.add(request)
    }

    private fun confirmJs(value: String?) {
        val current = jsDialog ?: return
        if (current.type == JsDialogType.PROMPT && current.result is JsPromptResult) {
            (current.result as JsPromptResult).confirm(value.orEmpty())
        } else {
            current.result.confirm()
        }
        jsDialog = jsDialogQueue.removeFirstOrNull()
    }

    private fun cancelJs() {
        jsDialog?.result?.cancel()
        jsDialog = jsDialogQueue.removeFirstOrNull()
    }

    @Composable
    private fun JsDialog(dialog: JsDialogRequest) {
        var input by remember(dialog) { mutableStateOf(dialog.defaultValue.orEmpty()) }
        AlertDialog(
            onDismissRequest = { cancelJs() },
            title = { Text(stringResource(R.string.js_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(dialog.message)
                    if (dialog.type == JsDialogType.PROMPT) {
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            singleLine = true,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { confirmJs(input) }) {
                    Text(stringResource(R.string.js_ok))
                }
            },
            dismissButton = if (dialog.type == JsDialogType.ALERT) {
                null
            } else {
                {
                    TextButton(onClick = { cancelJs() }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            },
        )
    }

    @Composable
    private fun PermissionDialog(prompt: PermissionPrompt) {
        AlertDialog(
            onDismissRequest = { denyPermission() },
            title = { Text(stringResource(R.string.permission_title)) },
            text = { Text(stringResource(R.string.permission_message, prompt.origin)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        sessionPermissionDecisions[prompt.origin] = true
                        prompt.request.grant(prompt.resources)
                        permissionPrompt = null
                    },
                ) {
                    Text(stringResource(R.string.allow))
                }
            },
            dismissButton = {
                TextButton(onClick = { denyPermission() }) {
                    Text(stringResource(R.string.deny))
                }
            },
        )
    }

    private fun denyPermission() {
        val prompt = permissionPrompt ?: return
        sessionPermissionDecisions[prompt.origin] = false
        prompt.request.deny()
        permissionPrompt = null
    }

    @Composable
    private fun LocationDialog(prompt: LocationPrompt) {
        AlertDialog(
            onDismissRequest = { denyLocation() },
            title = { Text(stringResource(R.string.location_title)) },
            text = { Text(stringResource(R.string.location_message, prompt.origin)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        sessionLocationDecisions[prompt.origin] = true
                        prompt.callback.invoke(prompt.origin, true, false)
                        locationPrompt = null
                    },
                ) {
                    Text(stringResource(R.string.allow))
                }
            },
            dismissButton = {
                TextButton(onClick = { denyLocation() }) {
                    Text(stringResource(R.string.deny))
                }
            },
        )
    }

    private fun denyLocation() {
        val prompt = locationPrompt ?: return
        sessionLocationDecisions[prompt.origin] = false
        prompt.callback.invoke(prompt.origin, false, false)
        locationPrompt = null
    }

    @Composable
    private fun ErrorOverlay(error: ErrorPage) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            ) {
                Text(
                    text = stringResource(R.string.error_page_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = error.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = {
                        errorPage = null
                        if (webView != null) webView?.reload() else recreate()
                    },
                ) {
                    Text(stringResource(R.string.retry))
                }
                TextButton(onClick = { openExternal(error.url) }) {
                    Text(stringResource(R.string.open_in_browser))
                }
                TextButton(onClick = { finish() }) {
                    Text(stringResource(R.string.back_to_manager))
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun WebAppSheet(currentApp: WebApp, onDismiss: () -> Unit) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(modifier = Modifier.navigationBarsPadding()) {
                SheetRow(Icons.Default.Refresh, stringResource(R.string.reload)) {
                    onDismiss()
                    webView?.reload()
                }
                SheetRow(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back_to_manager)) {
                    onDismiss()
                    finish()
                }
                SheetRow(null, stringResource(R.string.open_in_browser)) {
                    onDismiss()
                    openExternal(currentApp.url)
                }
                SheetRow(Icons.Default.Share, stringResource(R.string.share_url)) {
                    onDismiss()
                    shareUrl(currentApp.url)
                }
                SheetRow(Icons.Default.Settings, stringResource(R.string.app_settings)) {
                    onDismiss()
                    settingsLauncher?.launch(
                        AppSettingsActivity.createIntent(
                            this@WebAppActivity,
                            currentApp.id,
                            currentApp.settings,
                        ),
                    )
                }
                SheetRow(
                    Icons.Default.Delete,
                    stringResource(R.string.clear_data),
                    tint = MaterialTheme.colorScheme.error,
                ) {
                    onDismiss()
                    clearDataPrompt = true
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    @Composable
    private fun SheetRow(
        icon: androidx.compose.ui.graphics.vector.ImageVector?,
        label: String,
        tint: Color = MaterialTheme.colorScheme.onSurface,
        onClick: () -> Unit,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = tint)
            } else {
                Spacer(Modifier.width(24.dp))
            }
            Text(label, color = tint, style = MaterialTheme.typography.bodyLarge)
        }
    }

    @Composable
    private fun ClearDataDialog(currentApp: WebApp) {
        val shared = !currentApp.settings.isolateStorage ||
            !container.profileManager.isMultiProfileSupported()
        AlertDialog(
            onDismissRequest = { clearDataPrompt = false },
            title = { Text(stringResource(R.string.clear_data_title)) },
            text = {
                Text(
                    stringResource(
                        if (shared) R.string.clear_data_message_shared else R.string.clear_data_message,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clearDataPrompt = false
                        clearActiveData(currentApp)
                        messages.trySend(getString(R.string.data_cleared))
                    },
                ) {
                    Text(
                        stringResource(R.string.clear_data),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { clearDataPrompt = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    private fun clearActiveData(currentApp: WebApp) {
        val cookies = container.profileManager.cookieManager(profile)
        cookies.removeAllCookies(null)
        cookies.flush()
        if (profile != null && androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.MULTI_PROFILE)) {
            profile?.webStorage?.deleteAllData()
        } else {
            android.webkit.WebStorage.getInstance().deleteAllData()
        }
        webView?.clearCache(true)
        webView?.clearFormData()
        webView?.clearHistory()
        webView?.reload()
        container.profileManager.clearData(currentApp.id, currentApp.settings.isolateStorage)
    }

    private fun handleDownload(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
    ) {
        when {
            url.startsWith("data:", ignoreCase = true) -> saveDataUrl(url, mimeType)
            url.startsWith("blob:", ignoreCase = true) -> saveBlobUrl(url)
            url.startsWith("http", ignoreCase = true) ->
                enqueueDownloadManager(url, userAgent, contentDisposition, mimeType)
            else -> Unit
        }
    }

    private fun enqueueDownloadManager(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
    ) {
        val start = {
            runCatching {
                val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                val cookieManager = container.profileManager.cookieManager(profile)
                val request = DownloadManager.Request(url.toUri()).apply {
                    if (!mimeType.isNullOrBlank()) setMimeType(mimeType)
                    cookieManager.getCookie(url)?.let { addRequestHeader("Cookie", it) }
                    userAgent?.let { addRequestHeader("User-Agent", it) }
                    setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED,
                    )
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                }
                getSystemService(DownloadManager::class.java).enqueue(request)
                messages.trySend(getString(R.string.download_started))
            }.onFailure {
                messages.trySend(getString(R.string.download_failed))
            }
            Unit
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestAndroidPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            ) { granted ->
                if (granted) start() else messages.trySend(getString(R.string.download_failed))
            }
        } else {
            start()
        }
    }

    private fun saveBlobUrl(url: String) {
        val bridge = BlobSaveBridge()
        webView?.addJavascriptInterface(bridge, BLOB_BRIDGE)
        val script = """
            (function() {
              fetch(${JSONObject.quote(url)}).then(function(r) { return r.blob(); }).then(function(b) {
                var reader = new FileReader();
                reader.onloadend = function() {
                  if (window.$BLOB_BRIDGE) {
                    window.$BLOB_BRIDGE.save(reader.result, b.type || '');
                  }
                };
                reader.readAsDataURL(b);
              }).catch(function() {});
            })();
        """.trimIndent()
        webView?.evaluateJavascript(script, null)
    }

    private inner class BlobSaveBridge {
        @android.webkit.JavascriptInterface
        fun save(dataUrl: String?, mimeType: String?) {
            runOnUiThread {
                webView?.removeJavascriptInterface(BLOB_BRIDGE)
                if (!dataUrl.isNullOrBlank()) saveDataUrl(dataUrl, mimeType)
            }
        }
    }

    private fun saveDataUrl(dataUrl: String, fallbackMime: String?) {
        val comma = dataUrl.indexOf(',')
        if (comma < 0) return
        val metadata = dataUrl.substring(5, comma)
        val payload = dataUrl.substring(comma + 1)
        val isBase64 = metadata.contains(";base64", ignoreCase = true)
        val mime = metadata.substringBefore(';').ifBlank {
            fallbackMime?.takeIf { it.isNotBlank() } ?: "application/octet-stream"
        }
        val bytes = runCatching {
            if (isBase64) Base64.decode(payload, Base64.DEFAULT)
            else Uri.decode(payload).toByteArray()
        }.getOrNull() ?: return
        val name = "webdock-${System.currentTimeMillis()}.${extensionFor(mime)}"

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestAndroidPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            ) { granted ->
                if (granted) writeBytes(bytes, mime, name)
                else messages.trySend(getString(R.string.download_failed))
            }
        } else {
            writeBytes(bytes, mime, name)
        }
    }

    private fun writeBytes(bytes: ByteArray, mime: String, fileName: String) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mime)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    values,
                ) ?: error("no collection")
                contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("no stream")
            } else {
                val directory = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS,
                )
                val file = uniqueFile(directory, fileName)
                file.writeBytes(bytes)
                MediaScannerConnection.scanFile(this, arrayOf(file.absolutePath), arrayOf(mime), null)
            }
            messages.trySend(getString(R.string.download_saved))
        }.onFailure {
            messages.trySend(getString(R.string.download_failed))
        }
    }

    private fun uniqueFile(directory: File, fileName: String): File {
        directory.mkdirs()
        var file = File(directory, fileName)
        if (!file.exists()) return file
        val base = fileName.substringBeforeLast('.')
        val extension = fileName.substringAfterLast('.', "")
        var index = 1
        while (file.exists()) {
            file = File(directory, if (extension.isEmpty()) "$base-$index" else "$base-$index.$extension")
            index++
        }
        return file
    }

    private fun extensionFor(mime: String): String = when (mime.substringAfter('/', "").lowercase()) {
        "png" -> "png"
        "jpeg", "jpg" -> "jpg"
        "gif" -> "gif"
        "webp" -> "webp"
        "svg+xml" -> "svg"
        "pdf" -> "pdf"
        "zip" -> "zip"
        "plain" -> "txt"
        "json" -> "json"
        "html" -> "html"
        "mp4" -> "mp4"
        "mpeg" -> "mp3"
        else -> "bin"
    }

    override fun onPause() {
        container.profileManager.cookieManager(profile).flush()
        webView?.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val state = Bundle()
        webView?.saveState(state)
        outState.putBundle(STATE_WEB_VIEW, state)
    }

    override fun onDestroy() {
        jsDialog?.result?.cancel()
        jsDialog = null
        pendingFileChooser?.callback?.onReceiveValue(null)
        pendingFileChooser = null
        if (fullscreenOverlay != null) hideFullscreen()
        webView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
            view.stopLoading()
            view.destroy()
        }
        webView = null
        super.onDestroy()
    }

    private data class ErrorPage(val url: String, val description: String)

    private enum class JsDialogType { ALERT, CONFIRM, PROMPT }

    private data class JsDialogRequest(
        val type: JsDialogType,
        val message: String,
        val defaultValue: String?,
        val result: JsResult,
    )

    private data class PermissionPrompt(
        val origin: String,
        val resources: Array<String>,
        val request: PermissionRequest,
    )

    private data class LocationPrompt(
        val origin: String,
        val callback: GeolocationPermissions.Callback,
    )

    private data class PendingFileChooser(
        val callback: ValueCallback<Array<Uri>>,
        val params: WebChromeClient.FileChooserParams,
    )

    companion object {
        private const val EXTRA_APP_ID = "app_id"
        private const val STATE_WEB_VIEW = "web_view_state"
        private const val BLOB_BRIDGE = "WebDockBlobSaver"

        // WebView reports canceled loads with -3 but exposes no public constant.
        private const val ERROR_CANCELED_CODE = -3

        fun createIntent(context: Context, appId: String): Intent =
            Intent(context, WebAppActivity::class.java).apply {
                putExtra(EXTRA_APP_ID, appId)
                data = "webdock://app/$appId".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }
    }
}
