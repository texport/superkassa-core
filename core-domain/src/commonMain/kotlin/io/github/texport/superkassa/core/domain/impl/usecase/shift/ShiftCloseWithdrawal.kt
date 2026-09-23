package io.github.texport.superkassa.core.domain.impl.usecase.shift

import io.github.texport.superkassa.core.domain.api.model.common.CounterKeyFormats
import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.kkm.CashOperationType
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo
import io.github.texport.superkassa.core.domain.api.model.shift.WITHDRAWAL_IN_Z_REPORT_STATUS
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.api.port.internal.IdGeneratorPort
import io.github.texport.superkassa.core.domain.impl.helper.common.assignPrintedDocumentNumber

/**
 * Изъятие всей наличности при закрытии смены (настройка «автоизъятие»).
 *
 * По протоколу это признак `withdraw_money` внутри запроса закрытия смены:
 * БФД изымает остаток в закрываемой смене и только потом её закрывает
 * (референс: `OperationCalculator.closeShift`). Прежде изъятие уходило
 * отдельной командой после Z-отчёта — БФД проводил его уже в следующей
 * смене, Z-отчёт у БФД показывал остаток, а бумажный — ноль.
 *
 * Документ изъятия остаётся в журнале и в счётчиках смены, поэтому
 * Z-отчёт кассы и бумага показывают то же, что у БФД. Судьба документа
 * — судьба Z-отчёта: отклонён отчёт — отклонено и изъятие, и деньги
 * остаются в ящике.
 */
class ShiftCloseWithdrawal(
    private val storage: StoragePort,
    private val idGenerator: IdGeneratorPort,
    private val recalculate: RecalculateShiftCountersUseCase
) {
    /**
     * Записывает изъятие остатка, если оно включено и в ящике есть наличные.
     *
     * @return идентификатор документа изъятия или `null`, если изымать нечего.
     */
    fun record(kkm: KkmInfo, shift: ShiftInfo, now: Long): String? {
        if (!kkm.autoCashout) return null
        val cash = recalculate.execute(kkm.id, shift)[CounterKeyFormats.CASH_SUM] ?: 0L
        if (cash <= 0L) return null
        val id = idGenerator.nextId()
        storage.saveCashOperation(kkm.id, CashOperationType.CASH_OUT.name, Money.fromTiyn(cash), id, shift.id, now)
        assignPrintedDocumentNumber(storage, kkm.id, id)
        settle(id, shift, WITHDRAWAL_IN_Z_REPORT_STATUS, autonomous = false)
        return id
    }

    /** Z-отчёт принят или ушёл в автономную очередь: изъятие проведено вместе с ним. */
    fun confirm(documentId: String?, shift: ShiftInfo, autonomous: Boolean) {
        documentId?.let { settle(it, shift, WITHDRAWAL_IN_Z_REPORT_STATUS, autonomous) }
    }

    /** Z-отчёт отклонён: изъятия не было, остаток возвращается в ящик. */
    fun cancel(documentId: String?, shift: ShiftInfo) {
        documentId?.let { settle(it, shift, REJECTED, autonomous = false) }
    }

    private fun settle(documentId: String, shift: ShiftInfo, status: String, autonomous: Boolean) {
        storage.updateReceiptStatus(
            documentId = documentId,
            fiscalSign = null,
            autonomousSign = null,
            ofdStatus = status,
            deliveredAt = null,
            isAutonomous = autonomous
        )
        recalculate.execute(shift.kkmId, shift)
    }

    private companion object {
        /** Документ не стал фискальным: в счётчики и в ящик он не входит. */
        const val REJECTED = "FAILED"
    }
}
