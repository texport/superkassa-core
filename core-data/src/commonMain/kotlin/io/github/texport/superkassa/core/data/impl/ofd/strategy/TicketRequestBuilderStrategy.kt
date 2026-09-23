package io.github.texport.superkassa.core.data.impl.ofd.strategy

import io.github.texport.superkassa.core.data.impl.ofd.OfdConfig
import io.github.texport.superkassa.core.data.impl.ofd.OfdRequestFactory
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandRequest
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
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
        // Ненайденный чек — отказ, а не подстановка. Фискальный документ
        // нельзя сочинить: у выдуманного чека нет ни позиций, ни оплат,
        // а сумма взялась бы из воздуха.
        val stored = storage?.findFiscalDocumentWithReceiptPayload(command.payloadRef)
            ?: return null
        val (document, receipt) = stored
        // Чек, оформленный при оборванной связи, обязан нести свой автономный
        // номер: иначе сервер примет его как обычный сетевой документ.
        val offlineTicketNumber =
            if (document.isAutonomous) document.docNo?.let(::asUnsignedInt32) else null

        val ofdId = command.ofdProviderId.lowercase()
        val serviceBlock = buildServiceBlock(command) ?: return null
        // Смена и время — те, в которые чек оформлен, а не те, в которые он
        // досылается: иначе вчерашний чек ложится в сегодняшнюю смену.
        val frShiftNumber = shiftNumberOf(storage, document, command.kkmId)

        return OfdRequestFactory.buildTicketRequest(
            ofdId = ofdId,
            protocolVersion = config.protocolVersion,
            deviceId = command.deviceId,
            token = command.token,
            reqNum = command.reqNum,
            request = receipt,
            serviceBlock = serviceBlock,
            frShiftNumber = frShiftNumber,
            offlineTicketNumber = offlineTicketNumber,
            printedDocumentNumber = document.printedDocumentNumber,
            dateTimeMillis = document.createdAt
        )
    } }

/** Наибольшее значение поля uint32 в протоколе. */
private const val UNSIGNED_INT32_MAX = 4_294_967_295L

/**
 * Приводит номер к полю uint32.
 *
 * Значения выше Int.MAX_VALUE — законные для uint32: protobuf пишет их
 * в дополнительном коде, и на проводе они разворачиваются верно. А вот выше
 * 4294967295 число в поле не помещается, и молча заворачивать его нельзя:
 * это дало бы чужой номер документа.
 */
private fun asUnsignedInt32(value: Long): Int {
    require(value in 0..UNSIGNED_INT32_MAX) {
        "Document number " + value + " does not fit in uint32"
    }
    return value.toInt()
}
