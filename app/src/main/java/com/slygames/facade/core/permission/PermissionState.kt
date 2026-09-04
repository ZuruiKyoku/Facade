package com.slygames.facade.core.permission

/** Common result shape returned by every permission handler in this package. */
enum class PermissionState {
    GRANTED,
    DENIED,
    /** The underlying capability (e.g. Shizuku) isn't installed/running at all. */
    UNAVAILABLE
}

/** A permission Facade needs, surfaced together in the onboarding flow and Settings. */
enum class FacadePermission {
    SYSTEM_ALERT_WINDOW,
    ACCESSIBILITY_SERVICE,
    SHIZUKU
}
