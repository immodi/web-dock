package com.immodi.webdock.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.immodi.webdock.data.WebAppRepository
import com.immodi.webdock.data.prefs.HomeView
import com.immodi.webdock.data.prefs.SettingsRepository
import com.immodi.webdock.domain.UrlNormalizer
import com.immodi.webdock.domain.WebApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val apps: List<WebApp> = emptyList(),
    val totalCount: Int = 0,
    val query: String = "",
    val allTags: List<String> = emptyList(),
    val selectedTags: Set<String> = emptySet(),
    val view: HomeView = HomeView.GRID,
    val canReorder: Boolean = false,
) {
    val hasActiveFilters: Boolean get() = query.isNotBlank() || selectedTags.isNotEmpty()
}

class HomeViewModel(
    private val repository: WebAppRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val selectedTags = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<HomeUiState> = combine(
        repository.apps,
        settingsRepository.settings,
        query,
        selectedTags,
    ) { apps, settings, currentQuery, tags ->
        val filtered = apps.filter { app ->
            val matchesQuery = currentQuery.isBlank() ||
                app.name.contains(currentQuery, ignoreCase = true) ||
                UrlNormalizer.hostOf(app.url)?.contains(currentQuery, ignoreCase = true) == true ||
                app.tags.any { it.contains(currentQuery, ignoreCase = true) }
            val matchesTags = tags.isEmpty() || tags.all { selected ->
                app.tags.any { it.equals(selected, ignoreCase = true) }
            }
            matchesQuery && matchesTags
        }
        HomeUiState(
            apps = filtered,
            totalCount = apps.size,
            query = currentQuery,
            allTags = apps
                .flatMap { it.tags }
                .distinctBy { it.lowercase() }
                .sortedBy { it.lowercase() },
            selectedTags = tags,
            view = settings.defaultView,
            canReorder = currentQuery.isBlank() && tags.isEmpty(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun setQuery(value: String) {
        query.value = value
    }

    fun toggleTag(tag: String) {
        selectedTags.update { current ->
            if (current.any { it.equals(tag, ignoreCase = true) }) {
                current.filterNot { it.equals(tag, ignoreCase = true) }.toSet()
            } else {
                current + tag
            }
        }
    }

    fun setView(view: HomeView) {
        viewModelScope.launch { settingsRepository.setDefaultView(view) }
    }

    fun toggleView() {
        setView(if (uiState.value.view == HomeView.GRID) HomeView.LIST else HomeView.GRID)
    }

    fun setPinned(app: WebApp, pinned: Boolean) {
        viewModelScope.launch { repository.setPinned(app.id, pinned) }
    }

    fun delete(app: WebApp) {
        viewModelScope.launch { repository.delete(app.id) }
    }

    fun moveUnpinned(from: Int, to: Int) {
        viewModelScope.launch { repository.moveUnpinned(from, to) }
    }

    fun markOpened(app: WebApp) {
        viewModelScope.launch { repository.markOpened(app.id) }
    }
}
