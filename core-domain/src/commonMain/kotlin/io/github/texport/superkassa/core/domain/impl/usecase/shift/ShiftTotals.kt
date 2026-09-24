package io.github.texport.superkassa.core.domain.impl.usecase.shift

import io.github.texport.superkassa.core.domain.api.model.common.CounterKeyFormats
import io.github.texport.superkassa.core.domain.api.model.common.CounterScopes
import io.github.texport.superkassa.core.domain.api.model.common.format
import io.github.texport.superkassa.core.domain.api.model.kkm.CashOperationType
import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.kkm.becameFiscal
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptDocumentTypes
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort

/**
 * Число чеков и операций с деньгами «за всё время» на начало смены.
 *
 * БФД переносит `tickets_total_count` и `operations_total_count` из смены
 * в смену (`OperationCalculator.openShift`), обнуляя только счёт за смену.
 * Прежде касса считала «за всё время» заново в каждой смене, и со второй
 * смены её X/Z расходился с БФД.
 *
 * Счёт записывается при открытии смены. У смены, открытой прежней версией
 * кассы, записи нет — её счёт складывается из документов прошлых смен.
 */
class ShiftTotals(private val storage: StoragePort) {

    /**
     * Счёт на начало [shift]: записанный при открытии или сверкой с БФД,
     * а без записи — сложенный из документов прошлых смен.
     */
    fun atStart(kkmId: String, shift: ShiftInfo, stored: Map<String, Long>): Map<String, Long> =
        if (KEYS.any { it in stored }) KEYS.associateWith { stored[it] ?: 0L } else before(kkmId, shift)

    /** Счёт на начало смены, которая открывается после [previous]; без прошлой смены — нули. */
    fun after(kkmId: String, previous: ShiftInfo?): Map<String, Long> {
        previous ?: return ZERO
        val stored = storage.loadCounters(kkmId, CounterScopes.SHIFT, previous.id)
        return atStart(kkmId, previous, stored).plus(counted(kkmId, previous))
    }

    private fun before(kkmId: String, shift: ShiftInfo): Map<String, Long> =
        allShifts(kkmId).filter { it.shiftNo < shift.shiftNo }.fold(ZERO) { sum, s -> sum.plus(counted(kkmId, s)) }

    private fun allShifts(kkmId: String): List<ShiftInfo> =
        generateSequence(0) { it + PAGE }.map { storage.listShifts(kkmId, PAGE, it) }.takeWhile { it.isNotEmpty() }
            .flatten().toList()

    /** Чеки и операции с деньгами, которые смена [shift] провела: по одной на документ. */
    private fun counted(kkmId: String, shift: ShiftInfo): Map<String, Long> =
        generateSequence(0) { it + PAGE }
            .map { storage.listFiscalDocumentsByShift(kkmId, shift.id, PAGE, it) }
            .takeWhile { it.isNotEmpty() }
            .flatten()
            .filter { it.becameFiscal() }
            .mapNotNull(::keyOf)
            .groupingBy { it }.eachCount()
            .mapValues { it.value.toLong() }

    private fun keyOf(document: FiscalDocumentSnapshot): String? = when (document.docType) {
        in ReceiptDocumentTypes.ALL -> operationOf(document)?.let { START_TICKETS.format(it) }
        CashOperationType.CASH_IN.name -> placement(document, DEPOSIT)
        CashOperationType.CASH_OUT.name -> placement(document, WITHDRAWAL)
        else -> null
    }

    /** Операция с деньгами без суммы в счёт не идёт — как в пересчёте смены. */
    private fun placement(document: FiscalDocumentSnapshot, operation: String): String? =
        START_PLACEMENTS.format(operation).takeIf { (document.totalAmount ?: 0L) != 0L }

    /** Вид операции чека: по типу документа, а у типов прежних версий — по самому чеку. */
    private fun operationOf(document: FiscalDocumentSnapshot): String? = when (document.docType) {
        ReceiptDocumentTypes.SALE -> "OPERATION_SELL"
        ReceiptDocumentTypes.RETURN -> "OPERATION_SELL_RETURN"
        ReceiptDocumentTypes.BUY -> "OPERATION_BUY"
        ReceiptDocumentTypes.BUY_RETURN -> "OPERATION_BUY_RETURN"
        else -> storage.findFiscalDocumentWithReceiptPayload(document.id)?.second?.operation?.let(::operationKey)
    }

    private fun Map<String, Long>.plus(other: Map<String, Long>): Map<String, Long> =
        KEYS.associateWith { (this[it] ?: 0L) + (other[it] ?: 0L) }

    companion object {
        private const val PAGE = 500
        private const val DEPOSIT = "MONEY_PLACEMENT_DEPOSIT"
        private const val WITHDRAWAL = "MONEY_PLACEMENT_WITHDRAWAL"
        private const val START_TICKETS = CounterKeyFormats.START_SHIFT_TICKET_TOTAL_COUNT
        private const val START_PLACEMENTS = CounterKeyFormats.START_SHIFT_MONEY_PLACEMENT_TOTAL_COUNT

        /** Виды операций чека, как их называет протокол. */
        val OPERATIONS: List<String> =
            listOf("OPERATION_SELL", "OPERATION_SELL_RETURN", "OPERATION_BUY", "OPERATION_BUY_RETURN")

        /** Виды операций с деньгами, как их называет протокол. */
        val PLACEMENTS: List<String> = listOf(DEPOSIT, WITHDRAWAL)

        /** Ключи счёта на начало смены: по чекам каждого вида и по операциям с деньгами. */
        val KEYS: List<String> = OPERATIONS.map { START_TICKETS.format(it) } + PLACEMENTS.map { START_PLACEMENTS.format(it) }

        private val ZERO = KEYS.associateWith { 0L }

        /** Вид операции чека, как его называет протокол. */
        fun operationKey(operation: ReceiptOperationType): String = when (operation) {
            ReceiptOperationType.SELL -> "OPERATION_SELL"
            ReceiptOperationType.SELL_RETURN -> "OPERATION_SELL_RETURN"
            ReceiptOperationType.BUY -> "OPERATION_BUY"
            ReceiptOperationType.BUY_RETURN -> "OPERATION_BUY_RETURN"
        }
    }
}
