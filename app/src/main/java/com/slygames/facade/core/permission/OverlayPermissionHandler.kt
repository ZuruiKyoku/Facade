package com.slygames.facade.core.permission

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps `SYSTEM_ALERT_WINDOW` ("display over other apps"), required by the
 * Surface Overlay Engine to host [android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY]
 * windows (floating HUDs, custom status bar, volume controls).
 */
@Singleton
class OverlayPermissionHandler @Inject constructor(
    private val context: Context
) {

    fun currentState(): PermissionState =
        if (Settings.canDrawOverlays(context)) PermissionState.GRANTED else PermissionState.DENIED

    fun buildRequestIntent(): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
}
