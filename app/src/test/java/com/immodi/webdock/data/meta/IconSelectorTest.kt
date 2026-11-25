package com.immodi.webdock.data.meta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IconSelectorTest {

    @Test
    fun returnsNullWithoutCandidates() {
        assertNull(IconSelector.select(emptyList()))
    }

    @Test
    fun prefersManifestIconsOverPageIcons() {
        val selected = IconSelector.select(
            listOf(
                IconCandidate("https://a.dev/favicon.png", 512, "image/png", IconSource.ICON),
                IconCandidate("https://a.dev/manifest-192.png", 192, "image/png", IconSource.MANIFEST),
            ),
        )
        assertEquals("https://a.dev/manifest-192.png", selected?.url)
    }

    @Test
    fun prefersAppleTouchIconOverRelIcon() {
        val selected = IconSelector.select(
            listOf(
                IconCandidate("https://a.dev/favicon.png", 512, "image/png", IconSource.ICON),
                IconCandidate("https://a.dev/touch.png", 180, "image/png", IconSource.APPLE_TOUCH),
            ),
        )
        assertEquals("https://a.dev/touch.png", selected?.url)
    }

    @Test
    fun prefersSmallestIconAtOrAbove192() {
        val selected = IconSelector.select(
            listOf(
                IconCandidate("https://a.dev/icons/72.png", 72, "image/png", IconSource.MANIFEST),
                IconCandidate("https://a.dev/icons/192.png", 192, "image/png", IconSource.MANIFEST),
                IconCandidate("https://a.dev/icons/512.png", 512, "image/png", IconSource.MANIFEST),
            ),
        )
        assertEquals("https://a.dev/icons/192.png", selected?.url)
    }

    @Test
    fun fallsBackToLargestWhenAllIconsAreSmall() {
        val selected = IconSelector.select(
            listOf(
                IconCandidate("https://a.dev/icons/32.png", 32, "image/png", IconSource.MANIFEST),
                IconCandidate("https://a.dev/icons/64.png", 64, "image/png", IconSource.MANIFEST),
            ),
        )
        assertEquals("https://a.dev/icons/64.png", selected?.url)
    }

    @Test
    fun prefersPngOverSvg() {
        val selected = IconSelector.select(
            listOf(
                IconCandidate("https://a.dev/icon.svg", null, "image/svg+xml", IconSource.MANIFEST),
                IconCandidate("https://a.dev/icon-256.png", 256, "image/png", IconSource.MANIFEST),
            ),
        )
        assertEquals("https://a.dev/icon-256.png", selected?.url)
    }

    @Test
    fun usesSvgWhenItIsTheOnlyOption() {
        val selected = IconSelector.select(
            listOf(IconCandidate("https://a.dev/icon.svg", null, "image/svg+xml", IconSource.MANIFEST)),
        )
        assertTrue(IconSelector.isSvg(selected!!))
    }

    @Test
    fun parsesSizeAttribute() {
        assertEquals(512, IconSelector.parseWidth("192x192 512x512"))
        assertEquals(180, IconSelector.parseWidth("180x180"))
        assertNull(IconSelector.parseWidth("any"))
        assertNull(IconSelector.parseWidth(null))
    }
}
