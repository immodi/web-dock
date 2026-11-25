package com.immodi.webdock.domain

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Normalizes user input into a canonical http(s) URL.
 *
 * Rules: trim, add https:// when no scheme is given, lowercase scheme and host,
 * resolve IDN hosts to punycode, keep path and query, treat a missing trailing
 * slash consistently, and reject anything that is not http(s).
 */
object UrlNormalizer {

    private val SCHEME_PREFIX = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")

    fun normalize(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        val candidate = if (SCHEME_PREFIX.containsMatchIn(trimmed)) trimmed else "https://$trimmed"
        val url = candidate.toHttpUrlOrNull() ?: return null
        if (url.scheme != "http" && url.scheme != "https") return null
        return url.toString()
    }

    fun hostOf(normalizedUrl: String): String? = normalizedUrl.toHttpUrlOrNull()?.host

    fun isDuplicate(first: String, second: String): Boolean {
        val a = normalize(first) ?: return false
        val b = normalize(second) ?: return false
        return a == b
    }
}
