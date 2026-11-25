package com.immodi.webdock.data.meta

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.immodi.webdock.domain.UrlNormalizer
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import okio.BufferedSource

/**
 * Fetches and parses the metadata for a web app. Any failure returns null so the
 * caller can keep its fallbacks and never block saving.
 */
class MetadataFetcher(
    private val client: OkHttpClient = defaultClient(),
) {

    suspend fun fetch(url: String): Metadata? = withContext(Dispatchers.IO) {
        val normalized = UrlNormalizer.normalize(url) ?: return@withContext null
        runCatching { fetchBlocking(normalized) }.getOrNull()
    }

    private fun fetchBlocking(normalized: String): Metadata? {
        val page = get(normalized, MAX_HTML_BYTES, ACCEPT_HTML) ?: return null
        val parsed = PageMetadataParser.parse(page.text, page.finalUrl)

        val manifest = parsed.manifestUrl
            ?.let { manifestUrl -> get(manifestUrl, MAX_MANIFEST_BYTES, ACCEPT_JSON) }
            ?.let { fetched -> ManifestParser.parse(fetched.text, fetched.finalUrl) }

        val candidates = buildList {
            manifest?.icons?.let(::addAll)
            addAll(parsed.icons)
        }
        val host = page.finalUrl.toHttpUrlOrNull()?.host ?: normalized.toHttpUrlOrNull()?.host

        return Metadata(
            title = parsed.title,
            name = manifest?.name
                ?: manifest?.shortName
                ?: parsed.applicationName
                ?: parsed.title
                ?: host,
            iconUrl = IconSelector.select(candidates)?.url ?: faviconUrl(page.finalUrl),
            themeColor = CssColorParser.parse(manifest?.themeColor ?: parsed.themeColor),
            backgroundColor = CssColorParser.parse(manifest?.backgroundColor),
        )
    }

    private fun get(url: String, maxBytes: Long, accept: String): FetchedText? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", MOBILE_USER_AGENT)
            .header("Accept", accept)
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body ?: return null
            val bytes = readBounded(body.source(), maxBytes) ?: return null
            val charset = body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
            return FetchedText(
                text = bytes.toString(charset),
                finalUrl = response.request.url.toString(),
            )
        }
    }

    private fun readBounded(source: BufferedSource, maxBytes: Long): ByteArray? {
        val buffer = Buffer()
        val read = source.read(buffer, maxBytes + 1)
        if (read == -1L) return ByteArray(0)
        if (read > maxBytes) return null
        return buffer.readByteArray()
    }

    private fun faviconUrl(finalUrl: String): String? =
        finalUrl.toHttpUrlOrNull()
            ?.newBuilder()
            ?.encodedPath("/favicon.ico")
            ?.query(null)
            ?.fragment(null)
            ?.build()
            ?.toString()

    private data class FetchedText(val text: String, val finalUrl: String)

    companion object {
        const val MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"

        private const val ACCEPT_HTML =
            "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        private const val ACCEPT_JSON = "application/manifest+json,application/json,*/*;q=0.8"
        private const val MAX_HTML_BYTES = 2L * 1024 * 1024
        private const val MAX_MANIFEST_BYTES = 512L * 1024

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
            .callTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
}
