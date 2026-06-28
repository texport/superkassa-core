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
 * Стратегия построения запросов для COMMAND_REPORT (X-отчет без закрытия смены).
 *
 * Стратегия находит текущую смену ККМ, пересчитывает её счетчики на основе документов в хранилище,
 * подготавливает структуру сменного отчета X-типа и формирует JSON-запрос отчета для ОФД.
 */
// Регистрируется и используется динамически через список стратегий сборщика запросов / DI
@Suppress("unused", "DuplicatedCode")
class ReportRequestBuilderStrategy(
    private val storage: StoragePort,
    private val recalculateShiftCountersUseCase: RecalculateShiftCountersUseCase
) : OfdRequestBuilderStrategy {

    /**
     * Проверяет, может ли стратегия обработать указанный тип команды [commandType].
     *
     * @param commandType тип команды ОФД.
     * @return `true`, если тип команды [OfdCommandType.REPORT], иначе `false`.
     */
    override fun canHandle(commandType: OfdCommandType): Boolean =
        commandType == OfdCommandType.REPORT

    /**
     * Строит JSON-запрос для генерации X-отчета на основе параметров команды и конфигурации ОФД.
     *
     * @param command запрос команды ОФД [OfdCommandRequest].
     * @param config настройки протокола ОФД [OfdConfig].
     * @return JSON-объект [JsonObject] запроса X-отчета или `null`, если отсутствуют необходимые данные.
     */
    override fun build(command: OfdCommandRequest, config: OfdConfig): JsonObject? {
        val serviceBlock = buildServiceBlock(command) ?: return null
        val shift = storage.findOpenShift(command.kkmId) ?: return null
        // Для X-отчета пересобираем счётчики смены из документов,
        // чтобы zxReport опирался на консистентные данные.
        val counters = recalculateShiftCountersUseCase
            .execute(command.kkmId, shift)
        val now = command.offlineEndMillis ?: System.currentTimeMillis()
        val shiftNo = shift.shiftNo.toInt().coerceAtLeast(0)
        val zxInput = ZxReportBuilder.build(
            counters = counters,
            dateTimeMillis = now,
            shiftNumber = shiftNo,
            openShiftTimeMillis = shift.openedAt,
            closeShiftTimeMillis = null
        )

        return OfdRequestFactory.buildReportRequest(
            ofdId = command.ofdProviderId.lowercase(),
            protocolVersion = config.protocolVersion,
            deviceId = command.deviceId,
            token = command.token,
            reqNum = command.reqNum,
            reportType = "REPORT_X",
            zxReport = zxInput,
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
