package com.slygames.facade.features.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import com.slygames.facade.core.permission.AccessibilityPermissionHandler
import com.slygames.facade.core.permission.FacadePermission
import com.slygames.facade.core.permission.OverlayPermissionHandler
import com.slygames.facade.core.permission.PermissionState
import com.slygames.facade.core.permission.ShizukuPermissionHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Aggregates every permission Facade's modules depend on so onboarding and
 * Settings can render one consistent checklist. [refresh] is meant to be
 * called from `onResume()` of whatever hosts this screen, since most of
 * these states can only change while Facade is backgrounded (the user
 * granting them in system Settings).
 */
@HiltViewModel
class PermissionsViewModel @Inject constructor(
    private val overlayHandler: OverlayPermissionHandler,
    private val accessibilityHandler: AccessibilityPermissionHandler,
    private val shizukuHandler: ShizukuPermissionHandler
) : ViewModel() {

    private val _states = MutableStateFlow(emptyMap<FacadePermission, PermissionState>())
    val states: StateFlow<Map<FacadePermission, PermissionState>> = _states.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _states.value = mapOf(
            FacadePermission.SYSTEM_ALERT_WINDOW to overlayHandler.currentState(),
            FacadePermission.ACCESSIBILITY_SERVICE to accessibilityHandler.currentState(),
            FacadePermission.SHIZUKU to shizukuHandler.currentState()
        )
    }

    fun buildRequestIntent(permission: FacadePermission): Intent? = when (permission) {
        FacadePermission.SYSTEM_ALERT_WINDOW -> overlayHandler.buildRequestIntent()
        FacadePermission.ACCESSIBILITY_SERVICE -> accessibilityHandler.buildRequestIntent()
        FacadePermission.SHIZUKU -> null // handled via requestShizukuPermission(); Shizuku isn't a Settings deep link.
    }

    fun requestShizukuPermission(requestCode: Int) {
        shizukuHandler.requestPermission(requestCode)
    }
}
