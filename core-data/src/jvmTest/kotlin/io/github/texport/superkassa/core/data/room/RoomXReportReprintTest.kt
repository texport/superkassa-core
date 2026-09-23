package io.github.texport.superkassa.core.data.room

import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.ADMIN_PIN
import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.CASHIER_PIN
import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.KKM
import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.cash
import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.item
import io.github.texport.superkassa.core.presentation.api.model.receipt.PrintDocumentType
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptSellRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours

/**
 * Перепечатка X-отчёта — копия выданного документа: итоги и время те,
 * что были в нём, а не нынешние.
 */
class RoomXReportReprintTest {
    private val clock = ManualClock()
    private val kassa = RoomKassa(clock = clock)

    @Test
    fun `перепечатка X-отчёта не меняется от продаж после него и от хода часов`() {
        kassa.api.openShift(KKM, ADMIN_PIN)
        sell("1500", "sale-1")
        val report = kassa.api.createReport(KKM, CASHIER_PIN).documentId
        val issued = reprint(report)

        clock.advance(2.hours)
        sell("700", "sale-2")

        assertEquals(issued, reprint(report))
    }

    private fun reprint(documentId: String): String =
        kassa.api.getPrintHtml(KKM, PrintDocumentType.DOCUMENT, documentId, null, CASHIER_PIN)

    private fun sell(total: String, key: String) {
        kassa.api.createSellReceipt(
            KKM, CASHIER_PIN,
            ReceiptSellRequest(idempotencyKey = key, items = listOf(item(total)), payments = listOf(cash(total)))
        )
    }
}
