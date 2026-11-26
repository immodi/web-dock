package com.immodi.webdock.webview

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.webkit.Profile
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

/**
 * Wraps the androidx.webkit multi-profile APIs. A profile gives an app its own
 * cookies, web storage, cache, and service workers.
 */
class ProfileManager(
    private val context: Context,
    private val pendingStore: PendingProfileStore,
) {

    fun isMultiProfileSupported(): Boolean =
        WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)

    fun profileName(appId: String): String =
        "app_${appId.filter { it.isLetterOrDigit() }.lowercase()}"

    /**
     * Associates the WebView with its profile. Must run before any other call on
     * the WebView.
     */
    fun attachProfile(webView: WebView, appId: String, isolate: Boolean): Profile? {
        if (!isolate || !isMultiProfileSupported()) return null
        return runCatching {
            val name = profileName(appId)
            WebViewCompat.setProfile(webView, name)
            ProfileStore.getInstance().getOrCreateProfile(name)
        }.getOrNull()
    }

    fun cookieManager(profile: Profile?): CookieManager =
        if (profile != null && WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
            profile.cookieManager
        } else {
            CookieManager.getInstance()
        }

    /**
     * Deletes the app's profile, or clears shared data when profiles are not
     * available. A profile that cannot be deleted now is retried on the next
     * manager launch.
     */
    fun clearData(appId: String, isolate: Boolean) {
        if (isolate && isMultiProfileSupported()) {
            val store = ProfileStore.getInstance()
            val name = profileName(appId)
            if (store.getProfile(name) == null) return
            val deleted = runCatching { store.deleteProfile(name) }.getOrDefault(false)
            if (!deleted) pendingStore.add(appId)
        } else {
            clearSharedData()
        }
    }

    fun deleteAppData(appId: String, isolate: Boolean) {
        if (!isolate || !isMultiProfileSupported()) return
        val store = ProfileStore.getInstance()
        val name = profileName(appId)
        if (store.getProfile(name) == null) return
        val deleted = runCatching { store.deleteProfile(name) }.getOrDefault(false)
        if (!deleted) pendingStore.add(appId)
    }

    fun retryPending() {
        if (!isMultiProfileSupported()) return
        val store = ProfileStore.getInstance()
        pendingStore.get().forEach { appId ->
            val name = profileName(appId)
            if (store.getProfile(name) == null) {
                pendingStore.remove(appId)
                return@forEach
            }
            val deleted = runCatching { store.deleteProfile(name) }.getOrDefault(false)
            if (deleted) pendingStore.remove(appId)
        }
    }

    fun clearSharedData() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
        WebStorage.getInstance().deleteAllData()
        runCatching {
            WebView(context).apply {
                clearCache(true)
                clearFormData()
                clearHistory()
                destroy()
            }
        }
    }
}
