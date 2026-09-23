package io.github.texport.superkassa.embedded

import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptDocumentTypes
import io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus
import io.github.texport.superkassa.embedded.TestCashRegister.ADMIN_PIN
import io.github.texport.superkassa.embedded.TestCashRegister.CASHIER_PIN
import io.github.texport.superkassa.embedded.TestCashRegister.KKM_ID
import io.github.texport.superkassa.embedded.impl.EmbeddedSuperkassa
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Касса в процессе приложения на чистом каталоге: смена, чек без связи
 * с ОФД, X-отчёт, повтор, перезапуск и второй владелец каталога.
 */
class EmbeddedSuperkassaTest {
    private val dir: File = createTempDirectory("kassa-").toFile()

    @AfterTest
    fun cleanUp() {
        dir.deleteRecursively()
    }

    @Test
    fun `чистый каталог получает базу и настройки`() {
        TestCashRegister.open(dir).use { }

        assertTrue(File(dir, "superkassa.db").isFile)
        assertTrue(File(dir, "core-settings.json").readText().contains("KAZAKHTELECOM"))
    }

    @Test
    fun `чек без связи с ОФД пробивается автономно и ложится в очередь`() {
        val ofd = TestCashRegister.UnreachableOfd()
        TestCashRegister.open(dir, ofd).use { kassa ->
            TestCashRegister.registerKkm(kassa)
            kassa.api.openShift(KKM_ID, ADMIN_PIN)

            val receipt = kassa.api.createSellReceipt(KKM_ID, CASHIER_PIN, TestCashRegister.sale("sale-1"))

            assertEquals(DeliveryStatus.OFFLINE_QUEUED, receipt.deliveryStatus)
            assertNotNull(receipt.autonomousSign)
            assertTrue(ofd.calls > 0)
            assertEquals(1, kassa.queue.listQueue(KKM_ID, ADMIN_PIN).size)
        }
    }

    @Test
    fun `X-отчёт снимается по смене с автономным чеком`() {
        TestCashRegister.open(dir).use { kassa ->
            TestCashRegister.registerKkm(kassa)
            kassa.api.openShift(KKM_ID, ADMIN_PIN)
            kassa.api.createSellReceipt(KKM_ID, CASHIER_PIN, TestCashRegister.sale("sale-1"))

            val report = kassa.api.createReport(KKM_ID, CASHIER_PIN)

            assertTrue(report.documentId.isNotBlank())
        }
    }

    @Test
    fun `повтор с тем же ключом возвращает тот же чек и не добавляет второй`() {
        TestCashRegister.open(dir).use { kassa ->
            TestCashRegister.registerKkm(kassa)
            val shift = kassa.api.openShift(KKM_ID, ADMIN_PIN)

            val first = kassa.api.createSellReceipt(KKM_ID, CASHIER_PIN, TestCashRegister.sale("sale-1"))
            val again = kassa.api.createSellReceipt(KKM_ID, CASHIER_PIN, TestCashRegister.sale("sale-1"))

            assertEquals(first.documentId, again.documentId)
            assertEquals(1, receipts(kassa, shift.id))
            assertEquals(1, kassa.queue.listQueue(KKM_ID, ADMIN_PIN).size)
        }
    }

    @Test
    fun `после перезапуска на том же каталоге смена, чек и очередь на месте`() {
        val shiftId = TestCashRegister.open(dir).use { kassa ->
            TestCashRegister.registerKkm(kassa)
            val shift = kassa.api.openShift(KKM_ID, ADMIN_PIN)
            kassa.api.createSellReceipt(KKM_ID, CASHIER_PIN, TestCashRegister.sale("sale-1"))
            shift.id
        }

        TestCashRegister.open(dir).use { kassa ->
            assertEquals(shiftId, kassa.api.getLocalOpenShift(KKM_ID, CASHIER_PIN)?.id)
            assertEquals(1, receipts(kassa, shiftId))
            assertEquals(1, kassa.queue.listQueue(KKM_ID, ADMIN_PIN).size)
            val again = kassa.api.createSellReceipt(KKM_ID, CASHIER_PIN, TestCashRegister.sale("sale-1"))
            assertEquals(1, receipts(kassa, shiftId), "a replay after restart must not fiscalize twice: ${again.documentId}")
        }
    }

    @Test
    fun `попытка досылки без связи оставляет документ в очереди`() {
        TestCashRegister.open(dir).use { kassa ->
            TestCashRegister.registerKkm(kassa)
            kassa.api.openShift(KKM_ID, ADMIN_PIN)
            kassa.api.createSellReceipt(KKM_ID, CASHIER_PIN, TestCashRegister.sale("sale-1"))

            assertEquals(1, kassa.sendQueueNow(), "one attempt is made")
            val left = kassa.queue.listQueue(KKM_ID, ADMIN_PIN).single()
            assertTrue(left.status != "SENT", "the document stays queued: ${left.status}")
        }
    }

    @Test
    fun `второй экземпляр на том же каталоге получает отказ`() {
        TestCashRegister.open(dir).use {
            assertFailsWith<IllegalStateException> { TestCashRegister.open(dir) }
        }
        TestCashRegister.open(dir).use { }
    }

    @Test
    fun `настройки без базы — отказ, а не пустая касса`() {
        TestCashRegister.open(dir).use { }
        File(dir, "superkassa.db").delete()

        val refusal = assertFailsWith<IllegalStateException> { TestCashRegister.open(dir) }

        assertTrue(refusal.message.orEmpty().contains("superkassa.db"))
    }

    private fun receipts(kassa: EmbeddedSuperkassa, shiftId: String): Int =
        kassa.api.listShiftDocuments(KKM_ID, shiftId, 100, 0, CASHIER_PIN)
            .count { it.docType == ReceiptDocumentTypes.SALE }
}
