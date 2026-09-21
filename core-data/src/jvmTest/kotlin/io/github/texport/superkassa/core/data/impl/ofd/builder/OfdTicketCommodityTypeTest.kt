package io.github.texport.superkassa.core.data.impl.ofd.builder

import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.receipt.PaymentType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptItem
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptPayment
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class OfdTicketCommodityTypeTest {

    @Test
    fun `позиция из национального каталога объявляет себя товаром`() {
        // Позиция с НТИН уходила без типа предмета потребления, ОФД отвечал
        // кодом 15 «Касса заблокирована», и касса по этому коду уходила
        // в блокировку: первая же продажа товара из каталога останавливала
        // работу. НТИН выдаётся каталогом товаров, поэтому тип известен
        // без вопроса кассиру.
        val commodity = commodityOf(protocolVersion = "204")

        assertEquals("COMMODITY_TYPE_PRODUCT", commodity["commodityType"]!!.jsonPrimitive.content)
    }

    @Test
    fun `до версии 204 тип предмета потребления не посылается`() {
        // В более ранних версиях реквизита в протоколе нет, и ОФД отвергает
        // сам реквизит: посылать его паркý старых касс нельзя.
        assertNull(commodityOf(protocolVersion = "203")["commodityType"])
    }

    @Test
    fun `позиция без НТИН типа предмета потребления не объявляет`() {
        // Обязателен он только для товаров каталога; свободная позиция
        // кассира к нему не обязана.
        assertNull(commodityOf(protocolVersion = "204", ntin = null)["commodityType"])
    }

    private fun commodityOf(protocolVersion: String, ntin: String? = NTIN) =
        OfdTicketRequestBuilder.buildTicketRequest(
            ofdId = "bfd",
            protocolVersion = protocolVersion,
            deviceId = 11L,
            token = 22L,
            reqNum = 33,
            request = sale(ntin)
        )["payload"]!!
            .jsonObject["ticket"]!!
            .jsonObject["items"]!!
            .jsonArray[0]
            .jsonObject["commodity"]!!
            .jsonObject

    private fun sale(ntin: String?) = ReceiptRequest(
        kkmId = "kkm-1",
        pin = "5555",
        operation = ReceiptOperationType.SELL,
        items = listOf(
            ReceiptItem(
                name = "Шоколад",
                sectionCode = "001",
                quantity = 1_000,
                price = Money(2_500, 0),
                sum = Money(2_500, 0),
                ntin = ntin
            )
        ),
        payments = listOf(ReceiptPayment(PaymentType.CASH, Money(2_500, 0))),
        total = Money(2_500, 0),
        idempotencyKey = "idem-1"
    )

    private companion object {

        /** Код из каталога, на котором касса встала в блокировку. */
        const val NTIN = "0200198799025"
    }
}
