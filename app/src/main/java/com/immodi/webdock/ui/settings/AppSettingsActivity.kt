package com.immodi.webdock.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.immodi.webdock.App
import com.immodi.webdock.AppContainer
import com.immodi.webdock.R
import com.immodi.webdock.domain.AppSettings
import com.immodi.webdock.domain.WebApp
import com.immodi.webdock.ui.theme.WebDockAppTheme
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class AppSettingsActivity : ComponentActivity() {

    private val container: AppContainer get() = (application as App).container
    private val json = Json { ignoreUnknownKeys = true }
    private val saveQueue = Channel<WebApp>(Channel.CONFLATED)

    private var app: WebApp? by mutableStateOf(null)
    private var draftSettings: AppSettings by mutableStateOf(AppSettings())
    private var isDraft: Boolean = false
    private var loading: Boolean by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val appId = intent.getStringExtra(EXTRA_APP_ID)
        isDraft = appId == null

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = returnResult()
            },
        )

        lifecycleScope.launch {
            for (pending in saveQueue) {
                container.webAppRepository.save(pending, isNew = false)
            }
        }

        if (isDraft) {
            draftSettings = intent.getStringExtra(EXTRA_SETTINGS)
                ?.let { raw -> runCatching { json.decodeFromString<AppSettings>(raw) }.getOrNull() }
                ?: AppSettings()
            loading = false
        } else {
            lifecycleScope.launch {
                val loaded = container.webAppRepository.get(appId!!)
                if (loaded == null) {
                    finish()
                } else {
                    app = loaded
                    loading = false
                }
            }
        }

        setContent {
            WebDockAppTheme(container) {
                AppSettingsScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AppSettingsScreen() {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_settings)) },
                    navigationIcon = {
                        IconButton(onClick = { returnResult() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            if (loading) {
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

            val currentSettings = if (isDraft) draftSettings else app?.settings ?: AppSettings()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
            ) {
                if (!isDraft) {
                    Text(
                        text = stringResource(R.string.settings_takes_effect_next_open),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                AppSettingsEditor(
                    settings = currentSettings,
                    showIsolationNotice = currentSettings.isolateStorage &&
                        !container.profileManager.isMultiProfileSupported(),
                    onChange = ::updateSettings,
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    private fun updateSettings(settings: AppSettings) {
        if (isDraft) {
            draftSettings = settings
            return
        }
        val current = app ?: return
        val updated = current.copy(settings = settings)
        app = updated
        saveQueue.trySend(updated)
    }

    private fun returnResult() {
        if (!isDraft) {
            app?.let { current ->
                container.applicationScope.launch {
                    container.webAppRepository.save(current, isNew = false)
                }
            }
        }
        val currentSettings = if (isDraft) draftSettings else app?.settings
        val result = Intent().apply {
            currentSettings?.let { putExtra(EXTRA_SETTINGS, json.encodeToString(it)) }
        }
        setResult(RESULT_OK, result)
        finish()
    }

    companion object {
        const val EXTRA_APP_ID = "app_id"
        const val EXTRA_SETTINGS = "settings"

        fun createIntent(context: Context, appId: String?, settings: AppSettings): Intent =
            Intent(context, AppSettingsActivity::class.java).apply {
                if (appId != null) putExtra(EXTRA_APP_ID, appId)
                putExtra(EXTRA_SETTINGS, Json.encodeToString(settings))
            }
    }
}
