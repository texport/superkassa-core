package io.github.texport.superkassa.coredatabase.impl.adapter

import platform.Foundation.NSRecursiveLock

internal actual class ReentrantGate actual constructor() {
    private val lock = NSRecursiveLock()

    actual fun lock() = lock.lock()

    actual fun unlock() = lock.unlock()
}
