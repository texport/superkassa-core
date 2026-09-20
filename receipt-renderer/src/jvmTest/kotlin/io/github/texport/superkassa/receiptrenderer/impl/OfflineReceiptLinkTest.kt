package io.github.texport.superkassa.receiptrenderer.impl

import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.receiptrenderer.impl.renderer.ticket.OfflineReceiptLink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OfflineReceiptLinkTest {

    @Test
    fun `ссылка собирается из номера документа, регистрационного номера, суммы и времени`() {
        val link = OfflineReceiptLink.of(offlineDocument(), TOTAL_TIYN)

        assertEquals(
            "https://receipt.ecc.kz?i=5&f=KGD-2000302&s=450.00&t=20260902T190838",
            link
        )
    }

    @Test
    fun `без регистрационного номера ссылки нет`() {
        // Ссылка без регистрационного номера ведёт в никуда: сочинять её,
        // лишь бы напечатать код, значит выдать покупателю неработающий QR.
        assertNull(OfflineReceiptLink.of(offlineDocument().copy(registrationNumber = null), TOTAL_TIYN))
    }

    @Test
    fun `у неизвестного провайдера домена проверки нет`() {
        assertNull(OfflineReceiptLink.of(offlineDocument().copy(ofdProvider = "NOBODY:DEV"), TOTAL_TIYN))
    }

    private fun offlineDocument(): FiscalDocumentSnapshot = FiscalDocumentSnapshot(
        id = "doc-1",
        cashboxId = "kkm-1",
        shiftId = "shift-1",
        docType = "SALE",
        docNo = 5L,
        shiftNo = 3L,
        createdAt = CREATED_AT,
        totalAmount = TOTAL_TIYN,
        currency = "KZT",
        fiscalSign = null,
        autonomousSign = "1788358119061",
        isAutonomous = true,
        ofdStatus = "PENDING",
        deliveredAt = null,
        receiptUrl = null,
        registrationNumber = "KGD-2000302",
        ofdProvider = "BFD:DEV"
    )

    private companion object {
        /** 02.09.2026 19:08:38 по часам машины, на которой печатается чек. */
        val CREATED_AT: Long = java.time.LocalDateTime.of(2026, 9, 2, 19, 8, 38)
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        const val TOTAL_TIYN = 45_000L
    }
}
