package com.immodi.webdock.domain

import kotlinx.serialization.Serializable

@Serializable
enum class OrientationMode { AUTO, PORTRAIT, LANDSCAPE }

@Serializable
enum class LinkPolicy { SAME_HOST_IN_APP, ALL_IN_APP, ALL_EXTERNAL }

/**
 * Per-app behavior. Embedded in [WebApp]; defaults are the values for a new app.
 */
@Serializable
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
