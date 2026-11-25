package com.immodi.webdock

import android.app.Application
import android.content.Context
import com.immodi.webdock.data.WebAppRepository
import com.immodi.webdock.data.db.WebDockDatabase
import com.immodi.webdock.data.prefs.SettingsRepository

class App : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    private val database: WebDockDatabase by lazy { WebDockDatabase.build(appContext) }

    val webAppRepository: WebAppRepository by lazy {
        WebAppRepository(database, database.webAppDao())
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
}
