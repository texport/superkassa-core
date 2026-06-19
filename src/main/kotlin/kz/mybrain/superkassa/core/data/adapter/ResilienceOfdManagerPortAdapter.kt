package kz.mybrain.superkassa.core.data.adapter

import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import kz.mybrain.superkassa.core.application.error.OfdTransientException
import kz.mybrain.superkassa.core.domain.model.OfdCommandRequest
import kz.mybrain.superkassa.core.domain.model.OfdCommandResult
import kz.mybrain.superkassa.core.domain.model.OfdCommandStatus
import kz.mybrain.superkassa.core.domain.port.OfdManagerPort
import org.slf4j.LoggerFactory
import java.time.Duration

/**
 * Декоратор OfdManagerPort с retry и circuit breaker для отказоустойчивости.
 */
class ResilienceOfdManagerPortAdapter(
    private val delegate: OfdManagerPort
) : OfdManagerPort {

    companion object {
        private const val MAX_ATTEMPTS = 3
        private const val WAIT_DURATION_MILLIS = 500L
        private const val CB_SLIDING_WINDOW_SIZE = 10
        private const val CB_MIN_CALLS = 5
        private const val CB_FAILURE_RATE = 50.0f
        private const val CB_WAIT_DURATION_SECONDS = 30L
        private const val CB_HALF_OPEN_CALLS = 3
    }

    private val logger = LoggerFactory.getLogger(ResilienceOfdManagerPortAdapter::class.java)

    private val retry = Retry.of("ofd") {
        RetryConfig.custom<OfdCommandResult>()
            .maxAttempts(MAX_ATTEMPTS)
            .waitDuration(Duration.ofMillis(WAIT_DURATION_MILLIS))
            .retryOnException { it is OfdTransientException }
            .build()
    }

    private val circuitBreaker = CircuitBreaker.of("ofd") {
        CircuitBreakerConfig.custom()
            .slidingWindowSize(CB_SLIDING_WINDOW_SIZE)
            .minimumNumberOfCalls(CB_MIN_CALLS)
            .failureRateThreshold(CB_FAILURE_RATE)
            .waitDurationInOpenState(Duration.ofSeconds(CB_WAIT_DURATION_SECONDS))
            .permittedNumberOfCallsInHalfOpenState(CB_HALF_OPEN_CALLS)
            .build()
    }

    override fun send(command: OfdCommandRequest): OfdCommandResult {
        val decorated = CircuitBreaker.decorateFunction(circuitBreaker) { cmd: OfdCommandRequest ->
            Retry.decorateFunction(retry) { c: OfdCommandRequest ->
                val result = delegate.send(c)
                if (result.status == OfdCommandStatus.FAILED && isTransientFailure(result)) {
                    throw OfdTransientException(result.errorMessage ?: "OFD request failed")
                }
                result
            }.apply(cmd)
        }
        return try {
            decorated.apply(command)
        } catch (e: CallNotPermittedException) {
            logger.warn("OFD circuit breaker open: {}", e.message)
            OfdCommandResult(
                status = OfdCommandStatus.FAILED,
                errorMessage = "OFD service temporarily unavailable (circuit breaker open)"
            )
        } catch (e: OfdTransientException) {
            logger.warn("OFD transient failure after retries: {}", e.message)
            OfdCommandResult(
                status = OfdCommandStatus.FAILED,
                errorMessage = e.message
            )
        } catch (e: Exception) {
            logger.error("OFD unexpected error", e)
            OfdCommandResult(
                status = OfdCommandStatus.FAILED,
                errorMessage = "OFD request failed: ${e.message ?: "unknown error"}"
            )
        }
    }

    private fun isTransientFailure(result: OfdCommandResult): Boolean {
        val msg = result.errorMessage?.lowercase() ?: return true
        return msg.contains("connection") ||
            msg.contains("timeout") ||
            msg.contains("unknown host") ||
            msg.contains("connection refused") ||
            msg.contains("network") ||
            msg.contains("empty response") ||
            msg.contains("unknown")
    }
}
