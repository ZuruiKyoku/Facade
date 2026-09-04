package com.slygames.facade.core.util

import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Central place for every implicit/explicit [Intent] Facade fires from the
 * workspace, dock, or app drawer: launching apps, opening app details,
 * requesting an uninstall, or deep-linking into system settings. Every
 * launch is defensive - a third-party icon disappearing mid-drag or a
 * package being uninstalled out from under a tap should never crash the
 * launcher process.
 */
object IntentDispatcher {

    private const val TAG = "IntentDispatcher"

    /**
     * Launches an app's main activity, optionally animating outward from
     * [sourceBounds] (the tapped icon's on-screen rect) to match platform
     * launcher conventions.
     */
    fun launchApp(
        context: Context,
        packageName: String,
        className: String?,
        sourceBounds: Rect? = null
    ): Boolean {
        val intent = if (!className.isNullOrEmpty()) {
            Intent(Intent.ACTION_MAIN).apply {
                setClassName(packageName, className)
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
        } else {
            context.packageManager.getLaunchIntentForPackage(packageName)
        }

        if (intent == null) {
            Log.w(TAG, "No launch intent for $packageName/$className")
            return false
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        sourceBounds?.let { intent.sourceBounds = it }

        return try {
            val options = sourceBounds?.let {
                ActivityOptions.makeClipRevealAnimation(
                    /* source = */ null,
                    it.left, it.top, it.width(), it.height()
                ).toBundle()
            }
            context.startActivity(intent, options)
            true
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Activity not found for $packageName/$className", e)
            false
        } catch (e: SecurityException) {
            Log.w(TAG, "Not permitted to launch $packageName/$className", e)
            false
        }
    }

    /** Opens the platform "App info" screen for [packageName]. */
    fun openAppInfo(context: Context, packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        safeStart(context, intent)
    }

    /** Requests uninstall of [packageName] via the system confirmation dialog. */
    fun requestUninstall(context: Context, packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        safeStart(context, intent)
    }

    /** Opens the platform app widget picker/config activity for a pending bind, if declared. */
    fun launchWidgetConfigActivity(context: Context, componentPackage: String, componentClass: String): Boolean {
        val intent = Intent().apply {
            setClassName(componentPackage, componentClass)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return safeStart(context, intent)
    }

    private fun safeStart(context: Context, intent: Intent, options: Bundle? = null): Boolean = try {
        ContextCompat.startActivity(context, intent, options)
        true
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "No activity to handle $intent", e)
        false
    }
}
