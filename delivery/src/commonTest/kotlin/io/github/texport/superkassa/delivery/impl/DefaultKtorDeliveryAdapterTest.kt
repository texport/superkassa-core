package io.github.texport.superkassa.delivery.impl

import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryRequest
import kotlin.test.Test
import kotlin.test.assertFalse

/**
 * Адаптер доставки по умолчанию: получателя обязан проверять он сам.
 *
 * Отправка документа «в никуда» должна быть отказом, а не тихим успехом:
 * вызывающий по возвращённому значению решает, помечать ли документ доставленным.
 */
class DefaultKtorDeliveryAdapterTest {

    private val adapter = DefaultKtorDeliveryAdapter()

    private fun request(destination: String?) = DeliveryRequest(
        kkmId = "kkm-1",
        documentId = "doc-1",
        channel = "EMAIL",
        destination = destination,
        payloadType = "HTML",
        payloadUrl = "https://receipt.example.kz?i=1"
    )

    @Test
    fun `доставка с получателем не выдаётся за отправленную`() {
        assertFalse(adapter.deliver(request("buyer@example.kz")))
    }

    @Test
    fun `доставка без получателя отвергается`() {
        assertFalse(adapter.deliver(request(null)))
    }

    @Test
    fun `пустой получатель отвергается так же, как отсутствующий`() {
        assertFalse(adapter.deliver(request("")))
        assertFalse(adapter.deliver(request("   ")))
    }
}
