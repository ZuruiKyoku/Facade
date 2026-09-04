package com.slygames.facade.core.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle

/** Small helpers layered over [PackageManager] used by [com.slygames.facade.data.repository.AppRepository]. */

/** True if this app was pre-installed as part of the system image (no launcher entry unless updated). */
fun ApplicationInfo.isSystemApp(): Boolean =
    (flags and ApplicationInfo.FLAG_SYSTEM) != 0

/** True if this system app has since received a user-facing update (so it likely deserves a drawer entry). */
fun ApplicationInfo.isUpdatedSystemApp(): Boolean =
    (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

/** Loads this activity's label, falling back to the declaring application's label, then the raw package name. */
fun ResolveInfo.loadSafeLabel(packageManager: PackageManager): String = try {
    loadLabel(packageManager)?.toString()
        ?.takeIf { it.isNotBlank() }
        ?: activityInfo.applicationInfo.loadLabel(packageManager).toString()
} catch (_: Exception) {
    activityInfo?.packageName.orEmpty()
}

/** Loads this activity's icon, returning null (never throwing) if the package was uninstalled mid-query. */
fun ResolveInfo.loadSafeIcon(packageManager: PackageManager): Drawable? = try {
    loadIcon(packageManager)
} catch (_: Exception) {
    null
}

/** Stable identity key for a launchable activity, used as the primary key surrogate in [com.slygames.facade.data.model.AppItem]. */
fun ResolveInfo.componentKey(): String =
    "${activityInfo.packageName}/${activityInfo.name}"

/**
 * The user profile (personal vs. work profile) this app entry belongs to,
 * needed for [android.os.UserManager]-aware launch/uninstall flows on
 * managed devices. Facade currently only surfaces the primary user's apps.
 */
fun currentUserHandle(): UserHandle = Process.myUserHandle()

/** Convenience overload that resolves every activity exposing a LAUNCHER category entry point. */
fun PackageManager.queryLauncherActivities(): List<ResolveInfo> {
    val intent = android.content.Intent(android.content.Intent.ACTION_MAIN)
        .addCategory(android.content.Intent.CATEGORY_LAUNCHER)
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
    } else {
        @Suppress("DEPRECATION")
        queryIntentActivities(intent, PackageManager.MATCH_ALL)
    }
}

/** True when [Context.getPackageName] resolves to a package still installed for the current user. */
fun Context.isPackageInstalled(packageName: String): Boolean = try {
    packageManager.getApplicationInfo(packageName, 0)
    true
} catch (_: PackageManager.NameNotFoundException) {
    false
}
