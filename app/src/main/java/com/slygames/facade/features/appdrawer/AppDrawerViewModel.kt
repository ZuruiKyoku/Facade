package com.slygames.facade.features.appdrawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slygames.facade.data.local.datastore.LauncherPreferencesRepository
import com.slygames.facade.data.model.AppItem
import com.slygames.facade.data.repository.AppRepository
import com.slygames.facade.data.repository.IconPackRepository
import com.slygames.facade.data.repository.WorkspaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppDrawerUiState(
    val query: String = "",
    val allApps: List<AppItem> = emptyList(),
    val isLoading: Boolean = false,
    val iconScale: Float = 1f
) {
    /** Apps filtered by [query] against label, then grouped by the first uppercase letter for fast-scroll sectioning. */
    val filteredApps: List<AppItem> by lazy {
        if (query.isBlank()) allApps
        else allApps.filter { it.label.contains(query, ignoreCase = true) }
    }

    val sectionIndex: List<Char> by lazy {
        filteredApps.map { it.label.firstOrNull()?.uppercaseChar() ?: '#' }.distinct()
    }

    /** First item position (within [filteredApps]) for each letter in [sectionIndex] - lets the fast-scroll rail jump straight to a section instead of just labeling one. */
    val sectionStartIndex: Map<Char, Int> by lazy {
        val positions = LinkedHashMap<Char, Int>()
        filteredApps.forEachIndexed { index, app ->
            val letter = app.label.firstOrNull()?.uppercaseChar() ?: '#'
            positions.getOrPut(letter) { index }
        }
        positions
    }
}

@HiltViewModel
class AppDrawerViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val iconPackRepository: IconPackRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val preferencesRepository: LauncherPreferencesRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")

    val uiState: StateFlow<AppDrawerUiState> = combine(
        appRepository.apps,
        appRepository.isLoading,
        _query,
        preferencesRepository.preferencesFlow
    ) { apps, loading, query, prefs ->
        AppDrawerUiState(
            query = query,
            allApps = applyIconPackOverrides(apps),
            isLoading = loading,
            iconScale = prefs.appDrawerIconScale
        )
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

    /**
     * Drag-out-of-the-drawer add: the workspace grid isn't even composed while the drawer is
     * showing (they're separate NavHost destinations, not an overlay over a live workspace), so
     * there's no view to literally drop onto. Placing directly through the repository sidesteps
     * that entirely - it's a plain data write, and the workspace picks it up reactively via its
     * own Flow the next time it's actually on screen. Always targets page 0, same simplification
     * placeAppOnFirstFreeCell's callers already made.
     */
    fun addAppToHomeScreen(app: AppItem) {
        viewModelScope.launch {
            val prefs = preferencesRepository.preferencesFlow.first()
            workspaceRepository.placeAppOnFirstFreeCell(app, page = 0, prefs.gridColumns, prefs.gridRows)
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
