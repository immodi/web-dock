package com.immodi.webdock.ui.edit

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.immodi.webdock.AppContainer
import com.immodi.webdock.R
import com.immodi.webdock.domain.AppSettings
import com.immodi.webdock.domain.UrlNormalizer
import com.immodi.webdock.ui.components.WebAppIcon
import com.immodi.webdock.ui.components.hostLabel
import com.immodi.webdock.ui.settings.AppSettingsActivity
import kotlinx.serialization.json.Json

@Composable
fun EditRoute(
    container: AppContainer,
    appId: String?,
    onBack: () -> Unit,
) {
    val viewModel: EditViewModel = viewModel(key = appId ?: "new") {
        EditViewModel(
            appId,
            container.webAppRepository,
            container.metadataRepository,
            container.settingsRepository,
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                EditEvent.Saved, EditEvent.Finish -> onBack()
            }
        }
    }

    EditScreen(
        state = state,
        onUrlChange = viewModel::onUrlChange,
        onNameChange = viewModel::onNameChange,
        onTagInputChange = viewModel::onTagInputChange,
        onAddTag = viewModel::addTag,
        onRemoveTag = viewModel::removeTag,
        onSettingsChanged = viewModel::onSettingsChanged,
        onSave = viewModel::save,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    state: EditUiState,
    onUrlChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onTagInputChange: (String) -> Unit,
    onAddTag: () -> Unit,
    onRemoveTag: (String) -> Unit,
    onSettingsChanged: (AppSettings) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    val urlError = when (state.urlError) {
        EditUrlError.INVALID -> stringResource(R.string.error_invalid_url)
        EditUrlError.DUPLICATE -> stringResource(R.string.error_duplicate_url)
        null -> null
    }
    val normalizedUrl = UrlNormalizer.normalize(state.url)
    val previewHost = normalizedUrl?.let { hostLabel(it) }
    val existingIconFile = state.original?.takeIf { normalizedUrl == it.url }?.iconFile
    val context = LocalContext.current
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringExtra(AppSettingsActivity.EXTRA_SETTINGS)?.let { raw ->
                runCatching { Json.decodeFromString<AppSettings>(raw) }.getOrNull()?.let(onSettingsChanged)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isNew) R.string.add_app else R.string.edit_app,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = !state.saving && !state.loading) {
                        if (state.saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(stringResource(R.string.save))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (state.loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.url,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.url_label)) },
                placeholder = { Text(stringResource(R.string.url_placeholder)) },
                isError = urlError != null,
                supportingText = if (urlError != null) {
                    { Text(urlError) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next,
                ),
            )

            if (state.metadataLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (normalizedUrl != null && previewHost != null) {
                val previewColor = state.metadata?.themeColor?.let { Color(it) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (previewColor != null) {
                        CardDefaults.cardColors(containerColor = previewColor.copy(alpha = 0.15f))
                    } else {
                        CardDefaults.cardColors()
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        WebAppIcon(
                            name = state.name.ifBlank { previewHost },
                            host = previewHost,
                            iconUrl = state.metadata?.iconUrl,
                            iconFile = existingIconFile,
                            size = 48.dp,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.name.ifBlank { previewHost },
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = normalizedUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.name_label)) },
                placeholder = { Text(stringResource(R.string.name_placeholder)) },
                singleLine = true,
            )

            Text(
                text = stringResource(R.string.tags_label),
                style = MaterialTheme.typography.titleSmall,
            )

            if (state.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.tags.forEach { tag ->
                        InputChip(
                            selected = false,
                            onClick = {},
                            label = { Text(tag) },
                            trailingIcon = {
                                IconButton(
                                    onClick = { onRemoveTag(tag) },
                                    modifier = Modifier.size(20.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.remove_tag, tag),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.tagInput,
                onValueChange = onTagInputChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.tag_placeholder)) },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = onAddTag) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_tag),
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAddTag() }),
            )

            OutlinedButton(
                onClick = {
                    settingsLauncher.launch(
                        AppSettingsActivity.createIntent(context, state.original?.id, state.appSettings),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.app_settings))
            }

            if (state.saveFailed) {
                Text(
                    text = stringResource(R.string.error_save_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
