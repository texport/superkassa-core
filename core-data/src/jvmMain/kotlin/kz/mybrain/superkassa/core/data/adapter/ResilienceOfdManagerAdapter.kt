package kz.mybrain.superkassa.core.data.adapter

import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import java.time.Duration
import kz.mybrain.superkassa.core.data.exception.OfdTransientException
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandRequest
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandResult
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandStatus
import kz.mybrain.superkassa.core.domain.port.OfdManagerPort
import org.slf4j.LoggerFactory

/**
 * Декоратор OfdManagerPort с механизмами повторных попыток (Retry) и автоматического отключения (Circuit Breaker) на базе библиотеки Resilience4j.
 * Настраивается и создается как Spring-бин в superkassa-server.
 */
class ResilienceOfdManagerAdapter(
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

    private val logger = LoggerFactory.getLogger(ResilienceOfdManagerAdapter::class.java)

    // Политика повторных попыток (Retry): совершает до 3 попыток с интервалом в 500мс при временных сбоях сети
    private val retry = Retry.of("ofd") {
        RetryConfig.custom<OfdCommandResult>()
            .maxAttempts(MAX_ATTEMPTS)
            .waitDuration(Duration.ofMillis(WAIT_DURATION_MILLIS))
            .retryOnException { it is OfdTransientException }
            .build()
    }

    // Circuit Breaker (Автоматический выключатель): разрывает цепь при 50% ошибок из последних 10 звонков,
    // переводя кассу на 30 секунд в автономный режим (оффлайн), не нагружая неработающий ОФД
    private val circuitBreaker = CircuitBreaker.of("ofd") {
        CircuitBreakerConfig.custom()
            .slidingWindowSize(CB_SLIDING_WINDOW_SIZE)
            .minimumNumberOfCalls(CB_MIN_CALLS)
            .failureRateThreshold(CB_FAILURE_RATE)
            .waitDurationInOpenState(Duration.ofSeconds(CB_WAIT_DURATION_SECONDS))
            .permittedNumberOfCallsInHalfOpenState(CB_HALF_OPEN_CALLS)
            .build()
    }

    /**
     * Выполняет отправку команды в ОФД, защищая сетевое соединение с помощью Retry и Circuit Breaker.
     * @param command Команда фискализации (чек, закрытие смены, X/Z-отчет).
     * @return Результат отправки команды (успех/ошибка/автономный статус).
     */
    override fun send(command: OfdCommandRequest): OfdCommandResult {
        // Декорируем вызов функции
        val decorated = CircuitBreaker.decorateFunction(circuitBreaker) { cmd: OfdCommandRequest ->
            Retry.decorateFunction(retry) { c: OfdCommandRequest ->
                val result = delegate.send(c)
                // Если произошел сетевой сбой, генерируем OfdTransientException для инициации ретрая
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

    /**
     * Определяет, является ли сбой временным (сетевым).
     * Временные сбои (таймауты, недоступность хоста) подлежат повторным попыткам и влияют на состояние Circuit Breaker.
     */
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
