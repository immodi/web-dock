package com.immodi.webdock.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.immodi.webdock.data.WebAppRepository
import com.immodi.webdock.domain.UrlNormalizer
import com.immodi.webdock.domain.WebApp
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class EditUrlError { INVALID, DUPLICATE }

data class EditUiState(
    val isNew: Boolean = true,
    val loading: Boolean = false,
    val original: WebApp? = null,
    val url: String = "",
    val name: String = "",
    val nameEdited: Boolean = false,
    val tags: List<String> = emptyList(),
    val tagInput: String = "",
    val urlError: EditUrlError? = null,
    val saving: Boolean = false,
    val saveFailed: Boolean = false,
)

sealed interface EditEvent {
    data object Saved : EditEvent
    data object Finish : EditEvent
}

class EditViewModel(
    private val appId: String?,
    private val repository: WebAppRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EditUiState(isNew = appId == null, loading = appId != null))
    val state = _state.asStateFlow()

    private val _events = Channel<EditEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var duplicateJob: Job? = null

    init {
        if (appId != null) {
            viewModelScope.launch {
                val app = repository.get(appId)
                if (app == null) {
                    _events.send(EditEvent.Finish)
                } else {
                    _state.value = EditUiState(
                        isNew = false,
                        loading = false,
                        original = app,
                        url = app.url,
                        name = app.name,
                        nameEdited = app.nameEdited,
                        tags = app.tags,
                    )
                }
            }
        }
    }

    fun onUrlChange(value: String) {
        _state.update { it.copy(url = value, urlError = null, saveFailed = false) }
        duplicateJob?.cancel()
        val normalized = UrlNormalizer.normalize(value) ?: return
        duplicateJob = viewModelScope.launch {
            delay(350)
            val duplicate = repository.findDuplicate(normalized, appId)
            if (duplicate != null) {
                _state.update { it.copy(urlError = EditUrlError.DUPLICATE) }
            }
        }
    }

    fun onNameChange(value: String) {
        _state.update { it.copy(name = value, nameEdited = true, saveFailed = false) }
    }

    fun onTagInputChange(value: String) {
        _state.update { it.copy(tagInput = value) }
    }

    fun addTag() {
        _state.update { current ->
            val tag = current.tagInput.trim()
            when {
                tag.isEmpty() -> current
                current.tags.any { it.equals(tag, ignoreCase = true) } -> current.copy(tagInput = "")
                else -> current.copy(tags = current.tags + tag, tagInput = "")
            }
        }
    }

    fun removeTag(tag: String) {
        _state.update { current -> current.copy(tags = current.tags.filterNot { it == tag }) }
    }

    fun save() {
        val current = _state.value
        if (current.saving) return

        val normalized = UrlNormalizer.normalize(current.url)
        if (normalized == null) {
            _state.update { it.copy(urlError = EditUrlError.INVALID) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(saving = true, saveFailed = false) }

            val duplicate = repository.findDuplicate(normalized, appId)
            if (duplicate != null) {
                _state.update { it.copy(saving = false, urlError = EditUrlError.DUPLICATE) }
                return@launch
            }

            val host = UrlNormalizer.hostOf(normalized) ?: normalized
            val original = current.original
            val name = when {
                current.nameEdited -> current.name.trim().ifEmpty { host }
                original == null -> host
                normalized != original.url -> host
                else -> current.name
            }
            val app = if (original == null) {
                WebApp(
                    url = normalized,
                    name = name,
                    nameEdited = current.nameEdited,
                    tags = current.tags,
                )
            } else {
                original.copy(
                    url = normalized,
                    name = name,
                    nameEdited = current.nameEdited,
                    tags = current.tags,
                )
            }

            repository.save(app, isNew = original == null)
                .onSuccess { _events.send(EditEvent.Saved) }
                .onFailure { _state.update { it.copy(saving = false, saveFailed = true) } }
        }
    }
}
