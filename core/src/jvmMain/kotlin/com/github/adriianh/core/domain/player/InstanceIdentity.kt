package com.github.adriianh.core.domain.player

import com.github.adriianh.core.platform.MeloDataDirectory
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException

/**
 * Stable identity of a running Melo instance, resolved once per process.
 *
 * The same values drive both the OS media-session name and the playback-session row, so
 * the MPRIS player and the saved queue can never point at different instances:
 *
 * | Instance       | [role] | [instanceKey]     | [mprisName]        |
 * |----------------|--------|-------------------|--------------------|
 * | primary GUI    | gui    | gui               | Melo               |
 * | primary TUI    | tui    | tui               | Melo.tui           |
 * | extra instance | gui    | gui.instance<PID> | Melo.instance<PID> |
 *
 * The first process for a given [mprisName] takes a lock file under the Melo data
 * directory and keeps the clean name; later processes fall back to the
 * `instance<PID>` suffix. Non-Linux platforms have no MPRIS bus name to contend for,
 * so they always resolve as primary.
 */
class InstanceIdentity private constructor(
    val role: String,
    val instanceKey: String,
    val mprisName: String,
    private val lockFile: RandomAccessFile?,
    private val lock: FileLock?,
) {
    /** True when this process owns the canonical name and session row for [role]. */
    val isPrimary: Boolean get() = instanceKey == role

    /**
     * Releases the identity lock. Must run *after* the media session disables itself,
     * because JMTC requests the bus name with REPLACE_EXISTING and a successor would
     * otherwise steal the name from a still-live owner.
     *
     * The lock file itself is intentionally left in place: unlinking it would let a
     * second process lock an orphaned inode while a third locks the fresh file.
     */
    @Suppress("TooGenericExceptionCaught")
    fun release() {
        try {
            lock?.release()
        } catch (_: Throwable) {
        }
        try {
            lockFile?.close()
        } catch (_: Throwable) {
        }
    }

    companion object {
        private const val LOCK_DIR_NAME = "locks"

        /**
         * Resolves the identity for [role] and its canonical [mprisName], taking the
         * process-wide lock when this is the first instance of that role.
         */
        fun resolve(
            role: String,
            mprisName: String,
        ): InstanceIdentity =
            resolve(
                role = role,
                mprisName = mprisName,
                lockDir = File(MeloDataDirectory.resolve(), LOCK_DIR_NAME),
                isLinux = isLinux(),
            )

        @Suppress("TooGenericExceptionCaught")
        internal fun resolve(
            role: String,
            mprisName: String,
            lockDir: File,
            isLinux: Boolean,
            pid: Long = ProcessHandle.current().pid(),
        ): InstanceIdentity {
            if (!isLinux) return InstanceIdentity(role, role, mprisName, null, null)
            return try {
                if (!lockDir.exists()) lockDir.mkdirs()
                val file = File(lockDir, "$mprisName.lock")
                val raf = RandomAccessFile(file, "rw")
                val lock =
                    try {
                        raf.channel.tryLock()
                    } catch (_: OverlappingFileLockException) {
                        // Another identity in this same JVM already holds it.
                        null
                    }
                if (lock == null) {
                    raf.close()
                    secondary(role, mprisName, pid)
                } else {
                    InstanceIdentity(role, role, mprisName, raf, lock)
                }
            } catch (_: Throwable) {
                // Never let lock trouble stop the app from coming up.
                secondary(role, mprisName, pid)
            }
        }

        private fun secondary(
            role: String,
            mprisName: String,
            pid: Long,
        ) = InstanceIdentity(
            role = role,
            instanceKey = "$role.instance$pid",
            mprisName = "$mprisName.instance$pid",
            lockFile = null,
            lock = null,
        )

        private fun isLinux(): Boolean = System.getProperty("os.name")?.lowercase()?.contains("linux") == true
    }
}
