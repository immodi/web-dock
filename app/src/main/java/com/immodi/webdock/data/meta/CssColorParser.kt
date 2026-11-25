package com.immodi.webdock.data.meta

import kotlin.math.roundToInt

/**
 * Tolerant CSS color parsing. Returns an ARGB int, or null when the value is not
 * a color this parser understands. Invalid values are ignored by callers.
 */
object CssColorParser {

    private val HEX = Regex("^#([0-9a-fA-F]{3,8})$")
    private val RGB_FUNCTION = Regex("^rgba?\\(([^)]*)\\)$", RegexOption.IGNORE_CASE)
    private val WHITESPACE = Regex("[\\s,]+")

    fun parse(value: String?): Int? {
        val text = value?.trim()?.lowercase().orEmpty()
        if (text.isEmpty()) return null

        NAMED_COLORS[text]?.let { return it }

        HEX.matchEntire(text)?.let { match -> return parseHex(match.groupValues[1]) }

        RGB_FUNCTION.matchEntire(text)?.let { match ->
            return parseFunctional(match.groupValues[1])
        }

        return null
    }

    private fun parseHex(hex: String): Int? {
        if (!hex.all { it.isDigit() || it in 'a'..'f' }) return null
        fun channel(start: Int, length: Int): Int = hex.substring(start, start + length).toInt(16)

        return when (hex.length) {
            3 -> argb(255, channel(0, 1) * 17, channel(1, 1) * 17, channel(2, 1) * 17)
            4 -> argb(
                channel(3, 1) * 17,
                channel(0, 1) * 17,
                channel(1, 1) * 17,
                channel(2, 1) * 17,
            )

            6 -> argb(255, channel(0, 2), channel(2, 2), channel(4, 2))
            8 -> argb(channel(6, 2), channel(0, 2), channel(2, 2), channel(4, 2))
            else -> null
        }
    }

    private fun parseFunctional(arguments: String): Int? {
        val parts = arguments
            .replace("/", " ")
            .split(WHITESPACE)
            .filter { it.isNotBlank() }
        if (parts.size !in 3..4) return null

        val red = parseChannel(parts[0]) ?: return null
        val green = parseChannel(parts[1]) ?: return null
        val blue = parseChannel(parts[2]) ?: return null
        val alpha = if (parts.size == 4) parseAlpha(parts[3]) ?: return null else 255
        return argb(alpha, red, green, blue)
    }

    private fun parseChannel(value: String): Int? {
        val number = if (value.endsWith("%")) {
            value.dropLast(1).toFloatOrNull()?.let { it * 255f / 100f }
        } else {
            value.toFloatOrNull()
        } ?: return null
        return number.roundToInt().coerceIn(0, 255)
    }

    private fun parseAlpha(value: String): Int? {
        val number = if (value.endsWith("%")) {
            value.dropLast(1).toFloatOrNull()?.let { it * 255f / 100f }
        } else {
            value.toFloatOrNull()?.let { if (it <= 1f) it * 255f else it }
        } ?: return null
        return number.roundToInt().coerceIn(0, 255)
    }

    private fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int =
        (alpha shl 24) or (red shl 16) or (green shl 8) or blue

    private val NAMED_COLORS: Map<String, Int> = mapOf(
        "transparent" to 0x00000000,
        "black" to 0xFF000000.toInt(),
        "silver" to 0xFFC0C0C0.toInt(),
        "gray" to 0xFF808080.toInt(),
        "grey" to 0xFF808080.toInt(),
        "white" to 0xFFFFFFFF.toInt(),
        "maroon" to 0xFF800000.toInt(),
        "red" to 0xFFFF0000.toInt(),
        "purple" to 0xFF800080.toInt(),
        "fuchsia" to 0xFFFF00FF.toInt(),
        "magenta" to 0xFFFF00FF.toInt(),
        "green" to 0xFF008000.toInt(),
        "lime" to 0xFF00FF00.toInt(),
        "olive" to 0xFF808000.toInt(),
        "yellow" to 0xFFFFFF00.toInt(),
        "navy" to 0xFF000080.toInt(),
        "blue" to 0xFF0000FF.toInt(),
        "teal" to 0xFF008080.toInt(),
        "aqua" to 0xFF00FFFF.toInt(),
        "cyan" to 0xFF00FFFF.toInt(),
        "orange" to 0xFFFFA500.toInt(),
        "pink" to 0xFFFFC0CB.toInt(),
        "brown" to 0xFFA52A2A.toInt(),
        "gold" to 0xFFFFD700.toInt(),
        "indigo" to 0xFF4B0082.toInt(),
        "violet" to 0xFFEE82EE.toInt(),
        "rebeccapurple" to 0xFF663399.toInt(),
        "darkgray" to 0xFFA9A9A9.toInt(),
        "darkgrey" to 0xFFA9A9A9.toInt(),
        "lightgray" to 0xFFD3D3D3.toInt(),
        "lightgrey" to 0xFFD3D3D3.toInt(),
        "whitesmoke" to 0xFFF5F5F5.toInt(),
        "dodgerblue" to 0xFF1E90FF.toInt(),
    )
}
