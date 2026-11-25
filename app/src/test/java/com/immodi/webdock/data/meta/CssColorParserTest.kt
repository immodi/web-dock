package com.immodi.webdock.data.meta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CssColorParserTest {

    @Test
    fun parsesShortHex() {
        assertEquals(0xFFFFFFFF.toInt(), CssColorParser.parse("#fff"))
    }

    @Test
    fun parsesSixDigitHex() {
        assertEquals(0xFF123456.toInt(), CssColorParser.parse("#123456"))
    }

    @Test
    fun parsesEightDigitHexAsRgba() {
        assertEquals(0x78123456, CssColorParser.parse("#12345678"))
    }

    @Test
    fun parsesRgbFunction() {
        assertEquals(0xFF010203.toInt(), CssColorParser.parse("rgb(1, 2, 3)"))
    }

    @Test
    fun parsesRgbaFunction() {
        assertEquals(0x80FF0000.toInt(), CssColorParser.parse("rgba(255, 0, 0, 0.5)"))
    }

    @Test
    fun parsesModernSpaceSyntax() {
        assertEquals(0xFFFF0000.toInt(), CssColorParser.parse("rgb(100% 0% 0%)"))
        assertEquals(0x80FF0000.toInt(), CssColorParser.parse("rgb(255 0 0 / 50%)"))
    }

    @Test
    fun parsesNamedColors() {
        assertEquals(0xFFFF0000.toInt(), CssColorParser.parse("red"))
        assertEquals(0xFF663399.toInt(), CssColorParser.parse("rebeccapurple"))
        assertEquals(0x00000000, CssColorParser.parse("transparent"))
    }

    @Test
    fun ignoresInvalidValues() {
        assertNull(CssColorParser.parse("not-a-color"))
        assertNull(CssColorParser.parse("#12"))
        assertNull(CssColorParser.parse("rgb(1, 2)"))
        assertNull(CssColorParser.parse(null))
    }
}
