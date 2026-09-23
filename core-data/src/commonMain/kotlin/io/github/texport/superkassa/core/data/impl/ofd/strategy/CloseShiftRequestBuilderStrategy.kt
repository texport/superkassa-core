package io.github.texport.superkassa.core.data.impl.ofd.strategy

import io.github.texport.superkassa.core.data.impl.ofd.OfdConfig
import io.github.texport.superkassa.core.data.impl.ofd.OfdRequestFactory
import io.github.texport.superkassa.core.domain.impl.helper.zxreport.ZxReportBuilder
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandRequest
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo
import io.github.texport.superkassa.core.domain.api.model.shift.isWithdrawalInZReport
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.usecase.shift.RecalculateShiftCountersUseCase
import kotlinx.serialization.json.JsonObject

/**
 * Стратегия построения запросов для COMMAND_CLOSE_SHIFT (Z-отчет, закрытие смены).
 *
 * Стратегия берёт смену закрываемого документа, пересчитывает сменные счётчики
 * по фискальным документам хранилища, формирует входные данные Z-отчёта и строит
 * JSON-запрос закрытия смены для ОФД.
 */
// Регистрируется и используется динамически через список стратегий сборщика запросов / DI
class CloseShiftRequestBuilderStrategy(
    private val storage: StoragePort,
    private val recalculateShiftCountersUseCase: RecalculateShiftCountersUseCase
) : OfdRequestBuilderStrategy {

    /**
     * Смена закрываемого документа: своя у него, иначе открытая.
     *
     * Досылка закрытия после обрыва связи приходит сюда, когда касса успела
     * открыть следующую смену: по открытой смене собирался чужой отчёт —
     * с её номером и её счётчиками, — и ОФД получал Z-отчёт на смену,
     * которая только началась, а прежняя не закрывалась вовсе.
     *
     * Своей смены нет у документов, оформленных прежними версиями кассы:
     * для них остаётся прежний путь — открытая смена, иначе последняя.
     */
    private fun shiftOf(document: FiscalDocumentSnapshot?, kkmId: String): ShiftInfo? {
        val own = document?.shiftId?.takeIf { it.isNotBlank() && it != NO_SHIFT }
        return own?.let { storage.findShiftById(it) }
            ?: storage.findOpenShift(kkmId)
            ?: storage.listShifts(kkmId, limit = 1).firstOrNull()
    }

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
     * Счётчики смены пересобираются из документов. Отчёт, оформленный
     * при оборванной связи, сообщает об этом признаком автономности.
     *
     * @param command запрос команды ОФД [OfdCommandRequest].
     * @param config настройки протокола ОФД [OfdConfig].
     * @return JSON-объект [JsonObject] запроса закрытия смены или `null`, если отсутствуют необходимые данные.
     */
    override fun build(command: OfdCommandRequest, config: OfdConfig): JsonObject? {
        val serviceBlock = buildServiceBlock(command) ?: return null
        val document = storage.findFiscalDocumentById(command.payloadRef)
        val shift = shiftOf(document, command.kkmId) ?: return null
        val counters = recalculateShiftCountersUseCase.execute(command.kkmId, shift)
        // Время закрытия — время документа: то же, что у смены и на бумаге.
        // Досылка из очереди прежде ставила время отправки.
        val closedAt = document?.createdAt ?: command.offlineEndMillis ?: kotlin.time.Clock.System.now().toEpochMilliseconds()
        val shiftNo = shift.shiftNo.toInt().coerceAtLeast(0)
        val zxInput = ZxReportBuilder.build(counters, closedAt, shiftNo, shift.openedAt, closedAt)
        return OfdRequestFactory.buildCloseShiftRequest(
            ofdId = command.ofdProviderId.lowercase(),
            protocolVersion = config.protocolVersion,
            deviceId = command.deviceId,
            token = command.token,
            reqNum = command.reqNum,
            closeTimeMillis = closedAt,
            frShiftNumber = shiftNo,
            zxReport = OfdRequestFactory.buildZxReportInternal(zxInput),
            serviceBlock = serviceBlock,
            isOffline = document?.isAutonomous ?: false,
            printedDocumentNumber = document?.printedDocumentNumber,
            withdrawMoney = hasWithdrawalInZReport(shift)
        )
    }

    /**
     * Изъятие при закрытии записано в смене — значит, БФД изымает остаток
     * вместе с Z-отчётом. Отчёт кассы это изъятие уже несёт в счётчиках.
     */
    private fun hasWithdrawalInZReport(shift: ShiftInfo): Boolean {
        var offset = 0
        while (true) {
            val page = storage.listFiscalDocumentsByShift(shift.kkmId, shift.id, limit = PAGE, offset = offset)
            if (page.isEmpty()) return false
            if (page.any { it.isWithdrawalInZReport() }) return true
            offset += PAGE
        }
    }
}

/** Признак «смены нет» в снимке документа. */
private const val NO_SHIFT = "0"

/** Страница документов смены при поиске изъятия. */
private const val PAGE = 500
