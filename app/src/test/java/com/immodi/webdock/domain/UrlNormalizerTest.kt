package com.immodi.webdock.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlNormalizerTest {

    @Test
    fun trimsWhitespaceAndAddsScheme() {
        assertEquals("https://example.com/", UrlNormalizer.normalize("  example.com  "))
    }

    @Test
    fun lowercasesSchemeAndHost() {
        assertEquals(
            "https://example.com/Path?q=1",
            UrlNormalizer.normalize("HTTPS://Example.COM/Path?q=1"),
        )
    }

    @Test
    fun keepsPathAndQuery() {
        assertEquals(
            "https://trade-journel.pages.dev/app?account=2",
            UrlNormalizer.normalize("https://trade-journel.pages.dev/app?account=2"),
        )
    }

    @Test
    fun treatsMissingTrailingSlashConsistently() {
        assertEquals(
            UrlNormalizer.normalize("https://a.dev"),
            UrlNormalizer.normalize("https://a.dev/"),
        )
    }

    @Test
    fun keepsHttp() {
        assertEquals("http://a.dev/", UrlNormalizer.normalize("http://a.dev"))
    }

    @Test
    fun rejectsEmptyInput() {
        assertNull(UrlNormalizer.normalize(""))
        assertNull(UrlNormalizer.normalize("   "))
    }

    @Test
    fun rejectsNonHttpSchemes() {
        assertNull(UrlNormalizer.normalize("ftp://example.com/file"))
        assertNull(UrlNormalizer.normalize("javascript:alert(1)"))
        assertNull(UrlNormalizer.normalize("file:///tmp/index.html"))
    }

    @Test
    fun rejectsEmptyHost() {
        assertNull(UrlNormalizer.normalize("https://"))
    }

    @Test
    fun convertsIdnHostsToPunycode() {
        assertEquals("https://xn--mnchen-3ya.de/", UrlNormalizer.normalize("https://münchen.de"))
    }

    @Test
    fun reportsDuplicatesAfterNormalization() {
        assertTrue(UrlNormalizer.isDuplicate("a.dev", "https://A.dev/"))
        assertTrue(UrlNormalizer.isDuplicate("https://a.dev/path", "https://a.dev/path"))
        assertFalse(UrlNormalizer.isDuplicate("https://a.dev/path", "https://a.dev/other"))
    }

    @Test
    fun hostIsExtractedFromNormalizedUrl() {
        assertEquals("trade-journel.pages.dev", UrlNormalizer.hostOf("https://trade-journel.pages.dev/"))
    }
}
