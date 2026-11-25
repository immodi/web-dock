package com.immodi.webdock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.immodi.webdock.data.prefs.GlobalSettings
import com.immodi.webdock.data.prefs.ThemeMode
import com.immodi.webdock.ui.WebDockApp
import com.immodi.webdock.ui.theme.WebDockTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as App).container

        setContent {
            val settings by container.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = GlobalSettings())
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            WebDockTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.dynamicColor,
            ) {
                WebDockApp(container)
            }
        }
    }
}
