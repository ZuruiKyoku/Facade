package com.slygames.facade.core.permission

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Determines whether Facade is the currently active HOME app and builds the
 * right intent to prompt the user to change it - via [RoleManager] on
 * Android 10+, falling back to the legacy "Home app" settings screen below
 * that.
 */
@Singleton
class DefaultLauncherPermissionHandler @Inject constructor(
    private val context: Context
) {

    fun currentState(): PermissionState =
        if (isDefaultLauncher()) PermissionState.GRANTED else PermissionState.DENIED

    fun isDefaultLauncher(): Boolean {
        val resolveInfo = context.packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
        )
        return resolveInfo?.activityInfo?.packageName == context.packageName
    }

    /** Intent that surfaces the system's "become default launcher" UI. */
    fun buildRequestIntent(): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = ContextCompat.getSystemService(context, RoleManager::class.java)
            if (roleManager != null &&
                roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            ) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
            }
        }
        return Intent(Settings.ACTION_HOME_SETTINGS)
    }
}
