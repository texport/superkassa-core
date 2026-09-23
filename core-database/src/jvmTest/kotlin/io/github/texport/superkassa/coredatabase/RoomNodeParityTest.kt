package io.github.texport.superkassa.coredatabase

import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.queue.QueueTask
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptBranding
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptLanguage
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType
import io.github.texport.superkassa.core.domain.api.model.receipt.TicketAd
import io.github.texport.superkassa.coredatabase.RoomFile.Companion.receipt
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Хранилище Room отвечает на вызовы порта так же, как хранилище узла,
 * там, где от ответа зависит фискальный документ или его отправка.
 */
class RoomNodeParityTest {
    private val room = RoomFile()

    @AfterTest
    fun cleanUp() = room.delete()

    @Test
    fun `оформление чека переживает правку кассы и перезапуск`() {
        room.session { storage ->
            storage.createKkm(KKM_INFO.copy(branding = BRANDING))
            storage.updateKkm(checkNotNull(storage.findKkm(KKM)).copy(autoCloseShift = true, updatedAt = 2))
        }

        assertEquals(BRANDING, room.session { it.findKkm(KKM)?.branding })
    }

    @Test
    fun `повторная регистрация узнаётся по номеру кассы в ОФД`() = room.session { storage ->
        storage.createKkm(KKM_INFO)

        assertEquals(KKM, storage.findKkmBySystemId("4100")?.id)
    }

    @Test
    fun `повторная постановка документа в очередь не сбрасывает отправленную команду`() = room.session { storage ->
        storage.enqueueQueueTask(task(createdAt = 10))
        storage.updateQueueTaskStatus(TASK, "SENT", 1, null, null)

        assertFalse(storage.enqueueQueueTask(task(createdAt = 99)))

        val kept = storage.listQueueTasksByCashbox(KKM, "OFFLINE", 10, 0).single()
        assertEquals("SENT" to 10L, kept.status to kept.createdAt)
    }

    @Test
    fun `захват команды отмечает время и не сбрасывает число попыток`() = room.session { storage ->
        storage.enqueueQueueTask(task(createdAt = 10).copy(status = "FAILED", attempt = 3, nextAttemptAt = 20))

        storage.markQueueTaskInProgress(TASK, now = 500)

        val claimed = storage.listQueueTasksByCashbox(KKM, "OFFLINE", 10, 0).single()
        assertEquals(Triple("IN_PROGRESS", 3, 500L), Triple(claimed.status, claimed.attempt, claimed.nextAttemptAt))
    }

    @Test
    fun `аренда очереди у одного обработчика, пока не истекла`() = room.session { storage ->
        assertTrue(storage.tryAcquireQueueLock(KKM, "owner-1", leaseUntil = 1_000, acquiredAt = 0))

        assertFalse(storage.tryAcquireQueueLock(KKM, "owner-2", leaseUntil = 2_000, acquiredAt = 10))
        assertFalse(storage.renewQueueLock(KKM, "owner-2", leaseUntil = 2_000, now = 10))
        assertTrue(storage.tryAcquireQueueLock(KKM, "owner-2", leaseUntil = 3_000, acquiredAt = 1_001))
        assertFalse(storage.releaseQueueLock(KKM, "owner-1"))
        assertTrue(storage.releaseQueueLock(KKM, "owner-2"))
    }

    @Test
    fun `без сохранённого чека документ чеком не отдаётся`() = room.session { storage ->
        storage.saveCashOperation(KKM, "CASH_IN", Money.fromTiyn(123_456), "d-in", "s-1", createdAt = 1)

        assertNull(storage.findFiscalDocumentWithReceiptPayload("d-in"))
        assertNull(storage.findFiscalDocumentWithReceiptPayload("missing"))
    }

    @Test
    fun `принятый после отказа документ не несёт причины отказа`() = room.session { storage ->
        val sale = receipt(KKM, ReceiptOperationType.SELL, 150_050, "k-1").toReceiptRequest()
        storage.saveReceipt(sale, "d-sale", "s-1", createdAt = 1)
        storage.updateReceiptStatus("d-sale", null, null, "FAILED", 15, null, false, "Отказ ОФД")

        storage.updateReceiptStatus("d-sale", "FP-1", null, "SENT", null, 5, false, null)

        val accepted = checkNotNull(storage.findFiscalDocumentById("d-sale"))
        assertEquals(listOf("SENT", "FP-1", null, null), listOf(accepted.ofdStatus, accepted.fiscalSign, accepted.ofdErrorCode, accepted.ofdErrorText))
    }

    private fun task(createdAt: Long) = QueueTask(TASK, KKM, "OFFLINE", "TICKET", "d-1", createdAt, "PENDING", 0, null, null)

    private companion object {
        const val KKM = "kkm-1"
        const val TASK = "kkm-1:COMMAND_TICKET:d-1"
        val KKM_INFO = KkmInfo(id = KKM, createdAt = 1, updatedAt = 1, mode = "REGISTRATION", state = "ACTIVE", systemId = "4100")
        val BRANDING = ReceiptBranding(
            language = ReceiptLanguage.KK, headerLogoUrl = "https://example.kz/logo.png", paperWidthMm = 58, themeColor = "teal",
            beforeHeaderMsg = "a", headerMsg = "Рахмет!", afterHeaderMsg = "b", beforeItemsMsg = "c", afterItemsMsg = "d",
            beforeTotalsMsg = "e", afterTotalsMsg = "f", beforeQrMsg = "g", footerMsg = "Қайта келіңіз", useForceDarkTheme = true,
            customBackgroundColorHex = "#101010", customCardTopBorderColorHex = "#202020",
            ofdTicketAds = listOf(TicketAd("TICKET_AD_OFD", 3, "Жарнама")), printOfdTicketAds = false
        )
    }
}
