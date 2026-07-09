package io.github.texport.superkassa.offlinequeue.impl.logging

import io.github.texport.superkassa.offlinequeue.impl.getLogger
import kotlin.test.Test
import kotlin.test.assertNotNull

class LoggerTest {
    @Test
    fun testLoggerCreationAndLogging() {
        val logger = getLogger(LoggerTest::class)
        assertNotNull(logger, "getLogger should return a non-null Logger on iOS")
        logger.trace("Test iOS trace message with argument: {}", "value")
        logger.debug("Test iOS debug message with argument: {}", "value")
        logger.info("Test iOS info message with argument: {}", "value")
        logger.warn("Test iOS warning message with argument: {}", "value")
        logger.error("Test iOS error message", RuntimeException("Simulated iOS error"))
    }
}
