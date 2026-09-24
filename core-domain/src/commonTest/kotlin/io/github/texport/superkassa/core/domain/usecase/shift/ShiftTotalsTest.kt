package io.github.texport.superkassa.core.domain.impl.usecase.shift

import io.github.texport.superkassa.core.domain.api.model.common.CounterKeyFormats
import io.github.texport.superkassa.core.domain.api.model.common.CounterScopes
import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.common.format
import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.receipt.PaymentType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptItem
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptPayment
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftStatus
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.support.TestStoragePort
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Счёт чеков и операций с деньгами «за всё время» переходит из смены в смену,
 * как у БФД. Смена, открытая прежней версией кассы, счёта на начало не несёт:
 * он складывается из документов прошлых смен, и пересчёт смены даёт то же,
 * что БФД, — позиции в строке операций, скидки поштучно.
 */
class ShiftTotalsTest {
    private val stored = TestStoragePort()

    /** Хранилище, где один чек прошлой смены лежит под типом прежних версий «TICKET». */
    private val storage: StoragePort = object : StoragePort by stored {
        override fun listFiscalDocumentsByShift(kkmId: String, shiftId: String, limit: Int, offset: Int) =
            stored.listFiscalDocumentsByShift(kkmId, shiftId, limit, offset).map(::legacy)
    }
    private val first = shift("shift-1", 1)
    private val second = shift("shift-2", 2)

    init {
        stored.createShift(first)
        stored.createShift(second)
        receipt("sale-1", first, ReceiptOperationType.SELL)
        receipt(LEGACY_RETURN, first, ReceiptOperationType.SELL_RETURN)
        receipt("rejected", first, ReceiptOperationType.SELL, status = "FAILED")
        cash("in-1", first, "CASH_IN", Money(5000, 0))
        cash("out-0", first, "CASH_OUT", Money(0, 0))
        cash("out-1", first, "CASH_OUT", Money(100, 0))
        receipt("sale-2", second, ReceiptOperationType.SELL, discount = Money(10, 0))
    }

    @Test
    fun `смена прежней версии получает счёт на начало из документов прошлых смен`() {
        val start = ShiftTotals(storage).atStart(KKM, second, emptyMap())

        assertEquals(counts(sell = 1, sellReturn = 1, deposit = 1, withdrawal = 1), start)
    }

    @Test
    fun `следующая смена начинает со счёта прошлой смены и её документов`() {
        val start = ShiftTotals(storage).after(KKM, second)

        assertEquals(counts(sell = 2, sellReturn = 1, deposit = 1, withdrawal = 1), start)
    }

    @Test
    fun `записанный при открытии счёт берётся как есть, первая смена начинает с нуля`() {
        val totals = ShiftTotals(storage)
        val recorded = counts(sell = 7, sellReturn = 0, deposit = 3, withdrawal = 0)

        assertEquals(recorded, totals.atStart(KKM, second, recorded + ("cash.sum" to 1L)))
        assertEquals(counts(0, 0, 0, 0), totals.after(KKM, null))
    }

    @Test
    fun `пересчёт смены прежней версии ставит позиции, скидки поштучно и счёт за всё время`() {
        // Прежняя версия записала в строку операций число чеков и не вела скидки поштучно.
        stored.upsertCounter(KKM, CounterScopes.SHIFT, second.id, CounterKeyFormats.OPERATION_COUNT.format(SELL), 1L)

        val rebuilt = RecalculateShiftCountersUseCase(storage).execute(KKM, second)

        assertEquals(2L, rebuilt[CounterKeyFormats.OPERATION_COUNT.format(SELL)])
        assertEquals(1L, rebuilt[CounterKeyFormats.DISCOUNT_COUNT.format(SELL)])
        assertEquals(2L to 1L, rebuilt[CounterKeyFormats.TICKET_TOTAL_COUNT.format(SELL)] to rebuilt[CounterKeyFormats.TICKET_COUNT.format(SELL)])
        assertEquals(1L, stored.loadCounters(KKM, CounterScopes.SHIFT, second.id)[CounterKeyFormats.START_SHIFT_TICKET_TOTAL_COUNT.format(SELL)])
    }

    private fun legacy(document: FiscalDocumentSnapshot) =
        if (document.id == LEGACY_RETURN) document.copy(docType = "TICKET") else document

    private fun receipt(id: String, shift: ShiftInfo, operation: ReceiptOperationType, status: String = "SENT", discount: Money? = null) {
        val goods = listOf(ReceiptItem("Хлеб", "001", 1, Money(500, 0), Money(500, 0)), ReceiptItem("Молоко", "001", 1, Money(300, 0), Money(300, 0)))
        val request = ReceiptRequest(
            kkmId = KKM,
            pin = "1111",
            operation = operation,
            items = goods,
            payments = listOf(ReceiptPayment(PaymentType.CASH, Money(800, 0))),
            total = Money(800, 0),
            idempotencyKey = id,
            discount = discount
        )
        stored.saveReceipt(request, id, shift.id, shift.openedAt + 1)
        stored.updateReceiptStatus(id, null, null, status, deliveredAt = null, isAutonomous = false)
    }

    private fun cash(id: String, shift: ShiftInfo, type: String, amount: Money) {
        stored.saveCashOperation(KKM, type, amount, id, shift.id, shift.openedAt + 2)
        stored.updateReceiptStatus(id, null, null, "SENT", deliveredAt = null, isAutonomous = false)
    }

    private fun shift(id: String, no: Long) = ShiftInfo(id, KKM, no, ShiftStatus.CLOSED, openedAt = no * 1_000L)

    private fun counts(sell: Long, sellReturn: Long, deposit: Long, withdrawal: Long) = mapOf(
        CounterKeyFormats.START_SHIFT_TICKET_TOTAL_COUNT.format(SELL) to sell,
        CounterKeyFormats.START_SHIFT_TICKET_TOTAL_COUNT.format("OPERATION_SELL_RETURN") to sellReturn,
        CounterKeyFormats.START_SHIFT_TICKET_TOTAL_COUNT.format("OPERATION_BUY") to 0L,
        CounterKeyFormats.START_SHIFT_TICKET_TOTAL_COUNT.format("OPERATION_BUY_RETURN") to 0L,
        CounterKeyFormats.START_SHIFT_MONEY_PLACEMENT_TOTAL_COUNT.format("MONEY_PLACEMENT_DEPOSIT") to deposit,
        CounterKeyFormats.START_SHIFT_MONEY_PLACEMENT_TOTAL_COUNT.format("MONEY_PLACEMENT_WITHDRAWAL") to withdrawal
    )

    private companion object {
        const val KKM = "kkm-1"
        const val SELL = "OPERATION_SELL"
        const val LEGACY_RETURN = "return-legacy"
    }
}
