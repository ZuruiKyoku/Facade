package com.slygames.facade.features.systemtweaks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slygames.facade.services.shizuku.ShizukuCommandResult
import com.slygames.facade.services.shizuku.ShizukuConnectionState
import com.slygames.facade.services.shizuku.ShizukuManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AnimationScale(val settingValue: String, val label: String) {
    OFF("0", "Off"),
    NORMAL("1", "Normal (1x)"),
    FAST("0.5", "Fast (0.5x)")
}

data class SystemTweaksUiState(
    val connectionState: ShizukuConnectionState = ShizukuConnectionState.UNAVAILABLE,
    val lastCommandResult: ShizukuCommandResult? = null
)

@HiltViewModel
class SystemTweaksViewModel @Inject constructor(
    private val shizukuManager: ShizukuManager
) : ViewModel() {

    private val _lastResult = MutableStateFlow<ShizukuCommandResult?>(null)

    val uiState: StateFlow<SystemTweaksUiState> = combine(
        shizukuManager.connectionState,
        _lastResult
    ) { connection, result ->
        SystemTweaksUiState(connection, result)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SystemTweaksUiState())

    fun refreshConnection() = shizukuManager.refreshState()

    fun requestPermission() = shizukuManager.requestPermission()

    fun setAnimationScale(scale: AnimationScale) {
        viewModelScope.launch {
            val results = listOf(
                "window_animation_scale",
                "transition_animation_scale",
                "animator_duration_scale"
            ).map { setting ->
                shizukuManager.executeCommand("settings put global $setting ${scale.settingValue}")
            }
            _lastResult.value = results.lastOrNull()
        }
    }

    fun toggleSystemUiTuner(enabled: Boolean) {
        viewModelScope.launch {
            val value = if (enabled) "1" else "0"
            _lastResult.value = shizukuManager.executeCommand("settings put secure sysui_demo_allowed $value")
        }
    }
}
