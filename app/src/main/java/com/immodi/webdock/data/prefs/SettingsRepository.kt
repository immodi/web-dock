package com.immodi.webdock.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.immodi.webdock.domain.AppSettings
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class HomeView { GRID, LIST }

data class GlobalSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val defaultView: HomeView = HomeView.GRID,
    val showEdgeHandle: Boolean = true,
    val defaultAppSettings: AppSettings = AppSettings(),
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<GlobalSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            GlobalSettings(
                themeMode = prefs[THEME_MODE]?.toThemeMode() ?: ThemeMode.SYSTEM,
                dynamicColor = prefs[DYNAMIC_COLOR] ?: true,
                defaultView = prefs[DEFAULT_VIEW]?.toHomeView() ?: HomeView.GRID,
                showEdgeHandle = prefs[SHOW_EDGE_HANDLE] ?: true,
                defaultAppSettings = prefs[DEFAULT_APP_SETTINGS]
                    ?.let { runCatching { json.decodeFromString<AppSettings>(it) }.getOrNull() }
                    ?: AppSettings(),
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[DYNAMIC_COLOR] = enabled }
    }

    suspend fun setDefaultView(view: HomeView) {
        dataStore.edit { it[DEFAULT_VIEW] = view.name }
    }

    suspend fun setShowEdgeHandle(visible: Boolean) {
        dataStore.edit { it[SHOW_EDGE_HANDLE] = visible }
    }

    suspend fun setDefaultAppSettings(settings: AppSettings) {
        dataStore.edit { it[DEFAULT_APP_SETTINGS] = json.encodeToString(settings) }
    }

    private fun String.toThemeMode(): ThemeMode =
        runCatching { ThemeMode.valueOf(this) }.getOrDefault(ThemeMode.SYSTEM)

    private fun String.toHomeView(): HomeView =
        runCatching { HomeView.valueOf(this) }.getOrDefault(HomeView.GRID)

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DEFAULT_VIEW = stringPreferencesKey("default_view")
        val SHOW_EDGE_HANDLE = booleanPreferencesKey("show_edge_handle")
        val DEFAULT_APP_SETTINGS = stringPreferencesKey("default_app_settings")
    }
}
