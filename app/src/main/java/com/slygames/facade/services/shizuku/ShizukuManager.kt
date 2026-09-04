package com.slygames.facade.services.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import com.slygames.facade.BuildConfig
import com.slygames.facade.core.permission.PermissionState
import com.slygames.facade.core.permission.ShizukuPermissionHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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
 * [executeCommand] binds a Shizuku *user service* ([UserService], hosted by
 * [IUserService]) - a small AIDL-backed helper process Shizuku launches
 * with shell/ADB privileges - the first time it's needed, and reuses that
 * binding for subsequent commands until [dispose] tears it down.
 */
@Singleton
class ShizukuManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionHandler: ShizukuPermissionHandler
) {
    private val _connectionState = MutableStateFlow(ShizukuConnectionState.UNAVAILABLE)
    val connectionState: StateFlow<ShizukuConnectionState> = _connectionState.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener { refreshState() }
    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        userService = null
        refreshState()
    }
    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { _, _ -> refreshState() }

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(context.packageName, UserService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("shizuku_tweaks")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    private val bindLock = Mutex()
    private var userService: IUserService? = null
    private var pendingBind: CompletableDeferred<IUserService?>? = null

    private val userServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val service = if (binder.isBinderAlive) IUserService.Stub.asInterface(binder) else null
            userService = service
            pendingBind?.complete(service)
            pendingBind = null
        }

        override fun onServiceDisconnected(name: ComponentName) {
            userService = null
        }
    }

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
     * Executes [command] with Shizuku's elevated privileges, binding
     * [UserService] on first use. Fails loudly (a non-zero [ShizukuCommandResult.exitCode]
     * with a [ShizukuCommandResult.stderr] explanation) rather than silently no-op'ing if
     * Shizuku isn't connected or the bind times out, so callers can't mistake a no-op for
     * a toggle having taken effect.
     */
    suspend fun executeCommand(command: String): ShizukuCommandResult {
        if (_connectionState.value != ShizukuConnectionState.READY) {
            return ShizukuCommandResult(
                exitCode = ERROR_NOT_READY,
                stderr = "Shizuku is not connected (state=${_connectionState.value})"
            )
        }
        val service = obtainUserService() ?: return ShizukuCommandResult(
            exitCode = ERROR_BIND_FAILED,
            stderr = "Timed out binding the Shizuku user service."
        )
        return try {
            withContext(Dispatchers.IO) { ShizukuExecCodec.decode(service.exec(command)) }
        } catch (e: RemoteException) {
            userService = null
            ShizukuCommandResult(
                exitCode = ERROR_REMOTE_EXCEPTION,
                stderr = e.message ?: "The Shizuku user service died mid-call."
            )
        }
    }

    private suspend fun obtainUserService(): IUserService? {
        userService?.let { return it }
        return bindLock.withLock {
            userService?.let { return@withLock it }
            val deferred = CompletableDeferred<IUserService?>()
            pendingBind = deferred
            try {
                Shizuku.bindUserService(userServiceArgs, userServiceConnection)
            } catch (_: Throwable) {
                pendingBind = null
                return@withLock null
            }
            withTimeoutOrNull(BIND_TIMEOUT_MS) { deferred.await() }
        }
    }

    fun dispose() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        if (userService != null) {
            try {
                Shizuku.unbindUserService(userServiceArgs, userServiceConnection, true)
            } catch (_: Throwable) {
                // Best-effort: the daemon may already be gone.
            }
        }
        userService = null
    }

    companion object {
        const val DEFAULT_PERMISSION_REQUEST_CODE = 5721
        private const val BIND_TIMEOUT_MS = 5_000L
        private const val ERROR_NOT_READY = -1
        private const val ERROR_BIND_FAILED = -2
        private const val ERROR_REMOTE_EXCEPTION = -4
    }
}
