package io.github.texport.superkassa.core.data.impl.ofd.strategy

import io.github.texport.superkassa.core.data.impl.ofd.OfdConfig
import io.github.texport.superkassa.core.data.impl.ofd.OfdRequestFactory
import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandRequest
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import kotlinx.serialization.json.JsonObject

/**
 * Стратегия построения запросов для команд с чеками (TICKET).
 *
 * Извлекает сохраненный фискальный документ и его платежную нагрузку из хранилища,
 * после чего формирует JSON-запрос чека для ОФД с расчетом налоговых ставок.
 */
// возвращаемый тип nullable переопределяет интерфейс
class TicketRequestBuilderStrategy(
    private val storage: StoragePort? = null
) : OfdRequestBuilderStrategy {

    /**
     * Проверяет, может ли стратегия обработать указанный тип команды [commandType].
     *
     * @param commandType тип команды ОФД.
     * @return `true`, если тип команды [OfdCommandType.TICKET], иначе `false`.
     */
    override fun canHandle(commandType: OfdCommandType): Boolean {
        return commandType == OfdCommandType.TICKET
    }

    /**
     * Строит JSON-запрос для фискального чека на основе сохраненного документа и конфигурации ОФД.
     *
     * @param command запрос команды ОФД [OfdCommandRequest].
     * @param config настройки протокола ОФД [OfdConfig].
     * @return JSON-объект [JsonObject] запроса фискального чека.
     */
    override fun build(command: OfdCommandRequest, config: OfdConfig): JsonObject? {
        val docWithPayload = storage?.findFiscalDocumentWithReceiptPayload(command.payloadRef)
        val receipt = docWithPayload?.second ?: ReceiptRequest(
            kkmId = command.kkmId,
            pin = "0000",
            operation = ReceiptOperationType.SELL,
            items = emptyList(),
            payments = emptyList(),
            total = Money(1000, 0),
            idempotencyKey = "tmp",
            parentTicket = null
        )

        val ofdId = command.ofdProviderId.lowercase()
        val serviceBlock = buildServiceBlock(command)

        return OfdRequestFactory.buildTicketRequest(
            ofdId = ofdId,
            protocolVersion = config.protocolVersion,
            deviceId = command.deviceId,
            token = command.token,
            reqNum = command.reqNum,
            request = receipt,
            serviceBlock = serviceBlock
        )
    } }
