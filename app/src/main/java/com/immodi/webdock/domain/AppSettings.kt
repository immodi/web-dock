package com.immodi.webdock.domain

enum class OrientationMode { AUTO, PORTRAIT, LANDSCAPE }

enum class LinkPolicy { SAME_HOST_IN_APP, ALL_IN_APP, ALL_EXTERNAL }

/**
 * Per-app behavior. Embedded in [WebApp]; defaults are the values for a new app.
 */
data class AppSettings(
    val isolateStorage: Boolean = true,
    val desktopMode: Boolean = false,
    val zoomPercent: Int = 100,
    val orientation: OrientationMode = OrientationMode.AUTO,
    val keepScreenOn: Boolean = false,
    val pullToRefresh: Boolean = true,
    val javascriptEnabled: Boolean = true,
    val thirdPartyCookies: Boolean = true,
    val linkPolicy: LinkPolicy = LinkPolicy.SAME_HOST_IN_APP,
    val allowedHosts: List<String> = emptyList(),
    val immersive: Boolean = false,
)
