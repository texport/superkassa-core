package io.github.texport.superkassa.core.domain.impl.usecase.delivery

import io.github.texport.superkassa.core.domain.api.model.settings.DeliveryChannelSettings
import io.github.texport.superkassa.core.domain.api.model.settings.DeliverySettings
import io.github.texport.superkassa.core.domain.api.model.settings.PrintConnectionSettings
import io.github.texport.superkassa.core.domain.api.model.settings.PrintDeliverySettings
import kotlin.test.Test
import kotlin.test.assertEquals

/** Какие задачи доставки ставит чек по настройкам. */
class ReceiptDeliveryPlanTest {
    private fun routes(settings: DeliverySettings?, hasLink: Boolean = true) =
        ReceiptDeliveryPlan(settings).tasksFor("kkm-1", "doc-1", hasLink, now = 5L)
            .map { Triple(it.channel, it.destination, it.payloadType) }

    @Test
    fun `без настроек доставки задач нет`() {
        assertEquals(emptyList(), routes(null))
    }

    @Test
    fun `сетевой принтер печатает, принтер без адреса и выключенный - нет`() {
        val network = PrintConnectionSettings(type = "NETWORK", host = "10.0.0.5", port = 9100)

        assertEquals(listOf(Triple("PRINT", null, "ESC_POS")), routes(DeliverySettings(print = PrintDeliverySettings(connection = network))))
        assertEquals(emptyList(), routes(DeliverySettings(print = PrintDeliverySettings(connection = network.copy(port = null)))))
        assertEquals(emptyList(), routes(DeliverySettings(print = PrintDeliverySettings(connection = null))))
        assertEquals(emptyList(), routes(DeliverySettings(print = PrintDeliverySettings(enabled = false, connection = network))))
    }

    @Test
    fun `ссылка - только когда БФД её дал, документ - всегда`() {
        val both = DeliveryChannelSettings("SMS", payloadType = "both", documentFormat = "pdf", destination = "+77010000001")
        val link = DeliveryChannelSettings("TELEGRAM", payloadType = "LINK", destination = "chat")

        assertEquals(
            listOf(Triple("SMS", "+77010000001", "LINK"), Triple("SMS", "+77010000001", "PDF"), Triple("TELEGRAM", "chat", "LINK")),
            routes(DeliverySettings(channels = listOf(both, link)))
        )
        assertEquals(listOf(Triple("SMS", "+77010000001", "PDF")), routes(DeliverySettings(channels = listOf(both, link)), hasLink = false))
    }

    @Test
    fun `выключенный канал и незнакомый вид нагрузки задач не дают, канал без получателя - даёт`() {
        val channels = listOf(
            DeliveryChannelSettings("EMAIL", enabled = false, destination = "a@b.kz"),
            DeliveryChannelSettings("WHATSAPP", payloadType = "VIDEO", destination = "+77010000002"),
            DeliveryChannelSettings("EMAIL", documentFormat = "HTML")
        )

        assertEquals(listOf(Triple("EMAIL", null, "HTML")), routes(DeliverySettings(channels = channels)))
    }

    @Test
    fun `задача ждёт с момента постановки`() {
        val task = ReceiptDeliveryPlan(DeliverySettings(channels = listOf(DeliveryChannelSettings("SMS", destination = "+7"))))
            .tasksFor("kkm-1", "doc-1", hasLink = false, now = 5L).single()

        assertEquals(listOf("doc-1/SMS/PDF", "kkm-1", "doc-1"), listOf(task.id, task.kkmId, task.documentId))
        assertEquals(Triple(5L, 5L, 0), Triple(task.nextAttemptAt, task.createdAt, task.attempts))
    }
}
