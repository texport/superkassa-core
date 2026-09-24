package io.github.texport.superkassa.embedded.impl.storage

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile
import platform.posix.LOCK_EX
import platform.posix.LOCK_NB
import platform.posix.LOCK_UN
import platform.posix.O_CREAT
import platform.posix.O_RDWR
import platform.posix.close
import platform.posix.flock
import platform.posix.open

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal actual object LocalFiles {
    private const val OWNER_READ_WRITE = 420 // 0644

    actual fun exists(path: String): Boolean = NSFileManager.defaultManager.fileExistsAtPath(path)

    actual fun readText(path: String): String? =
        if (exists(path)) NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null) else null

    actual fun writeText(path: String, text: String) {
        val written = NSString.create(string = text).writeToFile(path, true, NSUTF8StringEncoding, null)
        check(written) { "Cannot write $path" }
    }

    actual fun ensureDirectory(path: String) {
        val created = NSFileManager.defaultManager.createDirectoryAtPath(path, true, null, null)
        check(created) { "Cannot create data directory $path" }
    }

    actual fun lock(path: String): AutoCloseable {
        val descriptor = open(path, O_RDWR or O_CREAT, OWNER_READ_WRITE)
        check(descriptor >= 0) { "Cannot open lock file $path" }
        if (flock(descriptor, LOCK_EX or LOCK_NB) != 0) {
            close(descriptor)
            error("Data directory of $path is already used by another Superkassa instance")
        }
        return AutoCloseable {
            flock(descriptor, LOCK_UN)
            close(descriptor)
        }
    }
}
