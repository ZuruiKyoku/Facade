package com.slygames.facade.data.local.datastore

/** A single physical/soft gesture surfaced in Settings > Desktop > Gestures. */
enum class GestureTrigger {
    SWIPE_UP,
    SWIPE_DOWN,
    DOUBLE_TAP,
    PINCH_IN,
    HOME_BUTTON_DOUBLE_PRESS
}

/** What a [GestureTrigger] resolves to. [LAUNCH_APP] additionally reads a stored component key. */
enum class GestureAction {
    NONE,
    OPEN_APP_DRAWER,
    OPEN_NOTIFICATION_SHADE,
    OPEN_QUICK_SETTINGS,
    LOCK_SCREEN,
    OPEN_SEARCH,
    LAUNCH_APP
}

data class GestureMapping(
    val trigger: GestureTrigger,
    val action: GestureAction,
    /** "pkg/activity" component key, only meaningful when [action] is [GestureAction.LAUNCH_APP]. */
    val targetComponentKey: String? = null
)

/**
 * Snapshot of every user-configurable launcher setting, persisted via
 * [LauncherPreferencesRepository] on top of Jetpack DataStore. Kept as one
 * immutable value class so the whole workspace/dock/drawer can collect a
 * single [kotlinx.coroutines.flow.Flow] instead of many scattered keys.
 */
data class LauncherPreferences(
    val gridColumns: Int = DEFAULT_GRID_COLUMNS,
    val gridRows: Int = DEFAULT_GRID_ROWS,
    val dockSlotCount: Int = DEFAULT_DOCK_SLOTS,
    val iconScale: Float = 1.0f,
    val showIconLabels: Boolean = true,
    val dynamicColorEnabled: Boolean = true,
    val infiniteScrollEnabled: Boolean = false,
    val activeIconPackPackage: String? = null,
    val gestureMappings: List<GestureMapping> = defaultGestureMappings(),
    val overlayStatusBarEnabled: Boolean = false,
    val overlayVolumeHudEnabled: Boolean = false,
    val overlayFloatingHudEnabled: Boolean = false
) {
    companion object {
        const val DEFAULT_GRID_COLUMNS = 5
        const val DEFAULT_GRID_ROWS = 6
        const val DEFAULT_DOCK_SLOTS = 5

        fun defaultGestureMappings(): List<GestureMapping> = listOf(
            GestureMapping(GestureTrigger.SWIPE_UP, GestureAction.OPEN_APP_DRAWER),
            GestureMapping(GestureTrigger.SWIPE_DOWN, GestureAction.OPEN_NOTIFICATION_SHADE),
            GestureMapping(GestureTrigger.DOUBLE_TAP, GestureAction.LOCK_SCREEN),
            GestureMapping(GestureTrigger.PINCH_IN, GestureAction.NONE),
            GestureMapping(GestureTrigger.HOME_BUTTON_DOUBLE_PRESS, GestureAction.NONE)
        )
    }
}
