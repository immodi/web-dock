package com.immodi.webdock.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.immodi.webdock.R
import com.immodi.webdock.domain.AppSettings
import com.immodi.webdock.domain.LinkPolicy
import com.immodi.webdock.domain.OrientationMode
import kotlin.math.roundToInt

@Composable
fun AppSettingsEditor(
    settings: AppSettings,
    showIsolationNotice: Boolean,
    onChange: (AppSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SettingSwitch(
            title = stringResource(R.string.setting_isolate_storage),
            summary = stringResource(R.string.setting_isolate_storage_summary),
            checked = settings.isolateStorage,
            onCheckedChange = { onChange(settings.copy(isolateStorage = it)) },
        )
        if (showIsolationNotice) {
            Text(
                text = stringResource(R.string.setting_isolate_unsupported),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        SettingSwitch(
            title = stringResource(R.string.setting_desktop_mode),
            summary = stringResource(R.string.setting_desktop_mode_summary),
            checked = settings.desktopMode,
            onCheckedChange = { onChange(settings.copy(desktopMode = it)) },
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.setting_zoom),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.setting_zoom_value, settings.zoomPercent),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Slider(
                value = settings.zoomPercent.toFloat(),
                onValueChange = { value ->
                    val stepped = ((value / 10f).roundToInt() * 10).coerceIn(50, 200)
                    if (stepped != settings.zoomPercent) {
                        onChange(settings.copy(zoomPercent = stepped))
                    }
                },
                valueRange = 50f..200f,
                steps = 14,
            )
        }

        Text(
            text = stringResource(R.string.setting_orientation),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OrientationMode.entries.forEach { mode ->
                FilterChip(
                    selected = settings.orientation == mode,
                    onClick = { onChange(settings.copy(orientation = mode)) },
                    label = { Text(stringResource(orientationLabel(mode))) },
                )
            }
        }

        SettingSwitch(
            title = stringResource(R.string.setting_keep_screen_on),
            checked = settings.keepScreenOn,
            onCheckedChange = { onChange(settings.copy(keepScreenOn = it)) },
        )
        SettingSwitch(
            title = stringResource(R.string.setting_pull_to_refresh),
            checked = settings.pullToRefresh,
            onCheckedChange = { onChange(settings.copy(pullToRefresh = it)) },
        )
        SettingSwitch(
            title = stringResource(R.string.setting_javascript),
            checked = settings.javascriptEnabled,
            onCheckedChange = { onChange(settings.copy(javascriptEnabled = it)) },
        )
        SettingSwitch(
            title = stringResource(R.string.setting_third_party_cookies),
            checked = settings.thirdPartyCookies,
            onCheckedChange = { onChange(settings.copy(thirdPartyCookies = it)) },
        )

        Text(
            text = stringResource(R.string.setting_link_policy),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            LinkPolicy.entries.forEach { policy ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChange(settings.copy(linkPolicy = policy)) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = settings.linkPolicy == policy,
                        onClick = { onChange(settings.copy(linkPolicy = policy)) },
                    )
                    Text(
                        text = stringResource(linkPolicyLabel(policy)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        AllowedHostsField(
            settings = settings,
            onChange = onChange,
        )

        SettingSwitch(
            title = stringResource(R.string.setting_immersive),
            summary = stringResource(R.string.setting_immersive_summary),
            checked = settings.immersive,
            onCheckedChange = { onChange(settings.copy(immersive = it)) },
        )
    }
}

@Composable
private fun AllowedHostsField(
    settings: AppSettings,
    onChange: (AppSettings) -> Unit,
) {
    var text by remember { mutableStateOf(settings.allowedHosts.joinToString(", ")) }

    LaunchedEffect(settings.allowedHosts) {
        val parsed = text.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (parsed != settings.allowedHosts) {
            text = settings.allowedHosts.joinToString(", ")
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = { value ->
            text = value
            val hosts = value.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            onChange(settings.copy(allowedHosts = hosts))
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        label = { Text(stringResource(R.string.setting_allowed_hosts)) },
        placeholder = { Text(stringResource(R.string.setting_allowed_hosts_hint)) },
        singleLine = true,
    )
}

@Composable
private fun SettingSwitch(
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

private fun orientationLabel(mode: OrientationMode): Int = when (mode) {
    OrientationMode.AUTO -> R.string.orientation_auto
    OrientationMode.PORTRAIT -> R.string.orientation_portrait
    OrientationMode.LANDSCAPE -> R.string.orientation_landscape
}

private fun linkPolicyLabel(policy: LinkPolicy): Int = when (policy) {
    LinkPolicy.SAME_HOST_IN_APP -> R.string.link_policy_same_host
    LinkPolicy.ALL_IN_APP -> R.string.link_policy_all_in_app
    LinkPolicy.ALL_EXTERNAL -> R.string.link_policy_all_external
}
