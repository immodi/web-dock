package com.immodi.webdock.data.meta

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class WebManifest(
    val name: String?,
    val shortName: String?,
    val themeColor: String?,
    val backgroundColor: String?,
    val icons: List<IconCandidate>,
    val startUrl: String?,
)

object ManifestParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(text: String, manifestUrl: String): WebManifest? {
        val manifest = runCatching { json.decodeFromString<ManifestJson>(text) }.getOrNull()
            ?: return null
        val base = manifestUrl.toHttpUrlOrNull()

        return WebManifest(
            name = manifest.name?.trim()?.takeIf { it.isNotEmpty() },
            shortName = manifest.shortName?.trim()?.takeIf { it.isNotEmpty() },
            themeColor = manifest.themeColor?.trim()?.takeIf { it.isNotEmpty() },
            backgroundColor = manifest.backgroundColor?.trim()?.takeIf { it.isNotEmpty() },
            icons = manifest.icons.mapNotNull { icon ->
                val resolved = base?.resolve(icon.src.trim()) ?: return@mapNotNull null
                if (resolved.scheme != "http" && resolved.scheme != "https") return@mapNotNull null
                IconCandidate(
                    url = resolved.toString(),
                    width = IconSelector.parseWidth(icon.sizes),
                    type = icon.type?.trim()?.takeIf { it.isNotEmpty() },
                    source = IconSource.MANIFEST,
                )
            }.distinctBy { it.url },
            startUrl = manifest.startUrl?.let { base?.resolve(it)?.toString() },
        )
    }

    @Serializable
    private data class ManifestJson(
        val name: String? = null,
        @SerialName("short_name") val shortName: String? = null,
        @SerialName("theme_color") val themeColor: String? = null,
        @SerialName("background_color") val backgroundColor: String? = null,
        val icons: List<ManifestIconJson> = emptyList(),
        @SerialName("start_url") val startUrl: String? = null,
    )

    @Serializable
    private data class ManifestIconJson(
        val src: String = "",
        val sizes: String? = null,
        val type: String? = null,
        val purpose: String? = null,
    )
}
