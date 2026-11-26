package com.immodi.webdock.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.immodi.webdock.AppContainer
import com.immodi.webdock.data.prefs.GlobalSettings
import com.immodi.webdock.data.prefs.ThemeMode

@Composable
fun WebDockAppTheme(container: AppContainer, content: @Composable () -> Unit) {
    val settings by container.settingsRepository.settings
        .collectAsStateWithLifecycle(initialValue = GlobalSettings())
    val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    WebDockTheme(darkTheme = darkTheme, dynamicColor = settings.dynamicColor, content = content)
}
