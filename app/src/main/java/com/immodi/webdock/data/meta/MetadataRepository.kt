package com.immodi.webdock.data.meta

import com.immodi.webdock.data.WebAppRepository
import com.immodi.webdock.domain.WebApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MetadataRepository(
    private val fetcher: MetadataFetcher,
    private val iconCache: IconCache,
    private val webAppRepository: WebAppRepository,
    private val applicationScope: CoroutineScope,
) {

    suspend fun fetch(url: String): Metadata? = fetcher.fetch(url)

    /** Refreshes metadata and icons for an existing app. Returns false when the fetch failed. */
    suspend fun refresh(app: WebApp): Boolean {
        val metadata = fetcher.fetch(app.url) ?: return false
        val iconFile = iconCache.cache(app.id, metadata.iconUrl)
        val updated = app.copy(
            name = if (app.nameEdited) app.name else metadata.name ?: app.name,
            iconUrl = metadata.iconUrl ?: app.iconUrl,
            iconFile = iconFile ?: app.iconFile,
            themeColor = metadata.themeColor ?: app.themeColor,
            backgroundColor = metadata.backgroundColor ?: app.backgroundColor,
        )
        webAppRepository.save(updated, isNew = false)
        return true
    }

    /**
     * Runs after a save so the UI can return immediately. A failure leaves the
     * remote icon URL in place, which the card can still load.
     */
    fun cacheIconInBackground(appId: String, iconUrl: String?) {
        if (iconUrl.isNullOrBlank()) return
        applicationScope.launch {
            val iconFile = iconCache.cache(appId, iconUrl) ?: return@launch
            webAppRepository.get(appId)?.let { app ->
                webAppRepository.save(app.copy(iconFile = iconFile), isNew = false)
            }
        }
    }
}
