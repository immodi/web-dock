package com.immodi.webdock.webview

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.Profile
import androidx.webkit.WebViewFeature
import com.immodi.webdock.BuildConfig
import com.immodi.webdock.domain.AppSettings

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"

/**
 * Applies per-app settings. Call this after [ProfileManager.attachProfile],
 * because setting a profile must be the first call on a WebView.
 */
@SuppressLint("SetJavaScriptEnabled")
fun WebView.configureForApp(appSettings: AppSettings, profile: Profile?) {
    settings.apply {
        javaScriptEnabled = appSettings.javascriptEnabled
        domStorageEnabled = true
        allowFileAccess = false
        allowContentAccess = false
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        textZoom = appSettings.zoomPercent
        setSupportMultipleWindows(true)
        javaScriptCanOpenWindowsAutomatically = false
        if (appSettings.desktopMode) {
            userAgentString = DESKTOP_USER_AGENT
            useWideViewPort = true
            loadWithOverviewMode = true
        }
    }

    CookieManager.getInstance().setAcceptThirdPartyCookies(this, appSettings.thirdPartyCookies)
    CookieManager.getInstance().setAcceptCookie(true)
    if (profile != null && WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
        profile.cookieManager.setAcceptCookie(true)
    }

    if (BuildConfig.DEBUG) {
        WebView.setWebContentsDebuggingEnabled(true)
    }
}
