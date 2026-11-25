package com.immodi.webdock.data.meta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PageMetadataParserTest {

    private val baseUrl = "https://trade-journel.pages.dev/"

    @Test
    fun parsesFullDocument() {
        val page = PageMetadataParser.parse(fixture("full.html"), baseUrl)

        assertEquals("Trade Journal — Home", page.title)
        assertEquals("Journal App", page.applicationName)
        assertEquals("#123456", page.themeColor)
        assertEquals("https://trade-journel.pages.dev/manifest.webmanifest", page.manifestUrl)
        assertEquals(3, page.icons.size)
        assertEquals("https://trade-journel.pages.dev/apple-touch-icon.png", page.icons[0].url)
        assertEquals("https://trade-journel.pages.dev/favicon.png", page.icons[2].url)
    }

    @Test
    fun parsesTitleOnlyDocument() {
        val page = PageMetadataParser.parse(fixture("title-only.html"), baseUrl)
        assertEquals("Only Title", page.title)
        assertNull(page.themeColor)
        assertNull(page.manifestUrl)
        assertEquals(0, page.icons.size)
    }

    @Test
    fun parsesFaviconOnlyDocument() {
        val page = PageMetadataParser.parse(fixture("favicon-only.html"), baseUrl)
        assertEquals(1, page.icons.size)
        assertEquals(IconSource.ICON, page.icons.single().source)
        assertEquals(64, page.icons.single().width)
    }

    @Test
    fun resolvesRelativeIconPaths() {
        val page = PageMetadataParser.parse(fixture("relative-icons.html"), baseUrl)
        assertEquals("https://trade-journel.pages.dev/touch.png", page.icons[0].url)
        assertEquals("https://trade-journel.pages.dev/assets/icon.png", page.icons[1].url)
    }

    @Test
    fun survivesBrokenHtml() {
        val page = PageMetadataParser.parse(fixture("broken.html"), baseUrl)
        assertEquals("Broken HTML", page.title)
        assertNull(CssColorParser.parse(page.themeColor))
    }

    @Test
    fun parsesManifest() {
        val manifestUrl = "https://trade-journel.pages.dev/manifest.webmanifest"
        val manifest = ManifestParser.parse(fixture("manifest.webmanifest"), manifestUrl)
        assertNotNull(manifest)
        assertEquals("Trade Journal", manifest!!.name)
        assertEquals("Journal", manifest.shortName)
        assertEquals("rgb(1, 2, 3)", manifest.themeColor)
        assertEquals("#fff", manifest.backgroundColor)
        assertEquals("https://trade-journel.pages.dev/?source=pwa", manifest.startUrl)
        assertEquals(3, manifest.icons.size)
        assertEquals("https://trade-journel.pages.dev/icons/192.png", manifest.icons[0].url)
        assertEquals(1024, manifest.icons[1].width)
    }

    @Test
    fun rejectsMalformedManifest() {
        assertNull(ManifestParser.parse("{ not json", "https://a.dev/manifest.json"))
    }

    private fun fixture(name: String): String =
        javaClass.getResource("/fixtures/$name")!!.readText()
}
