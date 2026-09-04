package com.slygames.facade.data.repository

import android.content.Context
import android.content.pm.PackageManager
import com.slygames.facade.core.util.currentUserHandle
import com.slygames.facade.core.util.isSystemApp
import com.slygames.facade.core.util.loadSafeIcon
import com.slygames.facade.core.util.loadSafeLabel
import com.slygames.facade.core.util.queryLauncherActivities
import com.slygames.facade.data.model.AppItem
import com.slygames.facade.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Source of truth for "every app installed for the current user", queried
 * via [PackageManager.queryIntentActivities] against `ACTION_MAIN` /
 * `CATEGORY_LAUNCHER`. [com.slygames.facade.services.launcher.PackageChangeReceiver]
 * calls [refresh] whenever a package is added, removed, or changed so the
 * app drawer and workspace stay live without a restart.
 */
@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val packageManager: PackageManager get() = context.packageManager
    private val refreshMutex = Mutex()

    private val _apps = MutableStateFlow<List<AppItem>>(emptyList())
    val apps: StateFlow<List<AppItem>> = _apps.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    suspend fun refresh() = withContext(ioDispatcher) {
        refreshMutex.withLock {
            _isLoading.value = true
            try {
                val resolvedActivities = packageManager.queryLauncherActivities()
                val items = resolvedActivities
                    .asSequence()
                    .map { resolveInfo ->
                        AppItem(
                            packageName = resolveInfo.activityInfo.packageName,
                            activityName = resolveInfo.activityInfo.name,
                            label = resolveInfo.loadSafeLabel(packageManager),
                            icon = resolveInfo.loadSafeIcon(packageManager),
                            userHandle = currentUserHandle(),
                            isSystemApp = resolveInfo.activityInfo.applicationInfo.isSystemApp()
                        )
                    }
                    .distinctBy { it.componentKey }
                    .sortedBy { it.label.lowercase() }
                    .toList()
                _apps.value = items
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getByComponentKey(componentKey: String): AppItem? =
        _apps.value.firstOrNull { it.componentKey == componentKey }

    fun getByPackageName(packageName: String): List<AppItem> =
        _apps.value.filter { it.packageName == packageName }

    /** Removes every entry for [packageName] from the in-memory cache immediately (before the next [refresh] confirms it). */
    fun evictPackage(packageName: String) {
        _apps.value = _apps.value.filterNot { it.packageName == packageName }
    }
}
