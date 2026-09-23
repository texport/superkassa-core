package io.github.texport.superkassa.core.domain.impl.usecase.shift

import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmState
import io.github.texport.superkassa.core.domain.api.model.report.ReportResult
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.api.port.integration.inTransaction
import io.github.texport.superkassa.core.domain.impl.logging.getLogger

/**
 * Автозакрытие смены до предела в сутки (настройка кассы «автозакрытие»).
 *
 * Правила: настройка включена; касса работает — не в программировании
 * и не заблокирована; смена открыта и до её предела осталось меньше
 * запаса [ShiftDayLimit.AUTO_CLOSE_LEAD_MILLIS] (или он пройден);
 * Z-отчёт этой смены БФД ещё не отклонял.
 *
 * Закрытие то же, что у кассира ([CloseShiftUseCase]): отказ БФД смену
 * не закрывает, таймаут закрывает её автономно. Отклонённый Z-отчёт
 * автозакрытие не повторяет: причину отказа разбирает человек, а
 * одинаковая отправка раз за разом только множит отказы в журнале.
 *
 * Когда звать — решает хозяин процесса: встраиваемая касса проверяет
 * по часам, узел — своим планировщиком.
 */
class AutoCloseShiftUseCase(
    private val storage: StoragePort,
    private val dayLimit: ShiftDayLimit,
    private val closeShift: CloseShiftUseCase
) {
    private val logger = getLogger(AutoCloseShiftUseCase::class)

    /** @return итог закрытия или `null`, если закрывать сейчас нечего. */
    fun execute(kkmId: String): ReportResult? = storage.inTransaction {
        val kkm = storage.findKkmForUpdate(kkmId)?.takeIf { it.autoCloseShift && it.isWorking() }
        val shift = kkm?.let { storage.findOpenShift(kkmId) }
        if (shift == null || !dayLimit.isDueForAutoClose(shift) || wasRejected(shift)) {
            null
        } else {
            logger.info("Closing shift {} of cashbox {} before the day limit", shift.shiftNo, kkmId)
            closeShift.executeBySystem(kkmId)
        }
    }

    private fun KkmInfo.isWorking(): Boolean =
        state != KkmState.PROGRAMMING.name && state != KkmState.BLOCKED.name

    private fun wasRejected(shift: ShiftInfo): Boolean {
        var offset = 0
        while (true) {
            val page = storage.listFiscalDocumentsByShift(shift.kkmId, shift.id, limit = PAGE, offset = offset)
            if (page.isEmpty()) return false
            if (page.any { it.docType == Z_REPORT && it.ofdStatus == REJECTED }) return true
            offset += PAGE
        }
    }

    private companion object {
        const val PAGE = 500
        const val Z_REPORT = "SHIFT_CLOSE"
        const val REJECTED = "FAILED"
    }
}
