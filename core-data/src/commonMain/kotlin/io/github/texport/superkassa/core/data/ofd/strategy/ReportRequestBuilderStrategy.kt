package io.github.texport.superkassa.core.data.ofd.strategy

import io.github.texport.superkassa.core.data.ofd.OfdConfig
import io.github.texport.superkassa.core.data.ofd.OfdRequestFactory
import io.github.texport.superkassa.core.domain.helper.zxreport.ZxReportBuilder
import io.github.texport.superkassa.core.domain.model.ofd.OfdCommandRequest
import io.github.texport.superkassa.core.domain.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.port.StoragePort
import io.github.texport.superkassa.core.domain.usecase.shift.RecalculateShiftCountersUseCase
import kotlinx.serialization.json.JsonObject

/**
 * Стратегия построения запросов для COMMAND_REPORT (X-отчет без закрытия смены).
 *
 * Стратегия находит текущую смену ККМ, пересчитывает её счетчики на основе документов в хранилище,
 * подготавливает структуру сменного отчета X-типа и формирует JSON-запрос отчета для ОФД.
 */
// Регистрируется и используется динамически через список стратегий сборщика запросов / DI
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
        val now = command.offlineEndMillis ?: kotlin.time.Clock.System.now().toEpochMilliseconds()
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
}
