package com.slygames.facade.features.overlays

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slygames.facade.core.permission.AccessibilityPermissionHandler
import com.slygames.facade.core.permission.OverlayPermissionHandler
import com.slygames.facade.core.permission.PermissionState
import com.slygames.facade.data.local.datastore.AppPreferences
import com.slygames.facade.data.local.datastore.AppPreferencesRepository
import com.slygames.facade.data.local.datastore.BatteryIconStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OverlaySettingsUiState(
    val preferences: AppPreferences = AppPreferences(),
    val overlayPermissionState: PermissionState = PermissionState.DENIED,
    val accessibilityServiceState: PermissionState = PermissionState.DENIED
) {
    /** Every HUD toggle requires both `SYSTEM_ALERT_WINDOW` and the accessibility service to actually render. */
    val prerequisitesMet: Boolean
        get() = overlayPermissionState == PermissionState.GRANTED && accessibilityServiceState == PermissionState.GRANTED
}

@HiltViewModel
class OverlaySettingsViewModel @Inject constructor(
    private val preferencesRepository: AppPreferencesRepository,
    private val overlayPermissionHandler: OverlayPermissionHandler,
    private val accessibilityPermissionHandler: AccessibilityPermissionHandler
) : ViewModel() {

    private val _permissionTick = MutableStateFlow(0)

    val uiState: StateFlow<OverlaySettingsUiState> = combine(
        preferencesRepository.preferencesFlow,
        _permissionTick
    ) { prefs, _ ->
        OverlaySettingsUiState(
            preferences = prefs,
            overlayPermissionState = overlayPermissionHandler.currentState(),
            accessibilityServiceState = accessibilityPermissionHandler.currentState()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OverlaySettingsUiState())

    fun refreshPermissions() {
        _permissionTick.value += 1
    }

    fun buildOverlayPermissionIntent() = overlayPermissionHandler.buildRequestIntent()
    fun buildAccessibilitySettingsIntent() = accessibilityPermissionHandler.buildRequestIntent()

    fun setStatusBarOverlayEnabled(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.setOverlayStatusBarEnabled(enabled)
    }

    fun setVolumeHudEnabled(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.setOverlayVolumeHudEnabled(enabled)
    }

    fun setStatusBarShowClock(show: Boolean) = viewModelScope.launch {
        preferencesRepository.setStatusBarShowClock(show)
    }

    fun setStatusBarShowBattery(show: Boolean) = viewModelScope.launch {
        preferencesRepository.setStatusBarShowBattery(show)
    }

    fun setStatusBarShowWifi(show: Boolean) = viewModelScope.launch {
        preferencesRepository.setStatusBarShowWifi(show)
    }

    fun setStatusBarUse24HourClock(use24h: Boolean) = viewModelScope.launch {
        preferencesRepository.setStatusBarUse24HourClock(use24h)
    }

    fun setStatusBarBatteryStyle(style: BatteryIconStyle) = viewModelScope.launch {
        preferencesRepository.setStatusBarBatteryStyle(style)
    }

    fun setOverlayAccentColor(argb: Int?) = viewModelScope.launch {
        preferencesRepository.setOverlayAccentColor(argb)
    }
}
