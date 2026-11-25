package com.immodi.webdock

import android.app.Application
import android.content.Context

class App : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
}
