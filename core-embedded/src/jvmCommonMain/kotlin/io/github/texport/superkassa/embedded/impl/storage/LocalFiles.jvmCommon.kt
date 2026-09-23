package io.github.texport.superkassa.embedded.impl.storage

import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal actual object LocalFiles {
    actual fun exists(path: String): Boolean = File(path).isFile

    actual fun readText(path: String): String? = File(path).takeIf { it.isFile }?.readText()

    actual fun writeText(path: String, text: String) {
        val target = File(path)
        val draft = File(target.parentFile, "${target.name}.tmp")
        draft.writeText(text)
        Files.move(draft.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    actual fun ensureDirectory(path: String) {
        val dir = File(path)
        check(dir.isDirectory || dir.mkdirs()) { "Cannot create data directory $path" }
    }

    actual fun lock(path: String): AutoCloseable {
        val file = RandomAccessFile(path, "rw")
        val lock = try {
            file.channel.tryLock()
        } catch (_: OverlappingFileLockException) {
            null
        }
        if (lock == null) {
            file.close()
            error("Data directory of $path is already used by another Superkassa instance")
        }
        return AutoCloseable {
            lock.release()
            file.close()
        }
    }
}
