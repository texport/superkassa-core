package io.github.texport.superkassa.core.data.impl.ofd.strategy

import io.github.texport.superkassa.core.data.impl.ofd.OfdConfig
import io.github.texport.superkassa.core.data.impl.ofd.OfdRequestFactory
import io.github.texport.superkassa.core.domain.impl.helper.zxreport.ZxReportBuilder
import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandRequest
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.usecase.shift.RecalculateShiftCountersUseCase
import io.github.texport.superkassa.core.domain.api.model.zxreport.ZxReportInput
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
     * Смена снятого отчёта: своя у документа, иначе открытая.
     *
     * Своей смены нет у отчётов, снятых прежними версиями кассы, — для них
     * остаётся прежний путь. Отчёт, снятый вне смены, не собирается вовсе:
     * счётчики брать неоткуда.
     */
    private fun shiftOf(document: FiscalDocumentSnapshot?, kkmId: String): ShiftInfo? {
        val own = document?.shiftId?.takeIf { it.isNotBlank() && it != NO_SHIFT }
        return own?.let { storage?.findShiftById(it) } ?: storage?.findOpenShift(kkmId)
    }

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
        val document = storage.findFiscalDocumentById(command.payloadRef)
        // Смена берётся та, в которой отчёт сняли, а не та, что открыта сейчас.
        // Прежде отчёт собирался по открытой смене, и досылка после
        // её закрытия собрать запрос уже не могла: задача уходила
        // в отбраковку, документ навсегда оставался неотправленным,
        // а очередь при этом показывала ноль.
        val shift = shiftOf(document, command.kkmId) ?: return null
        return OfdRequestFactory.buildReportRequest(
            ofdId = command.ofdProviderId.lowercase(),
            protocolVersion = config.protocolVersion,
            deviceId = command.deviceId,
            token = command.token,
            reqNum = command.reqNum,
            reportType = "REPORT_X",
            zxReport = reportOf(command, document, shift),
            serviceBlock = serviceBlock,
            // Отчёт, оформленный при оборванной связи, обязан это сообщать.
            isOffline = document?.isAutonomous ?: false,
            printedDocumentNumber = document?.printedDocumentNumber
        )
    }

    /**
     * Итоги и время — на момент документа, как на его бумаге: досылка
     * из очереди прежде отправляла итоги и время самой отправки.
     * Счётчики смены при этом сверяются с документами, как и прежде.
     */
    private fun reportOf(
        command: OfdCommandRequest,
        document: FiscalDocumentSnapshot?,
        shift: ShiftInfo
    ): ZxReportInput {
        val current = recalculateShiftCountersUseCase.execute(command.kkmId, shift)
        val counters = document?.let {
            recalculateShiftCountersUseCase.rebuildShiftCounters(
                command.kkmId,
                shift,
                it.createdAt
            )
        }
        val takenAt = document?.createdAt ?: command.offlineEndMillis ?: kotlin.time.Clock.System.now().toEpochMilliseconds()
        return ZxReportBuilder.build(
            counters = counters ?: current,
            dateTimeMillis = takenAt,
            shiftNumber = shift.shiftNo.toInt().coerceAtLeast(0),
            openShiftTimeMillis = shift.openedAt,
            closeShiftTimeMillis = null
        )
    }
}

/** Чем помечен отчёт, снятый вне смены: смены у него нет. */
private const val NO_SHIFT = "0"
