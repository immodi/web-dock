package com.immodi.webdock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.immodi.webdock.domain.UrlNormalizer

fun hostLabel(url: String): String = UrlNormalizer.hostOf(url) ?: url

fun avatarColor(seed: String): Color {
    val hash = seed.hashCode().toLong() and 0xFFFFFFFFL
    val hue = (hash * 360L / 0xFFFFFFFFL).toFloat()
    return Color.hsl(hue, 0.42f, 0.44f)
}

@Composable
fun AppIcon(
    name: String,
    seed: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val color = remember(seed) { avatarColor(seed) }
    val letter = remember(name) {
        name.trim().firstOrNull()?.uppercaseChar()?.toString()
            ?: seed.firstOrNull()?.uppercaseChar()?.toString()
            ?: "?"
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            color = Color.White,
            fontSize = (size.value * 0.42f).sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
