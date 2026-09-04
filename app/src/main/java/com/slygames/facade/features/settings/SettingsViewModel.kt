package com.slygames.facade.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slygames.facade.data.local.datastore.LauncherPreferences
import com.slygames.facade.data.local.datastore.LauncherPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: LauncherPreferencesRepository
) : ViewModel() {

    val preferences: StateFlow<LauncherPreferences> = preferencesRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LauncherPreferences())

    fun setGridSize(columns: Int, rows: Int) = viewModelScope.launch {
        preferencesRepository.setGridSize(columns, rows)
    }

    fun setDockSlotCount(count: Int) = viewModelScope.launch {
        preferencesRepository.setDockSlotCount(count)
    }

    fun setIconScale(scale: Float) = viewModelScope.launch {
        preferencesRepository.setIconScale(scale)
    }

    fun setShowIconLabels(show: Boolean) = viewModelScope.launch {
        preferencesRepository.setShowIconLabels(show)
    }

    fun setDynamicColorEnabled(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.setDynamicColorEnabled(enabled)
    }

    fun setInfiniteScrollEnabled(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.setInfiniteScrollEnabled(enabled)
    }
}
