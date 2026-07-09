package kz.mybrain.superkassa.offline_queue.application.logging

import kotlin.test.Test
import kotlin.test.assertNotNull

class LoggerTest {
    @Test
    fun testLoggerCreationAndLogging() {
        val logger = getLogger(LoggerTest::class)
        assertNotNull(logger, "getLogger should return a non-null Logger")
        logger.info("Test info message with argument: {}", "value")
        logger.warn("Test warning message with argument: {}", "value")
        logger.error("Test error message", RuntimeException("Simulated error"))
    }
}
