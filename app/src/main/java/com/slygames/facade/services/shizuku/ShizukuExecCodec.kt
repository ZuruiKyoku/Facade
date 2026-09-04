package com.slygames.facade.services.shizuku

/**
 * Packs/unpacks [ShizukuCommandResult] across the [IUserService] AIDL boundary, which only
 * supports returning a single String from [IUserService.exec]. [UserService] encodes,
 * [ShizukuManager] decodes.
 */
internal object ShizukuExecCodec {
    // U+001F (unit separator) - control character with no legitimate place in shell
    // stdout/stderr, used to pack three fields into IUserService.exec's single String return.
    private const val UNIT_SEPARATOR = ''

    fun encode(exitCode: Int, stdout: String, stderr: String): String =
        "$exitCode$UNIT_SEPARATOR$stdout$UNIT_SEPARATOR$stderr"

    fun decode(encoded: String): ShizukuCommandResult {
        val parts = encoded.split(UNIT_SEPARATOR, limit = 3)
        return ShizukuCommandResult(
            exitCode = parts.getOrNull(0)?.toIntOrNull() ?: -1,
            stdout = parts.getOrNull(1).orEmpty(),
            stderr = parts.getOrNull(2).orEmpty()
        )
    }
}
