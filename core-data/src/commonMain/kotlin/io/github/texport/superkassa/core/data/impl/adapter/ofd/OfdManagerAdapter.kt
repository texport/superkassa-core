package io.github.texport.superkassa.core.data.impl.adapter.ofd

import io.github.texport.superkassa.core.data.impl.exception.OfdProtocolException
import io.github.texport.superkassa.core.data.impl.ofd.OfdConfig
import io.github.texport.superkassa.core.data.impl.ofd.OfdProtocolCodec
import io.github.texport.superkassa.core.data.impl.ofd.OfdResponseUtils
import io.github.texport.superkassa.core.data.impl.ofd.strategy.NomenclatureRequestBuilderStrategy
import io.github.texport.superkassa.core.data.impl.ofd.strategy.OfdRequestBuilderStrategy
import io.github.texport.superkassa.core.data.impl.ofd.strategy.ServiceRequestBuilderStrategy
import io.github.texport.superkassa.core.data.impl.ofd.strategy.TicketRequestBuilderStrategy
import io.github.texport.superkassa.core.data.impl.util.createConcurrentMap
import io.github.texport.superkassa.core.domain.impl.logging.getLogger
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandRequest
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandStatus
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdEnvironment
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdProvider
import io.github.texport.superkassa.core.domain.api.port.internal.OfdManagerPort
import io.github.texport.superkassa.core.string.api.CoreStrings
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kz.mybrain.network.OfdNetworkClient
import kotlin.time.Duration.Companion.seconds
import kz.mybrain.network.OfdEndpoint as NetworkEndpoint

/**
 * Адаптер ОФД-менеджера с сериализацией через ofd-proto-codec.
 * Реализует OfdManagerPort и отправляет команды на сервер ОФД через сетевой клиент.
 * Использует стратегии для построения запросов (OCP).
 * Настраивается и создается как Spring-бин в superkassa-server.
 */
internal class OfdManagerAdapter(
    private val config: OfdConfig,
    private val codec: OfdProtocolCodec,
    private val networkClient: OfdNetworkClient,
    private val requestBuilders: List<OfdRequestBuilderStrategy>,
    /** Общее время на обработку транзакции (протокол п. 5), не менее 5 с. */
    private val timeoutSeconds: Long = 30L,
    /** Интервал задержки между попытками восстановления связи (протокол п. 5), не менее 60 с. */
    private val reconnectIntervalSeconds: Long = 60L
) : OfdManagerPort {
    private val logger = getLogger(OfdManagerAdapter::class)
    private val prettyJson = kotlinx.serialization.json.Json { prettyPrint = true }

    private fun formatJson(json: kotlinx.serialization.json.JsonElement): String {
        return if (config.prettyPrintJson) {
            prettyJson.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), json)
        } else {
            json.toString()
        }
    }

    private val reconnectIntervalMs: Long = reconnectIntervalSeconds.coerceAtLeast(
        MIN_RECONNECT_INTERVAL_SECONDS
    ) * SECONDS_TO_MILLIS

    /** Время последней неудачи по связи (нет ответа ОФД) по kkmId. */
    private val lastNoConnectionMillis = createConcurrentMap<String, Long>()

    companion object {
        private const val SECONDS_TO_MILLIS = 1000L
        private const val MIN_RECONNECT_INTERVAL_SECONDS = 60L

        /**
         * Дефолтный набор стратегий (без StoragePort).
         * Используется в тестах; в проде передаётся через конструктор.
         */
        fun defaultRequestBuilders(): List<OfdRequestBuilderStrategy> = listOf(
            ServiceRequestBuilderStrategy(),
            TicketRequestBuilderStrategy(),
            NomenclatureRequestBuilderStrategy()
        )
    }

    override fun send(command: OfdCommandRequest): OfdCommandResult {
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val throttleKey = "${command.kkmId}:${command.ofdProviderId}:${command.ofdEnvironmentId}"
        val lastFail = lastNoConnectionMillis[throttleKey]
        if (lastFail != null && (now - lastFail) < reconnectIntervalMs) {
            val throttleMsg = "OFD throttle: kkmId=${command.kkmId}, " +
                "retry after ${reconnectIntervalSeconds}s"
            logger.info(throttleMsg)
            return OfdCommandResult(
                status = OfdCommandStatus.TIMEOUT,
                errorMessage = CoreStrings.ofdRequestFailedData(
                    "No connection; retry not earlier than ${reconnectIntervalSeconds}s"
                ),
                resultCode = null
            )
        }
        return try {
            val endpoint = resolveEndpoint(command)
                ?: return OfdCommandResult(
                    status = OfdCommandStatus.FAILED,
                    errorMessage = CoreStrings.ofdRequestFailedData(
                        "Invalid OFD configuration: provider or endpoint not found"
                    )
                )
            val json = buildRequest(command)
                ?: return OfdCommandResult(
                    status = OfdCommandStatus.FAILED,
                    errorMessage = CoreStrings.ofdRequestFailedData(
                        "Missing required request parameters"
                    )
                )
            logger.debug("OFD SEND JSON: ${formatJson(json)}")
            val sendMsg = "OFD SEND: commandType=${command.commandType}, " +
                "kkmId=${command.kkmId}, reqNum=${command.reqNum}, " +
                "token=${command.token}"
            logger.info(sendMsg)
            val bytes = codec.encode(json)
            val response = runBlocking {
                try {
                    withTimeout(timeoutSeconds.seconds) {
                        networkClient.sendAndReceive(endpoint, bytes)
                    }
                } catch (_: TimeoutCancellationException) {
                    logger.warn("OFD request timeout after $timeoutSeconds seconds")
                    Result.failure<ByteArray>(
                        Exception("OFD request timeout after ${timeoutSeconds}s")
                    )
                }
            }
            if (response.isFailure) {
                val error = response.exceptionOrNull()?.message ?: "unknown"
                val isTimeout = error.contains("timeout", ignoreCase = true) ||
                    error.contains("time out", ignoreCase = true) ||
                    error.contains("Нет ответа", ignoreCase = true) ||
                    error.contains("No response", ignoreCase = true)
                lastNoConnectionMillis[throttleKey] = now
                val sendFailMsg = "OFD SEND FAILED: commandType=${command.commandType}, " +
                    "kkmId=${command.kkmId}, error=$error"
                logger.warn(sendFailMsg)
                return OfdCommandResult(
                    status = if (isTimeout) OfdCommandStatus.TIMEOUT else OfdCommandStatus.FAILED,
                    errorMessage = CoreStrings.ofdRequestFailedData(error)
                )
            }

            val responseBytes = response.getOrThrow()
            val responseJson = codec.decode(responseBytes)
            logger.debug("DEBUG_OFD_RESPONSE_JSON: ${formatJson(responseJson)}")
            val resultCode = extractResultCode(responseJson)
            val resultText = extractResultText(responseJson)
            val responseToken = extractHeaderToken(responseJson)
            val responseReqNum = extractHeaderReqNum(responseJson)
            val fiscalSign = OfdResponseUtils.extractFiscalSign(responseJson)

            if (resultCode != null) lastNoConnectionMillis.remove(throttleKey)
            val status = if (resultCode == 0) OfdCommandStatus.OK else OfdCommandStatus.FAILED

            if (status == OfdCommandStatus.OK) {
                val successMsg = "OFD RECV SUCCESS: commandType=${command.commandType}, " +
                    "resultCode=0, responseToken=$responseToken, " +
                    "responseReqNum=$responseReqNum, fiscalSign=$fiscalSign"
                logger.info(successMsg)
            } else {
                val errorMsg = "OFD RECV ERROR: commandType=${command.commandType}, " +
                    "resultCode=$resultCode, text=$resultText"
                logger.warn(errorMsg)
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
                receiptUrl = OfdResponseUtils.extractReceiptUrl(
                    responseJson
                ),
                errorMessage = if (status == OfdCommandStatus.OK) {
                    null
                } else {
                    resultText
                }
            )
        } catch (ex: OfdProtocolException) {
            val protoErrorMsg = "OFD protocol error for kkmId=${command.kkmId}, " +
                "commandType=${command.commandType}, payloadRef=${command.payloadRef}: " +
                "${ex.message ?: "unknown"}"
            logger.warn(protoErrorMsg)
            OfdCommandResult(
                status = OfdCommandStatus.FAILED,
                resultCode = -1,
                errorMessage = ex.message
            )
        } catch (ex: Exception) {
            val exName = ex::class.simpleName ?: ""
            val causeName = ex.cause?.let { it::class.simpleName } ?: ""
            val isNetworkError = exName.contains("IOException") ||
                causeName.contains("IOException") ||
                exName.contains("Timeout")
            if (isNetworkError) {
                val netErrMsg = "OFD connection failed for kkmId=${command.kkmId}: " +
                    "${ex.message ?: "unknown"}"
                logger.warn(netErrMsg)
            } else {
                logger.error("OFD request failed with unexpected error", ex)
            }
            lastNoConnectionMillis[throttleKey] = now
            OfdCommandResult(
                status = OfdCommandStatus.FAILED,
                errorMessage = CoreStrings.ofdRequestFailedData(ex.message)
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
