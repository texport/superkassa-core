package kz.mybrain.superkassa.offline_queue.application.policy

import kotlin.test.Test
import kotlin.test.assertTrue

class TimeProviderTest {
    @Test
    fun testCurrentTimeMillis() {
        val providerTime = currentTimeMillis()
        assertTrue(providerTime > 0, "TimeProvider should return positive time in millis on iOS")
    }

    @Test
    fun testSystemTimeProvider() {
        val providerTime = SystemTimeProvider.now()
        assertTrue(providerTime > 0, "SystemTimeProvider should return positive time in millis on iOS")
    }
}
