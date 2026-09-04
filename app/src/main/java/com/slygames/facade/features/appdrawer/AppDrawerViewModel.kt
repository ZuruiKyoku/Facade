package com.slygames.facade.features.appdrawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slygames.facade.data.model.AppItem
import com.slygames.facade.data.repository.AppRepository
import com.slygames.facade.data.repository.IconPackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppDrawerUiState(
    val query: String = "",
    val allApps: List<AppItem> = emptyList(),
    val isLoading: Boolean = false
) {
    /** Apps filtered by [query] against label, then grouped by the first uppercase letter for fast-scroll sectioning. */
    val filteredApps: List<AppItem> by lazy {
        if (query.isBlank()) allApps
        else allApps.filter { it.label.contains(query, ignoreCase = true) }
    }

    val sectionIndex: List<Char> by lazy {
        filteredApps.map { it.label.firstOrNull()?.uppercaseChar() ?: '#' }.distinct()
    }
}

@HiltViewModel
class AppDrawerViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val iconPackRepository: IconPackRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")

    val uiState: StateFlow<AppDrawerUiState> = combine(
        appRepository.apps,
        appRepository.isLoading,
        _query
    ) { apps, loading, query ->
        AppDrawerUiState(query = query, allApps = applyIconPackOverrides(apps), isLoading = loading)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppDrawerUiState())

    init {
        viewModelScope.launch { appRepository.refresh() }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun onIconPackSelected(packageName: String?) {
        viewModelScope.launch {
            iconPackRepository.setActivePack(packageName)
            appRepository.refresh()
        }
    }

    private fun applyIconPackOverrides(apps: List<AppItem>): List<AppItem> {
        if (iconPackRepository.activePack.value == null) return apps
        return apps.map { app ->
            val themedIcon = iconPackRepository.resolveIconDrawable(app.componentKey)
            if (themedIcon != null) app.withIcon(themedIcon, custom = true) else app
        }
    }
}
