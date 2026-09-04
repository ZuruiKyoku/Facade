package com.slygames.facade.core.permission

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around the Shizuku client API used to gate the Elevated
 * System Tweak Bridge. Facade never talks to Shizuku directly outside this
 * class and [com.slygames.facade.services.shizuku.ShizukuManager], which
 * layers connection-state listeners and command execution on top of it.
 */
@Singleton
class ShizukuPermissionHandler @Inject constructor(
    private val context: Context
) {

    /** True once the Shizuku manager app/service has started and its binder is alive. */
    fun isShizukuAvailable(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Throwable) {
        false
    }

    fun currentState(): PermissionState {
        if (!isShizukuAvailable()) return PermissionState.UNAVAILABLE
        return if (hasPermission()) PermissionState.GRANTED else PermissionState.DENIED
    }

    fun hasPermission(): Boolean = try {
        if (Shizuku.isPreV11()) {
            // Pre-v11 Shizuku granted permission at process start via the manifest permission.
            context.checkSelfPermission("moe.shizuku.manager.permission.API_V23") ==
                PackageManager.PERMISSION_GRANTED
        } else {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }
    } catch (_: Throwable) {
        false
    }

    /**
     * Requests the Shizuku permission. The result arrives asynchronously via
     * [Shizuku.OnRequestPermissionResultListener], which callers register
     * with [Shizuku.addRequestPermissionResultListener] (see
     * [com.slygames.facade.services.shizuku.ShizukuManager]).
     */
    fun requestPermission(requestCode: Int) {
        if (Shizuku.isPreV11()) return
        Shizuku.requestPermission(requestCode)
    }
}
