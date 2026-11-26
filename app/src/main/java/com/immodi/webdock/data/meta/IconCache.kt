package com.immodi.webdock.data.meta

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Downloads the chosen icon, renders it (PNG, JPEG, WebP, or SVG) and stores a
 * PNG capped at 512 px under filesDir/icons/<appId>.png.
 */
class IconCache(private val context: Context) {

    suspend fun cache(appId: String, iconUrl: String?): String? = withContext(Dispatchers.IO) {
        if (iconUrl.isNullOrBlank()) return@withContext null
        val request = ImageRequest.Builder(context)
            .data(iconUrl)
            .allowHardware(false)
            .size(ICON_SIZE_PX)
            .build()

        val drawable = (Coil.imageLoader(context).execute(request) as? SuccessResult)
            ?.drawable
            ?: return@withContext null

        val bitmap = drawable.toBitmap(ICON_SIZE_PX, ICON_SIZE_PX, Bitmap.Config.ARGB_8888)
        val directory = File(context.filesDir, ICONS_DIRECTORY).apply { mkdirs() }
        val file = File(directory, "$appId.png")
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        file.name
    }

    fun delete(appId: String) {
        File(context.filesDir, "$ICONS_DIRECTORY/$appId.png").delete()
    }

    private companion object {
        const val ICONS_DIRECTORY = "icons"
        const val ICON_SIZE_PX = 512
    }
}
