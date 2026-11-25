package com.immodi.webdock.webview

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.immodi.webdock.App
import com.immodi.webdock.domain.WebApp
import com.immodi.webdock.ui.theme.WebDockTheme
import kotlinx.coroutines.launch

class WebAppActivity : ComponentActivity() {

    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val appId = intent.data?.lastPathSegment
            ?: intent.getStringExtra(EXTRA_APP_ID)
            ?: run {
                finish()
                return
            }

        val container = (application as App).container

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val view = webView
                    if (view != null && view.canGoBack()) {
                        view.goBack()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            },
        )

        lifecycleScope.launch {
            val app = container.webAppRepository.get(appId)
            if (app == null) {
                finish()
                return@launch
            }
            showApp(app)
        }
    }

    @Suppress("DEPRECATION") // Single-argument TaskDescription is the only form available below API 33.
    private fun showApp(app: WebApp) {
        setTaskDescription(ActivityManager.TaskDescription(app.name))
        setContent {
            WebDockTheme {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        createWebView(context, app.settings).also { view ->
                            webView = view
                            view.loadUrl(app.url)
                        }
                    },
                    onRelease = { view -> view.destroy() },
                )
            }
        }
    }

    override fun onPause() {
        webView?.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
    }

    override fun onDestroy() {
        webView?.let { view ->
            (view.parent as? android.view.ViewGroup)?.removeView(view)
            view.destroy()
        }
        webView = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_APP_ID = "app_id"

        fun createIntent(context: Context, appId: String): Intent =
            Intent(context, WebAppActivity::class.java).apply {
                putExtra(EXTRA_APP_ID, appId)
                data = "webdock://app/$appId".toUri()
            }
    }
}
