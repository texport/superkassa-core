package io.github.texport.superkassa.offlinequeue.impl.policy

import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(kotlin.time.ExperimentalTime::class)
class TimeProviderTest {
    @Test
    fun testSystemTimeProvider() {
        val start = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val providerTime = SystemTimeProvider.now()
        val end = kotlin.time.Clock.System.now().toEpochMilliseconds()
        assertTrue(providerTime in start..end, "SystemTimeProvider should return current time in millis")
    }
}
