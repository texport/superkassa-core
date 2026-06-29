package kz.mybrain.superkassa.core.data.ofd.builder.strategy

import kotlinx.serialization.json.JsonObject
import kz.mybrain.superkassa.core.data.ofd.OfdConfig
import kz.mybrain.superkassa.core.data.ofd.OfdRequestFactory
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandRequest
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandType
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.core.domain.usecase.shift.RecalculateShiftCountersUseCase
import kz.mybrain.superkassa.core.domain.helper.zxreport.ZxReportBuilder

/**
 * Стратегия построения запросов для COMMAND_CLOSE_SHIFT (Z-отчет, закрытие смены).
 *
 * Стратегия извлекает текущую открытую смену, пересчитывает сменные счетчики на основе
 * фискальных документов в хранилище, формирует входные данные для Z-отчета и строит
 * JSON-запрос закрытия смены для ОФД.
 */
// Регистрируется и используется динамически через список стратегий сборщика запросов / DI
@Suppress("unused", "DuplicatedCode")
class CloseShiftRequestBuilderStrategy(
    private val storage: StoragePort,
    private val recalculateShiftCountersUseCase: RecalculateShiftCountersUseCase
) : OfdRequestBuilderStrategy {

    /**
     * Проверяет, является ли переданный тип команды [commandType] операцией закрытия смены.
     *
     * @param commandType тип команды ОФД.
     * @return `true`, если тип команды [OfdCommandType.CLOSE_SHIFT], иначе `false`.
     */
    override fun canHandle(commandType: OfdCommandType): Boolean =
        commandType == OfdCommandType.CLOSE_SHIFT

    /**
     * Строит JSON-запрос для закрытия смены на основе параметров команды и конфигурации ОФД.
     *
     * @param command запрос команды ОФД [OfdCommandRequest].
     * @param config настройки протокола ОФД [OfdConfig].
     * @return JSON-объект [JsonObject] запроса закрытия смены или `null`, если отсутствуют необходимые данные.
     */
    override fun build(command: OfdCommandRequest, config: OfdConfig): JsonObject? {
        val serviceBlock = buildServiceBlock(command) ?: return null
        val shift = storage.findOpenShift(command.kkmId)
            ?: storage.listShifts(command.kkmId, limit = 1).firstOrNull()
            ?: return null
        // Для Z-отчета обязательно пересобираем счётчики смены из документов.
        val counters = recalculateShiftCountersUseCase
            .execute(command.kkmId, shift)
        val now = command.offlineEndMillis ?: System.currentTimeMillis()
        val shiftNo = shift.shiftNo.toInt().coerceAtLeast(0)

        val zxInput = ZxReportBuilder.build(
            counters = counters,
            dateTimeMillis = now,
            shiftNumber = shiftNo,
            openShiftTimeMillis = shift.openedAt,
            closeShiftTimeMillis = now
        )

        val zxReport = OfdRequestFactory.buildZxReportInternal(zxInput)

        return OfdRequestFactory.buildCloseShiftRequest(
            ofdId = command.ofdProviderId.lowercase(),
            protocolVersion = config.protocolVersion,
            deviceId = command.deviceId,
            token = command.token,
            reqNum = command.reqNum,
            closeTimeMillis = now,
            frShiftNumber = shiftNo,
            zxReport = zxReport,
            serviceBlock = serviceBlock
        )
    }

    /**
     * Формирует служебный JSON-блок (payload) с регистрационной информацией и геолокацией.
     *
     * @param command запрос команды ОФД.
     * @return JSON-объект [JsonObject] со служебной информацией или `null`, если параметры неполны.
     */
    private fun buildServiceBlock(command: OfdCommandRequest): JsonObject? {
        val serviceInfo = command.serviceInfo ?: return null
        val regNo = command.registrationNumber ?: return null
        val factoryNo = command.factoryNumber ?: return null
        val systemId = command.ofdSystemId ?: return null
        val begin = command.offlineBeginMillis ?: System.currentTimeMillis()
        val end = command.offlineEndMillis ?: System.currentTimeMillis()
        return OfdRequestFactory.buildServicePayload(
            serviceInfo = serviceInfo,
            registrationNumber = regNo,
            factoryNumber = factoryNo,
            systemId = systemId,
            offlineBeginMillis = begin,
            offlineEndMillis = end
        )
    }
}
