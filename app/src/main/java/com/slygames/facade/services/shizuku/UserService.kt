package com.slygames.facade.services.shizuku

/**
 * Runs inside the separate process Shizuku spawns with the daemon's (shell/ADB) privileges when
 * [ShizukuManager] calls `Shizuku.bindUserService` - Shizuku instantiates this class by
 * reflection over there via its public no-arg constructor, entirely outside Facade's normal app
 * process/lifecycle, so it must not depend on anything from the main process (Application,
 * Context, Hilt graph, etc).
 */
class UserService : IUserService.Stub() {

    override fun exec(command: String): String = try {
        val process = ProcessBuilder("sh", "-c", command).start()
        val stdout = process.inputStream.bufferedReader().use { it.readText() }
        val stderr = process.errorStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()
        ShizukuExecCodec.encode(exitCode, stdout, stderr)
    } catch (t: Throwable) {
        ShizukuExecCodec.encode(EXEC_FAILED, "", t.message ?: t.toString())
    }

    private companion object {
        const val EXEC_FAILED = -3
    }
}
