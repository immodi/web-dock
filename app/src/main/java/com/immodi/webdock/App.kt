package com.immodi.webdock

import android.app.Application
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import com.immodi.webdock.data.WebAppRepository
import com.immodi.webdock.data.db.WebDockDatabase
import com.immodi.webdock.data.meta.IconCache
import com.immodi.webdock.data.meta.MetadataFetcher
import com.immodi.webdock.data.meta.MetadataRepository
import com.immodi.webdock.data.prefs.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class App : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(SvgDecoder.Factory()) }
            .build()
}

class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database: WebDockDatabase by lazy { WebDockDatabase.build(appContext) }

    val webAppRepository: WebAppRepository by lazy {
        WebAppRepository(database, database.webAppDao())
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }

    val metadataRepository: MetadataRepository by lazy {
        MetadataRepository(
            fetcher = MetadataFetcher(),
            iconCache = IconCache(appContext),
            webAppRepository = webAppRepository,
            applicationScope = applicationScope,
        )
    }
}
