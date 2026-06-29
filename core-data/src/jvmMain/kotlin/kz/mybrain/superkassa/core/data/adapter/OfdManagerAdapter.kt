package kz.mybrain.superkassa.core.data.adapter

import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kz.mybrain.network.OfdEndpoint as NetworkEndpoint
import kz.mybrain.network.OfdNetworkClient
import kz.mybrain.superkassa.core.data.exception.DataErrorMessages
import kz.mybrain.superkassa.core.data.exception.OfdProtocolException
import kz.mybrain.superkassa.core.data.ofd.OfdProtocolCodec
import kz.mybrain.superkassa.core.data.ofd.OfdConfig
import kz.mybrain.superkassa.core.data.ofd.OfdResponseUtils
import kz.mybrain.superkassa.core.data.ofd.builder.strategy.OfdRequestBuilderStrategy
import kz.mybrain.superkassa.core.data.ofd.builder.strategy.ServiceRequestBuilderStrategy
import kz.mybrain.superkassa.core.data.ofd.builder.strategy.TicketRequestBuilderStrategy
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandRequest
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandResult
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandStatus
import kz.mybrain.superkassa.core.domain.model.ofd.OfdEnvironment
import kz.mybrain.superkassa.core.domain.model.ofd.OfdProvider
import kz.mybrain.superkassa.core.domain.port.OfdManagerPort
import org.slf4j.LoggerFactory

/**
 * Адаптер ОФД-менеджера с сериализацией через ofd-proto-codec.
 * Реализует OfdManagerPort и отправляет команды на сервер ОФД через сетевой клиент.
 * Использует стратегии для построения запросов (OCP).
 * Настраивается и создается как Spring-бин в superkassa-server.
 */
class OfdManagerAdapter(
    private val config: OfdConfig,
    private val codec: OfdProtocolCodec,
    private val networkClient: OfdNetworkClient,
    private val requestBuilders: List<OfdRequestBuilderStrategy>,
    /** Общее время на обработку транзакции (протокол п. 5), не менее 5 с. */
    private val timeoutSeconds: Long = 30L,
    /** Интервал задержки между попытками восстановления связи (протокол п. 5), не менее 60 с. */
    private val reconnectIntervalSeconds: Long = 60L
) : OfdManagerPort {
    private val logger = LoggerFactory.getLogger(OfdManagerAdapter::class.java)

    private val reconnectIntervalMs: Long = reconnectIntervalSeconds.coerceAtLeast(MIN_RECONNECT_INTERVAL_SECONDS) * SECONDS_TO_MILLIS

    /** Время последней неудачи по связи (нет ответа ОФД) по kkmId. */
    private val lastNoConnectionMillis = ConcurrentHashMap<String, Long>()

    companion object {
        private const val SECONDS_TO_MILLIS = 1000L
        private const val MIN_RECONNECT_INTERVAL_SECONDS = 60L

        /**
         * Дефолтный набор стратегий (без StoragePort).
         * Используется в тестах; в проде передаётся через конструктор.
         */
        fun defaultRequestBuilders(): List<OfdRequestBuilderStrategy> = listOf(
            ServiceRequestBuilderStrategy(),
            TicketRequestBuilderStrategy()
        )
    }

    override fun send(command: OfdCommandRequest): OfdCommandResult {
        val now = System.currentTimeMillis()
        val throttleKey = "${command.kkmId}:${command.ofdProviderId}:${command.ofdEnvironmentId}"
        val lastFail = lastNoConnectionMillis[throttleKey]
        if (lastFail != null && (now - lastFail) < reconnectIntervalMs) {
            logger.debug("OFD throttle: kkmId={}, retry after {}s", command.kkmId, reconnectIntervalSeconds)
            return OfdCommandResult(
                status = OfdCommandStatus.TIMEOUT,
                errorMessage = DataErrorMessages.ofdRequestFailed(
                    "No connection; retry not earlier than ${reconnectIntervalSeconds}s"
                ),
                resultCode = null
            )
        }
        return try {
            val endpoint = resolveEndpoint(command)
                ?: return OfdCommandResult(
                    status = OfdCommandStatus.FAILED,
                    errorMessage = DataErrorMessages.ofdRequestFailed("Invalid OFD configuration: provider or endpoint not found")
                )
            val json = buildRequest(command)
                ?: return OfdCommandResult(
                    status = OfdCommandStatus.FAILED,
                    errorMessage = DataErrorMessages.ofdRequestFailed("Missing required request parameters")
                )
            logger.info("OFD SEND: commandType={}, kkmId={}, reqNum={}, token={}", command.commandType, command.kkmId, command.reqNum, command.token)
            val bytes = codec.encode(json)
            val response = runBlocking {
                try {
                    withTimeout(timeoutSeconds.seconds) {
                        networkClient.sendAndReceive(endpoint, bytes)
                    }
                } catch (_: TimeoutCancellationException) {
                    logger.warn("OFD request timeout after {} seconds", timeoutSeconds)
                    return@runBlocking Result.failure(
                        java.util.concurrent.TimeoutException("OFD request timeout after ${timeoutSeconds}s")
                    )
                }
            }
            if (response.isFailure) {
                val error = response.exceptionOrNull()?.message ?: "unknown"
                val isTimeout = error.contains("timeout", ignoreCase = true)
                lastNoConnectionMillis[throttleKey] = now
                logger.warn("OFD SEND FAILED: commandType={}, kkmId={}, error={}", command.commandType, command.kkmId, error)
                return OfdCommandResult(
                    status = if (isTimeout) OfdCommandStatus.TIMEOUT else OfdCommandStatus.FAILED,
                    errorMessage = DataErrorMessages.ofdRequestFailed(error)
                )
            }

            val responseBytes = response.getOrThrow()
            val responseJson = codec.decode(responseBytes)
            logger.debug("DEBUG_OFD_RESPONSE_JSON: {}", responseJson)
            val resultCode = extractResultCode(responseJson)
            val resultText = extractResultText(responseJson)
            val responseToken = extractHeaderToken(responseJson)
            val responseReqNum = extractHeaderReqNum(responseJson)
            val fiscalSign = OfdResponseUtils.extractFiscalSign(responseJson)

            if (resultCode != null) lastNoConnectionMillis.remove(throttleKey)
            val status = if (resultCode == 0) OfdCommandStatus.OK else OfdCommandStatus.FAILED
            
            if (status == OfdCommandStatus.OK) {
                logger.info(
                    "OFD RECV SUCCESS: commandType={}, resultCode=0, responseToken={}, responseReqNum={}, fiscalSign={}",
                    command.commandType, responseToken, responseReqNum, fiscalSign
                )
            } else {
                logger.warn(
                    "OFD RECV ERROR: commandType={}, resultCode={}, text={}",
                    command.commandType, resultCode, resultText
                )
            }

            OfdCommandResult(
                status = status,
                responseBin = responseBytes,
                responseJson = responseJson,
                responseToken = responseToken,
                responseReqNum = responseReqNum,
                resultCode = resultCode,
                resultText = resultText,
                fiscalSign = fiscalSign,
                receiptUrl = OfdResponseUtils.extractReceiptUrl(responseJson),
                errorMessage = if (status == OfdCommandStatus.OK) null else resultText
            )
        } catch (ex: OfdProtocolException) {
            logger.warn(
                "OFD protocol error for kkmId={}, commandType={}, payloadRef={}: {}",
                command.kkmId,
                command.commandType,
                command.payloadRef,
                ex.message
            )
            OfdCommandResult(
                status = OfdCommandStatus.FAILED,
                resultCode = -1,
                errorMessage = ex.message
            )
        } catch (ex: Exception) {
            val isNetworkError = ex is java.io.IOException || ex.cause is java.io.IOException || ex is java.util.concurrent.TimeoutException
            if (isNetworkError) {
                logger.warn("OFD connection failed for kkmId={}: {}", command.kkmId, ex.message ?: "unknown")
            } else {
                logger.error("OFD request failed with unexpected error", ex)
            }
            lastNoConnectionMillis[throttleKey] = now
            OfdCommandResult(
                status = OfdCommandStatus.FAILED,
                errorMessage = DataErrorMessages.ofdRequestFailed(ex.message)
            )
        }
    }

    private fun resolveEndpoint(command: OfdCommandRequest): NetworkEndpoint? {
        val provider = OfdProvider.findProvider(command.ofdProviderId) ?: return null
        val environment = OfdEnvironment.entries.firstOrNull {
            it.name.equals(command.ofdEnvironmentId, ignoreCase = true)
        } ?: return null
        val endpoint = provider.endpoints[environment] ?: return null
        return NetworkEndpoint(endpoint.host, endpoint.port)
    }

    private fun buildRequest(command: OfdCommandRequest): JsonObject? {
        // Находим первую стратегию, которая может обработать данный тип команды
        val builder = requestBuilders.firstOrNull { it.canHandle(command.commandType) }
            ?: return null
        return builder.build(command, config)
    }

    private fun extractHeaderToken(response: JsonObject): Long? {
        val header = response["header"] as? JsonObject ?: return null
        return header["token"]?.jsonPrimitive?.longOrNull
    }

    private fun extractHeaderReqNum(response: JsonObject): Int? {
        val header = response["header"] as? JsonObject ?: return null
        return header["reqNum"]?.jsonPrimitive?.intOrNull
    }

    private fun extractResultCode(response: JsonObject): Int? {
        val payload = response["payload"]?.jsonObject ?: return null
        val result = payload["result"]?.jsonObject ?: return null
        return result["resultCode"]?.jsonPrimitive?.intOrNull
    }

    private fun extractResultText(response: JsonObject): String? {
        val payload = response["payload"]?.jsonObject ?: return null
        val result = payload["result"]?.jsonObject ?: return null
        return result["resultText"]?.jsonPrimitive?.contentOrNull
    }
}
