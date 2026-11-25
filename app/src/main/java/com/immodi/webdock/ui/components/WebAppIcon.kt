package com.immodi.webdock.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import java.io.File

@Composable
fun WebAppIcon(
    name: String,
    host: String,
    iconUrl: String?,
    iconFile: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val context = LocalContext.current
    val model: Any? = when {
        !iconFile.isNullOrBlank() -> File(context.filesDir, "icons/$iconFile")
        !iconUrl.isNullOrBlank() -> iconUrl
        else -> null
    }

    if (model == null) {
        AppIcon(name = name, seed = host, size = size, modifier = modifier)
        return
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(model)
            .build(),
        contentDescription = null,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f)),
        contentScale = ContentScale.Crop,
    ) {
        if (painter.state is AsyncImagePainter.State.Success) {
            SubcomposeAsyncImageContent()
        } else {
            AppIcon(name = name, seed = host, size = size)
        }
    }
}
