package com.slygames.facade.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slygames.facade.data.local.datastore.AppPreferences
import com.slygames.facade.data.local.datastore.AppPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: AppPreferencesRepository
) : ViewModel() {

    val preferences: StateFlow<AppPreferences> = preferencesRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPreferences())

    fun setDynamicColorEnabled(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.setDynamicColorEnabled(enabled)
    }
}
