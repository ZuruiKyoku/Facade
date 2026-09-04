package com.slygames.facade.core.permission

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import com.slygames.facade.services.overlay.FacadeAccessibilityService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Checks whether [FacadeAccessibilityService] is enabled via the
 * `enabled_accessibility_services` secure setting, since there is no direct
 * "isEnabled" API for a specific service.
 */
@Singleton
class AccessibilityPermissionHandler @Inject constructor(
    private val context: Context
) {

    fun currentState(): PermissionState =
        if (isServiceEnabled()) PermissionState.GRANTED else PermissionState.DENIED

    fun isServiceEnabled(): Boolean {
        val expectedComponent = "${context.packageName}/${FacadeAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        if (TextUtils.isEmpty(enabledServices)) return false

        return enabledServices.split(':').any { it.equals(expectedComponent, ignoreCase = true) }
    }

    fun buildRequestIntent(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
}
