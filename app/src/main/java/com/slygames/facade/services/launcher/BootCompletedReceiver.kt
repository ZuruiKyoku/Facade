package com.slygames.facade.services.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.slygames.facade.data.repository.AppRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Warms [AppRepository]'s cache right after boot so the first time the user
 * actually opens Facade (as HOME, immediately after unlock) the app drawer
 * and workspace aren't waiting on a cold `queryIntentActivities()` call.
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject lateinit var appRepository: AppRepository

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        receiverScope.launch {
            try {
                appRepository.refresh()
            } catch (e: Exception) {
                Log.e(TAG, "Boot-time app cache warm-up failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "BootCompletedReceiver"
    }
}
