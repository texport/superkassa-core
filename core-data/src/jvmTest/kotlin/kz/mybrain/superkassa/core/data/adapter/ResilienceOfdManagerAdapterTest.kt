package kz.mybrain.superkassa.core.data.adapter

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandRequest
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandResult
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandStatus
import kz.mybrain.superkassa.core.domain.port.OfdManagerPort

class ResilienceOfdManagerAdapterTest {
    private val delegate: OfdManagerPort = mockk()
    private val adapter = ResilienceOfdManagerAdapter(delegate)

    @Test
    fun testSendSuccess() {
        val request = mockk<OfdCommandRequest>()
        val expectedResult = OfdCommandResult(
            status = OfdCommandStatus.OK,
            fiscalSign = "fs-123"
        )

        every { delegate.send(request) } returns expectedResult

        val actualResult = adapter.send(request)
        assertEquals(OfdCommandStatus.OK, actualResult.status)
        assertEquals("fs-123", actualResult.fiscalSign)

        verify(exactly = 1) { delegate.send(request) }
    }

    @Test
    fun testSendTransientFailureRetriesAndSucceeds() {
        val request = mockk<OfdCommandRequest>()
        val failResult = OfdCommandResult(
            status = OfdCommandStatus.FAILED,
            errorMessage = "Connection timeout occurred"
        )
        val successResult = OfdCommandResult(
            status = OfdCommandStatus.OK,
            fiscalSign = "fs-123"
        )

        // First attempt fails with transient network error, second attempt succeeds
        every { delegate.send(request) } returns failResult andThen successResult

        val actualResult = adapter.send(request)
        assertEquals(OfdCommandStatus.OK, actualResult.status)
        assertEquals("fs-123", actualResult.fiscalSign)

        verify(exactly = 2) { delegate.send(request) }
    }

    @Test
    fun testSendPermanentFailureDoesNotRetry() {
        val request = mockk<OfdCommandRequest>()
        val permanentFailResult = OfdCommandResult(
            status = OfdCommandStatus.FAILED,
            errorMessage = "Validation error: invalid tag format"
        )

        every { delegate.send(request) } returns permanentFailResult

        val actualResult = adapter.send(request)
        assertEquals(OfdCommandStatus.FAILED, actualResult.status)
        assertEquals("Validation error: invalid tag format", actualResult.errorMessage)

        verify(exactly = 1) { delegate.send(request) }
    }

    @Test
    fun testSendUnexpectedExceptionLogged() {
        val request = mockk<OfdCommandRequest>()
        every { delegate.send(request) } throws RuntimeException("Unexpected db lock error")

        val actualResult = adapter.send(request)
        assertEquals(OfdCommandStatus.FAILED, actualResult.status)
        assertEquals("OFD request failed: Unexpected db lock error", actualResult.errorMessage)
    }

    @Test
    fun testSendUnexpectedExceptionNoMessage() {
        val request = mockk<OfdCommandRequest>()
        every { delegate.send(request) } throws RuntimeException()

        val actualResult = adapter.send(request)
        assertEquals(OfdCommandStatus.FAILED, actualResult.status)
        assertEquals("OFD request failed: unknown error", actualResult.errorMessage)
    }

    @Test
    fun testCircuitBreakerOpensAndRejectsCalls() {
        val request = mockk<OfdCommandRequest>()
        val failResult = OfdCommandResult(
            status = OfdCommandStatus.FAILED,
            errorMessage = "Connection refused error"
        )

        // Make it return transient failures repeatedly
        every { delegate.send(request) } returns failResult

        // We need to trigger the sliding window of CB.
        // CB Config: min calls = 5, sliding window = 10, failure rate = 50%
        // Let's call it 6 times. All 6 fail.
        repeat(6) {
            val res = adapter.send(request)
            assertEquals(OfdCommandStatus.FAILED, res.status)
        }

        // Now circuit breaker should be in OPEN state.
        // The next call will be rejected immediately by the CB decorator without calling delegate.
        val rejectedResult = adapter.send(request)
        assertEquals(OfdCommandStatus.FAILED, rejectedResult.status)
        assertEquals("OFD service temporarily unavailable (circuit breaker open)", rejectedResult.errorMessage)

        // Verify delegate was called 3 attempts * 6 requests = 18 times (or less due to retries policy count)
        // Wait, each send(request) retries up to 3 times because it's a transient exception.
        // So the delegate is indeed called multiple times.
        verify(atLeast = 5) { delegate.send(request) }
    }
    @Test
    fun testIsTransientFailureVariousMessages() {
        val request = mockk<OfdCommandRequest>()
        
        val transientErrors = listOf(
            null,
            "Unknown host error",
            "Network is down",
            "Empty response from server",
            "Unknown OFD response",
            "connection error"
        )
        
        for (err in transientErrors) {
            val failResult = OfdCommandResult(
                status = OfdCommandStatus.FAILED,
                errorMessage = err
            )
            // It will retry if transient is true.
            // By returning failResult twice, it will retry and verify that it is transient.
            every { delegate.send(request) } returns failResult
            
            adapter.send(request)
            
            // Verifies it retried (at least 2 invocations)
            verify(atLeast = 2) { delegate.send(request) }
        }
    }
}
