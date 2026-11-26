package com.immodi.webdock.data.meta

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class MetadataFetcherTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun readsTitleManifestAndIcon() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/html; charset=utf-8")
                .setBody(
                    """
                    <!doctype html>
                    <html><head>
                      <title>Page Title</title>
                      <meta name="theme-color" content="#102030">
                      <link rel="manifest" href="/manifest.json">
                    </head><body></body></html>
                    """.trimIndent(),
                ),
        )
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/manifest+json")
                .setBody(
                    """
                    {
                      "name": "Manifest Name",
                      "theme_color": "#aabbcc",
                      "background_color": "rgb(255, 255, 255)",
                      "icons": [
                        { "src": "/icons/192.png", "sizes": "192x192", "type": "image/png" },
                        { "src": "/icons/512.png", "sizes": "512x512", "type": "image/png" }
                      ]
                    }
                    """.trimIndent(),
                ),
        )

        val metadata = MetadataFetcher(OkHttpClient()).fetch(server.url("/page").toString())

        assertNotNull(metadata)
        assertEquals("Page Title", metadata!!.title)
        assertEquals("Manifest Name", metadata.name)
        assertEquals(0xFFAABBCC.toInt(), metadata.themeColor)
        assertEquals(0xFFFFFFFF.toInt(), metadata.backgroundColor)
        assertEquals(server.url("/icons/192.png").toString(), metadata.iconUrl)
    }

    @Test
    fun fallsBackToTitleAndFavicon() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/html")
                .setBody("<html><head><title>No Icons</title></head><body></body></html>"),
        )

        val metadata = MetadataFetcher(OkHttpClient()).fetch(server.url("/app").toString())

        assertNotNull(metadata)
        assertEquals("No Icons", metadata!!.name)
        assertEquals(server.url("/favicon.ico").toString(), metadata.iconUrl)
        assertNull(metadata.themeColor)
    }

    @Test
    fun returnsNullWhenTheConnectionFails() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val metadata = MetadataFetcher(OkHttpClient()).fetch(server.url("/page").toString())

        assertNull(metadata)
    }

    @Test
    fun keepsPageMetadataWhenManifestIsBroken() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/html")
                .setBody(
                    """
                    <html><head>
                      <title>Plain Page</title>
                      <link rel="manifest" href="/broken.json">
                    </head><body></body></html>
                    """.trimIndent(),
                ),
        )
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{ definitely not json"),
        )

        val metadata = MetadataFetcher(OkHttpClient()).fetch(server.url("/page").toString())

        assertNotNull(metadata)
        assertEquals("Plain Page", metadata!!.name)
    }

    @Test
    fun rejectsInvalidUrls() = runTest {
        assertNull(MetadataFetcher(OkHttpClient()).fetch("not a url"))
        assertNull(MetadataFetcher(OkHttpClient()).fetch("javascript:alert(1)"))
    }

    @Test
    fun givesUpAfterFiveRedirects() = runTest {
        repeat(6) { index ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(302)
                    .setHeader("Location", "/redirect-${index + 1}"),
            )
        }
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/html")
                .setBody("<html><head><title>Final</title></head></html>"),
        )

        val metadata = MetadataFetcher().fetch(server.url("/start").toString())

        assertNull(metadata)
    }
}
