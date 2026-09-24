package io.github.texport.superkassa.testing.api.kassa

import io.github.texport.superkassa.core.presentation.api.model.kkm.VatGroup
import io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus
import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.string.api.TrilingualMessage
import io.github.texport.superkassa.testing.api.kassa.BenchDirectory.Companion.ADMIN_PIN
import io.github.texport.superkassa.testing.api.kassa.BenchDirectory.Companion.CASHIER_PIN
import io.github.texport.superkassa.testing.api.kassa.BenchDirectory.Companion.NOT_PAYER
import io.github.texport.superkassa.testing.impl.kassa.Receipts
import kz.kazakhtelecom.proto.v203.OperationTypeEnum
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Касса стенда на встраиваемой сборке и БФД внутри процесса: смена, чеки,
 * деньги, очередь досылки и повтор.
 */
class ReadyKassaTest {
    private val directory = BenchDirectory()
    private val bench = directory.open()

    @AfterTest
    fun tearDown() {
        bench.close()
        directory.close()
    }

    @Test
    fun `касса продаёт, и БФД учитывает чек своим номером`() {
        val kassa = bench.registerKassa(NOT_PAYER).also { it.openShift() }

        val sale = kassa.sell()

        assertEquals(DeliveryStatus.ONLINE_OK, sale.deliveryStatus, sale.deliveryError)
        assertEquals(1, bench.bfd.countedTickets().size)
        val document = kassa.api.getDocumentDetails(kassa.kkmId, sale.documentId, ADMIN_PIN).document
        assertEquals(9001L, document.docNo)
    }

    @Test
    fun `смена закрывается Z-отчётом, принятым БФД`() {
        val kassa = bench.registerKassa(NOT_PAYER).also { it.openShift() }
        kassa.sell()
        kassa.xReport()

        val z = kassa.closeShift()

        assertEquals(DeliveryStatus.ONLINE_OK, z.deliveryStatus, z.deliveryError)
        assertEquals(1, bench.bfd.closeShifts().size)
        assertEquals(1, bench.bfd.xReports().size)
        assertEquals(false, kassa.info().isShiftOpen)
    }

    @Test
    fun `чеки со скидкой, возврат и НДС на чек и по позициям принимаются БФД`() {
        val kassa = bench.registerKassa(NOT_PAYER.copy(vat = VatMode.Payer(VatGroup.VAT_16))).also { it.openShift() }

        val receipts = listOf(
            kassa.sellWithDiscount(),
            kassa.refund(kassa.sell()),
            kassa.sellWithReceiptVat(VatGroup.VAT_16),
            kassa.sellWithItemVat(VatGroup.VAT_16, VatGroup.NO_VAT)
        )

        assertEquals(List(receipts.size) { DeliveryStatus.ONLINE_OK }, receipts.map { it.deliveryStatus }, receipts.toString())
        val tickets = bench.bfd.countedTickets()
        assertEquals(100L, tickets[0].amounts.discount?.sum?.bills, "discount of 100 tenge")
        assertEquals(1, tickets[3].taxes.size + tickets[3].items.count { it.commodity?.taxes.orEmpty().isNotEmpty() })
    }

    @Test
    fun `внесение и изъятие доходят до БФД`() {
        val kassa = bench.registerKassa(NOT_PAYER).also { it.openShift() }

        val placed = listOf(kassa.cashIn(), kassa.cashOut())

        assertEquals(listOf(DeliveryStatus.ONLINE_OK, DeliveryStatus.ONLINE_OK), placed.map { it.deliveryStatus })
        assertEquals(mapOf(1 to 100_000L), bench.bfd.withdrawnByShift(kassa.systemId))
    }

    @Test
    fun `автономный чек досылается после восстановления связи и учитывается один раз`() {
        val kassa = bench.registerKassa(NOT_PAYER).also { it.openShift() }
        val offline = kassa.offlineSale()
        val queued = kassa.info().offlineQueueCount

        val resent = kassa.resendQueue()

        assertEquals(DeliveryStatus.OFFLINE_QUEUED to 1, offline.deliveryStatus to queued)
        assertEquals(1, resent)
        assertEquals(0, kassa.info().offlineQueueCount)
        assertEquals(1, bench.bfd.countedTickets().size)
    }

    @Test
    fun `повтор чека с тем же ключом не удваивает его ни в кассе, ни в БФД`() {
        val kassa = bench.registerKassa(NOT_PAYER).also { it.openShift() }
        val request = Receipts.sale("500.00", "1")

        val first = kassa.sell(request)
        val again = kassa.sell(request)

        assertEquals(first.documentId, again.documentId)
        assertEquals(1, bench.bfd.countedTickets().size)
    }

    @Test
    fun `повтор полного возврата после потерянного ответа получает свой документ, и возврат в БФД один`() {
        val kassa = bench.registerKassa(NOT_PAYER).also { it.openShift() }
        val sale = kassa.sell()
        val refund = Receipts.refund(kassa.api.getDocumentDetails(kassa.kkmId, sale.documentId, ADMIN_PIN))
        bench.bfd.loseNextAnswer()

        val first = kassa.api.createSellReturnReceipt(kassa.kkmId, CASHIER_PIN, refund)
        val again = kassa.api.createSellReturnReceipt(kassa.kkmId, CASHIER_PIN, refund)
        kassa.resendQueue()

        assertEquals(first.documentId, again.documentId)
        assertEquals(1, bench.bfd.countedTickets().count { it.operation == OperationTypeEnum.OPERATION_SELL_RETURN })
    }

    @Test
    fun `отказ БФД виден у документа с кодом`() {
        val kassa = bench.registerKassa(NOT_PAYER).also { it.openShift() }

        val rejected = kassa.rejectedSale()

        val document = kassa.api.getDocumentDetails(kassa.kkmId, rejected.documentId, ADMIN_PIN).document
        assertEquals(DeliveryStatus.ONLINE_ERROR, rejected.deliveryStatus)
        assertEquals(13, document.ofdErrorCode)
        assertNull(bench.bfd.countedTickets().firstOrNull())
    }

    @Test
    fun `отказ БФД досланному документу назван в очереди словами на языке кассира`() {
        val kassa = bench.registerKassa(NOT_PAYER).also { it.openShift() }
        kassa.offlineSale()
        bench.bfd.refuseNext(INCORRECT_REQUEST_DATA)

        kassa.resendQueue()

        val item = bench.superkassa.queue.listQueue(kassa.kkmId, ADMIN_PIN).single()
        assertEquals("REJECTED", item.status)
        assertEquals(CoreStrings.bfdRefusal(INCORRECT_REQUEST_DATA), TrilingualMessage(item.errorRu!!, item.errorKk!!, item.errorEn!!))
    }

    @Test
    fun `переименованная касса отвечает так же полно, как чтение кассы - со сменой и очередью`() {
        val kassa = bench.registerKassa(NOT_PAYER).also { it.openShift() }
        kassa.offlineSale()

        val renamed = kassa.api.updateKkmName(kassa.kkmId, CASHIER_PIN, "Касса у входа")

        assertEquals(true to 1, renamed.isShiftOpen to renamed.offlineQueueCount)
        assertEquals(kassa.info(), renamed)
    }

    @Test
    fun `кассир входит своим пином`() {
        val kassa = bench.registerKassa(NOT_PAYER)

        assertEquals("CASHIER", kassa.api.currentUser(kassa.kkmId, CASHIER_PIN).role.name)
    }

    private companion object {
        /** RESULT_TYPE_INCORRECT_REQUEST_DATA: БФД не принял данные документа. */
        const val INCORRECT_REQUEST_DATA = 13
    }
}
