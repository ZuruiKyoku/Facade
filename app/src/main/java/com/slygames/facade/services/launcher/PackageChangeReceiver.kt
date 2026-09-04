package com.slygames.facade.services.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.slygames.facade.data.repository.AppRepository
import com.slygames.facade.data.repository.WorkspaceRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Dynamic package monitoring: keeps [AppRepository] (and, on uninstall, the
 * persisted workspace layout) in sync with `PACKAGE_ADDED` / `_REMOVED` /
 * `_REPLACED` / `_CHANGED` without requiring Facade to be running in the
 * foreground. Uses [goAsync] since a `BroadcastReceiver`'s process is
 * eligible for the system to kill the instant [onReceive] returns - without
 * it, `appRepository.refresh()`'s coroutine could be torn down mid-query.
 */
@AndroidEntryPoint
class PackageChangeReceiver : BroadcastReceiver() {

    @Inject lateinit var appRepository: AppRepository
    @Inject lateinit var workspaceRepository: WorkspaceRepository

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart
        if (packageName == null) {
            Log.w(TAG, "Received ${intent.action} with no package data")
            return
        }

        val pendingResult = goAsync()
        receiverScope.launch {
            try {
                when (intent.action) {
                    Intent.ACTION_PACKAGE_REMOVED -> {
                        val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                        if (!replacing) {
                            appRepository.evictPackage(packageName)
                            workspaceRepository.removeAllForPackage(packageName)
                        }
                        appRepository.refresh()
                    }
                    Intent.ACTION_PACKAGE_ADDED,
                    Intent.ACTION_PACKAGE_REPLACED,
                    Intent.ACTION_PACKAGE_CHANGED -> appRepository.refresh()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed handling ${intent.action} for $packageName", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "PackageChangeReceiver"
    }
}
