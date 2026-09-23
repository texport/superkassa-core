package io.github.texport.superkassa.importnode.impl.target

import io.github.texport.superkassa.importnode.api.NodeImportException
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.OverlappingFileLockException

/**
 * Замок каталога данных кассы — тот же файл и тот же замок системы,
 * что берёт касса при запуске (`superkassa.lock`).
 *
 * Пока идёт перенос, касса над этим каталогом не поднимется, и наоборот.
 */
internal object DataDirLock {

    private const val LOCK = "superkassa.lock"

    fun acquire(dataDir: File): AutoCloseable {
        val file = RandomAccessFile(File(dataDir, LOCK), "rw")
        val lock = try {
            file.channel.tryLock()
        } catch (_: OverlappingFileLockException) {
            null
        }
        if (lock == null) {
            file.close()
            throw NodeImportException("Data directory $dataDir is used by a running cash register")
        }
        return AutoCloseable {
            lock.release()
            file.close()
        }
    }
}
