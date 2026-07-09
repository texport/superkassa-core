package io.github.texport.superkassa.offlinequeue.impl.policy

@OptIn(kotlin.time.ExperimentalTime::class)
internal fun interface TimeProvider {
    fun now(): Long
}

@OptIn(kotlin.time.ExperimentalTime::class)
internal object SystemTimeProvider : TimeProvider {
    override fun now(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
}
