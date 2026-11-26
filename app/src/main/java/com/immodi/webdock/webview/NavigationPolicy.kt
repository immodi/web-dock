package com.immodi.webdock.webview

import com.immodi.webdock.domain.AppSettings
import com.immodi.webdock.domain.LinkPolicy
import com.immodi.webdock.domain.UrlNormalizer
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

sealed interface NavigationDecision {
    data object InApp : NavigationDecision
    data class External(val url: String) : NavigationDecision
    data class ExternalApp(val url: String) : NavigationDecision
    data object Blocked : NavigationDecision
}

/**
 * Pure navigation policy. Decides whether a URL stays in the WebView, opens in a
 * Custom Tab, or is handed to another app.
 */
class NavigationPolicy(
    private val startUrl: String,
    private val settings: AppSettings,
) {

    fun decide(url: String): NavigationDecision {
        val scheme = url.substringBefore(':', missingDelimiterValue = "").lowercase()
        if (scheme != "http" && scheme != "https") {
            return when (scheme) {
                "mailto", "tel", "geo", "sms", "intent", "market" -> NavigationDecision.ExternalApp(url)
                else -> NavigationDecision.Blocked
            }
        }

        val host = url.toHttpUrlOrNull()?.host ?: return NavigationDecision.Blocked
        return when (settings.linkPolicy) {
            LinkPolicy.ALL_IN_APP -> NavigationDecision.InApp
            LinkPolicy.ALL_EXTERNAL -> NavigationDecision.External(url)
            LinkPolicy.SAME_HOST_IN_APP -> {
                val startHost = UrlNormalizer.hostOf(startUrl)
                val allowed = host.equals(startHost, ignoreCase = true) ||
                    settings.allowedHosts.any { it.trim().equals(host, ignoreCase = true) }
                if (allowed) NavigationDecision.InApp else NavigationDecision.External(url)
            }
        }
    }

    fun isInApp(url: String): Boolean = decide(url) is NavigationDecision.InApp
}
