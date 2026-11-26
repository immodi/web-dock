package com.immodi.webdock.ui.settings

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.immodi.webdock.AppContainer
import com.immodi.webdock.BuildConfig
import com.immodi.webdock.R
import com.immodi.webdock.data.prefs.GlobalSettings
import com.immodi.webdock.data.prefs.HomeView
import com.immodi.webdock.data.prefs.ThemeMode
import com.immodi.webdock.domain.AppSettings
import kotlinx.coroutines.launch

private const val README_URL = "https://github.com/immodi/web-dock#readme"

@Composable
fun SettingsRoute(container: AppContainer, onBack: () -> Unit) {
    val settings by container.settingsRepository.settings
        .collectAsStateWithLifecycle(initialValue = GlobalSettings())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    SettingsScreen(
        settings = settings,
        onBack = onBack,
        onThemeMode = { mode -> scope.launch { container.settingsRepository.setThemeMode(mode) } },
        onDynamicColor = { enabled ->
            scope.launch { container.settingsRepository.setDynamicColor(enabled) }
        },
        onDefaultView = { view ->
            scope.launch { container.settingsRepository.setDefaultView(view) }
        },
        onEdgeHandle = { visible ->
            scope.launch { container.settingsRepository.setShowEdgeHandle(visible) }
        },
        onDefaultAppSettings = { appSettings ->
            scope.launch { container.settingsRepository.setDefaultAppSettings(appSettings) }
        },
        onOpenReadme = {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, README_URL.toUri()))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: GlobalSettings,
    onBack: () -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onDefaultView: (HomeView) -> Unit,
    onEdgeHandle: (Boolean) -> Unit,
    onDefaultAppSettings: (AppSettings) -> Unit,
    onOpenReadme: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {
            SectionHeader(stringResource(R.string.settings_appearance))

            Text(
                text = stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { onThemeMode(mode) },
                        label = { Text(stringResource(themeLabel(mode))) },
                    )
                }
            }

            SettingsSwitchRow(
                title = stringResource(R.string.settings_dynamic_color),
                summary = stringResource(R.string.settings_dynamic_color_summary),
                checked = settings.dynamicColor,
                onCheckedChange = onDynamicColor,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionHeader(stringResource(R.string.settings_home))

            Text(
                text = stringResource(R.string.settings_default_view),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = settings.defaultView == HomeView.GRID,
                    onClick = { onDefaultView(HomeView.GRID) },
                    label = { Text(stringResource(R.string.view_grid)) },
                )
                FilterChip(
                    selected = settings.defaultView == HomeView.LIST,
                    onClick = { onDefaultView(HomeView.LIST) },
                    label = { Text(stringResource(R.string.view_list)) },
                )
            }

            SettingsSwitchRow(
                title = stringResource(R.string.settings_edge_handle),
                summary = stringResource(R.string.settings_edge_handle_summary),
                checked = settings.showEdgeHandle,
                onCheckedChange = onEdgeHandle,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionHeader(stringResource(R.string.settings_defaults))
            AppSettingsEditor(
                settings = settings.defaultAppSettings,
                showIsolationNotice = false,
                onChange = onDefaultAppSettings,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionHeader(stringResource(R.string.settings_about))
            Text(
                text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            Text(
                text = stringResource(R.string.settings_readme),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenReadme)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    summary: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun themeLabel(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}
