package com.slygames.facade.services.shizuku

import com.slygames.facade.core.permission.PermissionState
import com.slygames.facade.core.permission.ShizukuPermissionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton

enum class ShizukuConnectionState {
    /** The Shizuku manager app isn't installed, or its service was never started. */
    UNAVAILABLE,
    /** The daemon is alive but Facade hasn't been granted the Shizuku permission yet. */
    PERMISSION_REQUIRED,
    /** Ready to execute elevated commands. */
    READY
}

data class ShizukuCommandResult(
    val exitCode: Int,
    val stdout: String = "",
    val stderr: String = ""
) {
    val isSuccess: Boolean get() = exitCode == 0
}

/**
 * Thread-safe façade over the Shizuku client API: tracks daemon
 * connection/permission state via [Shizuku]'s sticky listeners and exposes
 * it as a hot [StateFlow], plus a permission request entry point and a
 * command execution surface for the Elevated System Tweak Bridge (UI Tuner
 * toggles, `WRITE_SECURE_SETTINGS`-gated settings, animation scales).
 *
 * Command execution against the elevated shell requires binding a Shizuku
 * *user service* (a small AIDL-backed helper process Shizuku launches with
 * shell/ADB privileges) rather than calling into the daemon directly -
 * [executeCommand] is therefore a documented stub other modules can build
 * against; wiring a concrete `IShizukuUserService` implementation is the
 * next step once a specific tweak (e.g. `settings put global
 * window_animation_scale`) is being wired end-to-end.
 */
@Singleton
class ShizukuManager @Inject constructor(
    private val permissionHandler: ShizukuPermissionHandler
) {
    private val _connectionState = MutableStateFlow(ShizukuConnectionState.UNAVAILABLE)
    val connectionState: StateFlow<ShizukuConnectionState> = _connectionState.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener { refreshState() }
    private val binderDeadListener = Shizuku.OnBinderDeadListener { refreshState() }
    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { _, _ -> refreshState() }

    init {
        // Sticky: fires immediately with the current state if the binder is already alive,
        // in addition to on every future (re)connect - covers Facade being launched after
        // the Shizuku service was already running.
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
        refreshState()
    }

    fun refreshState() {
        _connectionState.value = when (permissionHandler.currentState()) {
            PermissionState.UNAVAILABLE -> ShizukuConnectionState.UNAVAILABLE
            PermissionState.DENIED -> ShizukuConnectionState.PERMISSION_REQUIRED
            PermissionState.GRANTED -> ShizukuConnectionState.READY
        }
    }

    fun requestPermission(requestCode: Int = DEFAULT_PERMISSION_REQUEST_CODE) {
        permissionHandler.requestPermission(requestCode)
    }

    /**
     * Executes [command] with Shizuku's elevated privileges once a user
     * service is bound. Until that binding is wired up, this reports a
     * clear "not yet implemented" failure instead of silently no-op'ing, so
     * callers building system-tweak UI on top of it fail loudly in debug
     * builds rather than believing a toggle took effect.
     */
    suspend fun executeCommand(command: String): ShizukuCommandResult {
        if (_connectionState.value != ShizukuConnectionState.READY) {
            return ShizukuCommandResult(
                exitCode = ERROR_NOT_READY,
                stderr = "Shizuku is not connected (state=${_connectionState.value})"
            )
        }
        // TODO: bind an IShizukuUserService (Shizuku.bindUserService) that shells out to
        // `command` in a privileged process and relay its stdout/stderr/exit code back here.
        return ShizukuCommandResult(
            exitCode = ERROR_NOT_IMPLEMENTED,
            stderr = "Elevated command execution requires a bound Shizuku user service (not yet wired)."
        )
    }

    fun dispose() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
    }

    companion object {
        const val DEFAULT_PERMISSION_REQUEST_CODE = 5721
        private const val ERROR_NOT_READY = -1
        private const val ERROR_NOT_IMPLEMENTED = -2
    }
}
