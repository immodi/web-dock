package com.immodi.webdock.data.meta

enum class IconSource(val priority: Int) {
    MANIFEST(3),
    APPLE_TOUCH(2),
    ICON(1),
}

data class IconCandidate(
    val url: String,
    val width: Int? = null,
    val type: String? = null,
    val source: IconSource,
)

object IconSelector {

    /**
     * Prefers manifest icons, then apple-touch-icon, then rel=icon. Within the
     * winning source, prefers PNG (or unknown raster) over SVG, then the
     * smallest icon at or above 192 px, otherwise the largest available.
     */
    fun select(candidates: List<IconCandidate>): IconCandidate? {
        if (candidates.isEmpty()) return null

        val bestSource = candidates.maxByOrNull { it.source.priority }?.source ?: return null
        val group = candidates.filter { it.source == bestSource }
        val raster = group.filterNot { isSvg(it) }
        val pool = raster.ifEmpty { group }

        val atLeast192 = pool.filter { (it.width ?: 0) >= 192 }
        return if (atLeast192.isNotEmpty()) {
            atLeast192.minByOrNull { it.width ?: Int.MAX_VALUE }
        } else {
            pool.maxByOrNull { it.width ?: 0 }
        }
    }

    fun isSvg(candidate: IconCandidate): Boolean =
        candidate.type?.contains("svg", ignoreCase = true) == true ||
            candidate.url.substringBefore('?').endsWith(".svg", ignoreCase = true)

    fun parseWidth(sizes: String?): Int? {
        if (sizes.isNullOrBlank()) return null
        if (sizes.equals("any", ignoreCase = true)) return null
        val dimensions = Regex("(\\d+)x(\\d+)", RegexOption.IGNORE_CASE)
            .findAll(sizes)
            .mapNotNull { it.groupValues[1].toIntOrNull() }
            .toList()
        return dimensions.maxOrNull()
    }
}
