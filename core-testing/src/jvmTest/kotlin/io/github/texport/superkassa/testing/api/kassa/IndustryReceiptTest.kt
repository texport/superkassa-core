package io.github.texport.superkassa.testing.api.kassa

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.presentation.api.model.receipt.CreateReceiptCommand
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptDomainRequest
import io.github.texport.superkassa.testing.api.kassa.BenchDirectory.Companion.NOT_PAYER
import io.github.texport.superkassa.testing.impl.kassa.Receipts
import kz.kazakhtelecom.proto.v203.DomainTypeEnum
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Отраслевые реквизиты чека — такси, стоянки, АЗС, услуг — обязательны
 * для БФД и доходят до него, какой бы командой чек ни пробит.
 */
class IndustryReceiptTest {
    private val directory = BenchDirectory()
    private val bench = directory.open()

    @AfterTest
    fun tearDown() {
        bench.close()
        directory.close()
    }

    @Test
    fun `чек такси общей командой уходит в БФД с номером машины, заказом и тарифом`() {
        val kassa = bench.registerKassa(NOT_PAYER).also { it.openShift() }
        val sale = Receipts.sale("1500.00", "1")
        val taxi = ReceiptDomainRequest.TaxiRequest(carNumber = "777ABC02", isOrder = true, currentFee = Decimal.parse("1500.00"))

        kassa.api.createReceipt(
            CreateReceiptCommand(
                kkmId = kassa.kkmId, pin = kassa.cashierPin, operation = "SELL", idempotencyKey = sale.idempotencyKey,
                items = sale.items, discountPercent = null, discountSum = null, markupPercent = null, markupSum = null,
                payments = sale.payments, taken = null, domain = ReceiptDomainRequest(type = "DOMAIN_TAXI", taxi = taxi)
            )
        )

        val domain = bench.bfd.countedTickets().single().domain
        assertEquals(DomainTypeEnum.DOMAIN_TAXI, domain?.type)
        assertEquals("777ABC02" to true, domain?.taxi?.car_number to domain?.taxi?.is_order)
        assertEquals(1500L, domain?.taxi?.current_fee?.bills)
    }
}
