package com.immodi.webdock.data.meta

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

data class PageMetadata(
    val title: String?,
    val applicationName: String?,
    val themeColor: String?,
    val manifestUrl: String?,
    val icons: List<IconCandidate>,
)

object PageMetadataParser {

    private val WHITESPACE = Regex("\\s+")

    fun parse(html: String, baseUrl: String): PageMetadata {
        val document = Jsoup.parse(html, baseUrl)
        val base = baseUrl.toHttpUrlOrNull()
        val icons = mutableListOf<IconCandidate>()
        var manifestUrl: String? = null

        document.select("link[href]").forEach { link ->
            val relations = link.attr("rel").split(WHITESPACE).filter { it.isNotBlank() }
            val href = resolve(base, link.attr("href")) ?: return@forEach

            if (relations.any { it.equals("manifest", ignoreCase = true) }) {
                if (manifestUrl == null) manifestUrl = href
                return@forEach
            }

            val source = when {
                relations.any { it.equals("apple-touch-icon", true) || it.equals("apple-touch-icon-precomposed", true) } ->
                    IconSource.APPLE_TOUCH

                relations.any { it.equals("icon", ignoreCase = true) } -> IconSource.ICON
                else -> return@forEach
            }

            icons += IconCandidate(
                url = href,
                width = IconSelector.parseWidth(link.attr("sizes")),
                type = link.attr("type").takeIf { it.isNotBlank() },
                source = source,
            )
        }

        return PageMetadata(
            title = document.title().trim().takeIf { it.isNotEmpty() },
            applicationName = metaContent(document, "application-name"),
            themeColor = metaContent(document, "theme-color"),
            manifestUrl = manifestUrl,
            icons = icons.distinctBy { it.url },
        )
    }

    private fun metaContent(document: Document, name: String): String? =
        document.select("meta[name]")
            .firstOrNull { it.attr("name").equals(name, ignoreCase = true) }
            ?.attr("content")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    private fun resolve(base: okhttp3.HttpUrl?, href: String): String? {
        if (href.isBlank()) return null
        val resolved = base?.resolve(href.trim()) ?: href.trim().toHttpUrlOrNull() ?: return null
        return if (resolved.scheme == "http" || resolved.scheme == "https") resolved.toString() else null
    }
}
