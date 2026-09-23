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
import kotlin.time.TimeMark
import kotlin.time.TimeSource
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
    /** Сколько касса ждёт ответа БФД, прежде чем счесть связь пропавшей. */
    internal val timeoutSeconds: Long = DEFAULT_RESPONSE_TIMEOUT_SECONDS,
    /** Интервал задержки между попытками восстановления связи (протокол п. 5), не менее 60 с. */
    private val reconnectIntervalSeconds: Long = 60L,
    /** Часы кассы: по ним отсчитывается интервал восстановления связи. */
    private val now: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() }
) : OfdManagerPort {
    private val logger = getLogger(OfdManagerAdapter::class)
    private val reconnectIntervalMs: Long = reconnectIntervalSeconds.coerceAtLeast(
        MIN_RECONNECT_INTERVAL_SECONDS
    ) * SECONDS_TO_MILLIS

    /**
     * Страховочный предел вокруг всего обмена.
     *
     * Свой срок сетевой клиент считает сам и по нему возвращает отказ,
     * который касса умеет отличить от отказа БФД по существу. Здесь предел
     * заведомо больше, чтобы первым срабатывал именно он, а этот остался
     * на случай, когда клиент не вернулся вовсе.
     */
    internal val guardTimeoutSeconds: Long = timeoutSeconds + GUARD_MARGIN_SECONDS

    /** Время последней неудачи по связи (нет ответа ОФД) по kkmId. */
    private val lastNoConnectionMillis = createConcurrentMap<String, Long>()

    companion object {
        private const val SECONDS_TO_MILLIS = 1000L
        private const val MIN_RECONNECT_INTERVAL_SECONDS = 60L

        /** Умолчание ожидания ответа БФД: столько кассир ждёт у экрана. */
        internal const val DEFAULT_RESPONSE_TIMEOUT_SECONDS = 7L

        /** Запас страховочного предела над сроком сетевого клиента. */
        private const val GUARD_MARGIN_SECONDS = 1L

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
        val now = now()
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
        val started = TimeSource.Monotonic.markNow()
        return try {
            val endpoint = resolveEndpoint(command)
                ?: return OfdCommandResult(
                    status = OfdCommandStatus.FAILED,
                    errorMessage = CoreStrings.ofdRequestFailedData(
                        "Invalid BFD configuration: provider or endpoint not found"
                    )
                )
            val json = buildRequest(command)
                ?: return OfdCommandResult(
                    status = OfdCommandStatus.FAILED,
                    errorMessage = CoreStrings.ofdRequestFailedData(
                        "Missing required request parameters"
                    )
                )
            val bytes = codec.encode(json)
            val response = runBlocking {
                try {
                    withTimeout(guardTimeoutSeconds.seconds) {
                        networkClient.sendAndReceive(endpoint, bytes)
                    }
                } catch (_: TimeoutCancellationException) {
                    logger.warn("OFD request timeout after $guardTimeoutSeconds seconds")
                    Result.failure<ByteArray>(
                        Exception("BFD request timeout after ${guardTimeoutSeconds}s")
                    )
                }
            }
            if (response.isFailure) {
                val exception = response.exceptionOrNull()
                val error = exception?.message ?: "unknown"

                val exName = exception?.let { it::class.simpleName } ?: ""
                val causeName = exception?.cause?.let { it::class.simpleName } ?: ""
                val isNetworkError = exName.contains("IOException") ||
                    causeName.contains("IOException") ||
                    exName.contains("Timeout") ||
                    error.contains("timeout", ignoreCase = true) ||
                    error.contains("time out", ignoreCase = true) ||
                    error.contains("Нет ответа", ignoreCase = true) ||
                    error.contains("No response", ignoreCase = true) ||
                    error.contains("connection", ignoreCase = true) ||
                    error.contains("refused", ignoreCase = true)

                lastNoConnectionMillis[throttleKey] = now
                logger.warn("BFD ${exchange(command, started)} failed: ${exception?.let { it::class.simpleName }}")

                return OfdCommandResult(
                    status = if (isNetworkError) OfdCommandStatus.TIMEOUT else OfdCommandStatus.FAILED,
                    errorMessage = CoreStrings.ofdRequestFailedData(error)
                )
            }

            val responseBytes = response.getOrThrow()
            val responseJson = codec.decode(responseBytes)
            val resultCode = extractResultCode(responseJson)
            val resultText = extractResultText(responseJson)
            val responseToken = extractHeaderToken(responseJson)
            val responseReqNum = extractHeaderReqNum(responseJson)
            val fiscalSign = OfdResponseUtils.extractFiscalSign(responseJson)

            if (resultCode != null) lastNoConnectionMillis.remove(throttleKey)

            val status = when (resultCode) {
                0 -> OfdCommandStatus.OK
                254, 255 -> OfdCommandStatus.TIMEOUT
                else -> OfdCommandStatus.FAILED
            }

            val answered = "BFD ${exchange(command, started)} answered with code $resultCode"
            if (status == OfdCommandStatus.OK) logger.info(answered) else logger.warn(answered)

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
            logger.warn("BFD ${exchange(command, started)} failed: packet cannot be encoded or decoded")
            // Кода результата здесь нет: ОФД отказал на уровне протокола
            // и своего кода не присылал. Прежде подставлялось `-1`, и кассир
            // читал в журнале «Код отказа −1» — число, которого в CPCR нет.
            // Пусто честнее: отказ объясняют слова, а не выдуманный код.
            OfdCommandResult(
                status = OfdCommandStatus.FAILED,
                resultCode = null,
                errorMessage = ex.message
            )
        } catch (ex: Exception) {
            val exName = ex::class.simpleName ?: ""
            val causeName = ex.cause?.let { it::class.simpleName } ?: ""
            val errorMsg = ex.message ?: "unknown"

            val isNetworkError = exName.contains("IOException") ||
                causeName.contains("IOException") ||
                exName.contains("Timeout") ||
                errorMsg.contains("connection", ignoreCase = true) ||
                errorMsg.contains("refused", ignoreCase = true)

            val failed = "BFD ${exchange(command, started)} failed: $exName"
            if (isNetworkError) logger.warn(failed) else logger.error(failed)

            lastNoConnectionMillis[throttleKey] = now

            OfdCommandResult(
                status = if (isNetworkError) OfdCommandStatus.TIMEOUT else OfdCommandStatus.FAILED,
                errorMessage = CoreStrings.ofdRequestFailedData(errorMsg)
            )
        }
    }

    /**
     * Обмен в журнале: команда, номер запроса и длительность — и только.
     *
     * Пакеты, токен и фискальные документы в журнал не пишутся: в них
     * покупатель, суммы и право слать документы от имени кассы. Разбор
     * обмена ведётся по номеру запроса и коду ответа, а текст отказа
     * получает вызывающий в результате.
     */
    private fun exchange(command: OfdCommandRequest, started: TimeMark): String =
        "${command.commandType} reqNum=${command.reqNum} in ${started.elapsedNow().inWholeMilliseconds} ms"

    /** Адрес отправки берётся из перечисления провайдеров по контуру команды. */
    private fun resolveEndpoint(command: OfdCommandRequest): NetworkEndpoint? {
        val provider = OfdProvider.findProvider(command.ofdProviderId) ?: return null
        val environment = OfdEnvironment.findEnvironment(command.ofdEnvironmentId) ?: return null
        val endpoint = provider.endpoint(environment) ?: return null
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
