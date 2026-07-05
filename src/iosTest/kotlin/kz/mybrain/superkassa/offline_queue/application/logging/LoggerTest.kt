package kz.mybrain.superkassa.offline_queue.application.logging

import kotlin.test.Test
import kotlin.test.assertNotNull

class LoggerTest {
    @Test
    fun testLoggerCreationAndLogging() {
        val logger = getLogger(LoggerTest::class)
        assertNotNull(logger, "getLogger should return a non-null Logger on iOS")
        logger.info("Test iOS info message with argument: {}", "value")
        logger.warn("Test iOS warning message with argument: {}", "value")
        logger.error("Test iOS error message", RuntimeException("Simulated iOS error"))
    }
}
