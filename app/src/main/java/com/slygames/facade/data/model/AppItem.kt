package com.slygames.facade.data.model

import android.graphics.drawable.Drawable
import android.os.UserHandle

/**
 * A single launchable activity as surfaced by [android.content.pm.PackageManager],
 * used by both the app drawer (Compose) and as the source data for a
 * workspace/dock [WorkspaceItem.App] once placed on the grid.
 *
 * Equality/hashing is intentionally keyed on [componentKey] + [userHandle]
 * rather than the full field set (in particular [icon], which is a mutable
 * platform [Drawable] with identity semantics) so this type behaves
 * predictably as a Compose `key` and in list diffing.
 */
class AppItem(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: Drawable?,
    val userHandle: UserHandle,
    val isSystemApp: Boolean = false,
    /** True once a third-party icon pack override has replaced [icon]. */
    val hasCustomIcon: Boolean = false
) {
    /** Stable identity for this activity, independent of user profile. */
    val componentKey: String get() = "$packageName/$activityName"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppItem) return false
        return componentKey == other.componentKey && userHandle == other.userHandle
    }

    override fun hashCode(): Int = 31 * componentKey.hashCode() + userHandle.hashCode()

    override fun toString(): String = "AppItem(label=$label, component=$componentKey)"

    fun withIcon(newIcon: Drawable?, custom: Boolean): AppItem =
        AppItem(packageName, activityName, label, newIcon, userHandle, isSystemApp, custom)
}
