package com.immodi.webdock.webview

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import com.immodi.webdock.BuildConfig
import com.immodi.webdock.domain.AppSettings

/**
 * Creates the WebView used for a single web app. Kept apart from the activity so
 * the configuration is easy to review and extend.
 */
@SuppressLint("SetJavaScriptEnabled")
fun createWebView(context: Context, appSettings: AppSettings): WebView {
    val webView = WebView(context)
    webView.settings.apply {
        javaScriptEnabled = appSettings.javascriptEnabled
        domStorageEnabled = true
        allowFileAccess = false
        allowContentAccess = false
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        textZoom = appSettings.zoomPercent
        if (appSettings.desktopMode) {
            userAgentString = userAgentString
                .replace("Mobile", "")
                .replace("Android", "")
                .replace(Regex("\\s+"), " ")
                .trim()
            useWideViewPort = true
            loadWithOverviewMode = true
        }
    }
    CookieManager.getInstance().setAcceptThirdPartyCookies(webView, appSettings.thirdPartyCookies)
    if (BuildConfig.DEBUG) {
        WebView.setWebContentsDebuggingEnabled(true)
    }
    return webView
}
