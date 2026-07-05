package kz.mybrain.superkassa.offline_queue.application.policy

import kotlin.test.Test
import kotlin.test.assertTrue

class TimeProviderTest {
    @Test
    fun testCurrentTimeMillis() {
        val start = System.currentTimeMillis()
        val providerTime = currentTimeMillis()
        val end = System.currentTimeMillis()
        assertTrue(providerTime in start..end, "TimeProvider should return current time in millis")
    }

    @Test
    fun testSystemTimeProvider() {
        val start = System.currentTimeMillis()
        val providerTime = SystemTimeProvider.now()
        val end = System.currentTimeMillis()
        assertTrue(providerTime in start..end, "SystemTimeProvider should wrap currentTimeMillis")
    }
}
