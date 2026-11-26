package com.immodi.webdock.webview

import androidx.core.content.edit
import android.content.Context
import android.content.SharedPreferences

/** Remembers profile deletions that could not run because the profile was in use. */
class PendingProfileStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("pending_profiles", Context.MODE_PRIVATE)

    fun add(appId: String) {
        prefs.edit { putStringSet(KEY, get() + appId) }
    }

    fun remove(appId: String) {
        prefs.edit { putStringSet(KEY, get() - appId) }
    }

    fun get(): Set<String> = prefs.getStringSet(KEY, emptySet()).orEmpty().toSet()

    private companion object {
        const val KEY = "app_ids"
    }
}
