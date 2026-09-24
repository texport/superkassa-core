package io.github.texport.superkassa.core.domain.impl.usecase.delivery

import io.github.texport.superkassa.core.domain.api.model.receipt.CustomerContact
import io.github.texport.superkassa.core.domain.api.model.settings.DeliveryChannelSettings
import io.github.texport.superkassa.core.domain.api.model.settings.DeliverySettings
import io.github.texport.superkassa.core.domain.api.model.settings.PrintConnectionSettings
import io.github.texport.superkassa.core.domain.api.model.settings.PrintDeliverySettings
import kotlin.test.Test
import kotlin.test.assertEquals

/** Какие задачи доставки ставит чек по настройкам и контакту покупателя. */
class ReceiptDeliveryPlanTest {
    private val buyer = CustomerContact(phone = "+77010000001", email = "buyer@mail.kz", telegram = "chat")

    private fun routes(settings: DeliverySettings?, hasLink: Boolean = true, contact: CustomerContact? = buyer) =
        ReceiptDeliveryPlan { settings }.tasksFor("kkm-1", "doc-1", hasLink, now = 5L, contact = contact)
            .map { Triple(it.channel, it.destination, it.payloadType) }

    @Test
    fun `без настроек доставки задач нет`() {
        assertEquals(emptyList(), routes(null))
    }

    @Test
    fun `сетевой принтер печатает, принтер без адреса и выключенный - нет`() {
        val network = PrintConnectionSettings(type = "NETWORK", host = "10.0.0.5", port = 9100)

        assertEquals(listOf(Triple("PRINT", null, "ESC_POS")), routes(DeliverySettings(print = PrintDeliverySettings(connection = network))))
        assertEquals(listOf(Triple("PRINT", null, "ESC_POS")), routes(DeliverySettings(print = PrintDeliverySettings(connection = network)), contact = null))
        assertEquals(emptyList(), routes(DeliverySettings(print = PrintDeliverySettings(connection = network.copy(port = null)))))
        assertEquals(emptyList(), routes(DeliverySettings(print = PrintDeliverySettings(connection = null))))
        assertEquals(emptyList(), routes(DeliverySettings(print = PrintDeliverySettings(enabled = false, connection = network))))
    }

    @Test
    fun `ссылка - только когда БФД её дал, документ - всегда`() {
        val both = DeliveryChannelSettings("SMS", payloadType = "both", documentFormat = "pdf")
        val link = DeliveryChannelSettings("TELEGRAM", payloadType = "LINK")

        assertEquals(
            listOf(Triple("SMS", "+77010000001", "LINK"), Triple("SMS", "+77010000001", "PDF"), Triple("TELEGRAM", "chat", "LINK")),
            routes(DeliverySettings(channels = listOf(both, link)))
        )
        assertEquals(listOf(Triple("SMS", "+77010000001", "PDF")), routes(DeliverySettings(channels = listOf(both, link)), hasLink = false))
    }

    @Test
    fun `чек уходит на контакт покупателя, а не на получателя из настроек канала`() {
        val sms = DeliveryChannelSettings("SMS", destination = "+77000000000")

        assertEquals(listOf(Triple("SMS", "+77010000001", "PDF")), routes(DeliverySettings(channels = listOf(sms))))
    }

    @Test
    fun `контакт одного вида - только его каналы, без контакта задач нет`() {
        val channels = listOf("SMS", "WHATSAPP", "EMAIL", "TELEGRAM").map { DeliveryChannelSettings(it) }
        val settings = DeliverySettings(channels = channels)

        assertEquals(listOf("SMS", "WHATSAPP"), routes(settings, contact = CustomerContact(phone = "+7701")).map { it.first })
        assertEquals(listOf("EMAIL"), routes(settings, contact = CustomerContact(email = "a@b.kz")).map { it.first })
        assertEquals(emptyList(), routes(settings, contact = null))
        assertEquals(emptyList(), routes(settings, contact = CustomerContact(phone = " ")))
    }

    @Test
    fun `выключенный канал и незнакомый вид нагрузки задач не дают`() {
        val channels = listOf(
            DeliveryChannelSettings("EMAIL", enabled = false),
            DeliveryChannelSettings("WHATSAPP", payloadType = "VIDEO"),
            DeliveryChannelSettings("EMAIL", documentFormat = "HTML")
        )

        assertEquals(listOf(Triple("EMAIL", "buyer@mail.kz", "HTML")), routes(DeliverySettings(channels = channels)))
    }

    @Test
    fun `задача ждёт с момента постановки`() {
        val task = ReceiptDeliveryPlan { DeliverySettings(channels = listOf(DeliveryChannelSettings("SMS"))) }
            .tasksFor("kkm-1", "doc-1", hasLink = false, now = 5L, contact = buyer).single()

        assertEquals(listOf("doc-1/SMS/PDF", "kkm-1", "doc-1"), listOf(task.id, task.kkmId, task.documentId))
        assertEquals(Triple(5L, 5L, 0), Triple(task.nextAttemptAt, task.createdAt, task.attempts))
    }
}
