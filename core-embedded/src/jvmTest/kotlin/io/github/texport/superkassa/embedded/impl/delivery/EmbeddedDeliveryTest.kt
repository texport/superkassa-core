package io.github.texport.superkassa.embedded.impl.delivery

import com.sun.net.httpserver.HttpServer
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryRequest
import io.github.texport.superkassa.core.domain.api.model.settings.CoreMode
import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import io.github.texport.superkassa.core.domain.api.model.settings.DeliverySettings
import io.github.texport.superkassa.core.domain.api.model.settings.SmsProviderSettings
import io.github.texport.superkassa.core.domain.api.model.settings.StorageSettings
import io.github.texport.superkassa.delivery.api.model.DeliveryChannel
import io.github.texport.superkassa.delivery.api.model.DeliveryResult
import io.github.texport.superkassa.delivery.api.port.DeliveryPort
import java.net.InetSocketAddress
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import io.github.texport.superkassa.delivery.api.model.DeliveryRequest as ChannelRequest

/**
 * Каналы доставки кассы в приложении собираются из настроек ядра:
 * приложение их не перечисляет, а подменить может любой.
 */
class EmbeddedDeliveryTest {
    private val gateway = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    private val received = mutableListOf<String>()

    init {
        gateway.createContext("/send") { exchange ->
            received += exchange.requestURI.rawQuery
            exchange.sendResponseHeaders(200, -1)
            exchange.close()
        }
        gateway.start()
    }

    @AfterTest
    fun stop() = gateway.stop(0)

    private fun settings(delivery: DeliverySettings?) = CoreSettings(
        mode = CoreMode.DESKTOP,
        storage = StorageSettings(engine = "SQLITE", jdbcUrl = "jdbc:sqlite:kassa.db"),
        delivery = delivery
    )

    private fun request(channel: String) = DeliveryRequest(
        kkmId = "kkm-1",
        documentId = "doc-1",
        channel = channel,
        destination = "+77770001122",
        payloadType = "LINK",
        payloadUrl = "https://receipt.example.kz?i=1"
    )

    @Test
    fun `SMS из настроек уходит на шлюз без участия приложения`() {
        val sms = SmsProviderSettings(providerUrl = "http://127.0.0.1:${gateway.address.port}/send?to={phone}&text={text}")
        val delivery = EmbeddedDelivery(emptyList()) { settings(DeliverySettings(sms = sms)) }

        assertTrue(delivery.deliver(request("SMS")))
        assertTrue(received.single().startsWith("to=%2B77770001122&text="), received.single())
    }

    @Test
    fun `канал без настроек — отказ, а не доставленный чек`() {
        val delivery = EmbeddedDelivery(emptyList()) { settings(null) }

        listOf("SMS", "TELEGRAM", "WHATSAPP", "EMAIL").forEach { channel ->
            assertFalse(delivery.deliver(request(channel)), channel)
        }
        assertTrue(received.isEmpty())
    }

    @Test
    fun `канал приложения заменяет канал из настроек`() {
        val sent = mutableListOf<ChannelRequest>()
        val telegram = object : DeliveryPort {
            override val channel = DeliveryChannel.TELEGRAM

            override fun send(request: ChannelRequest): DeliveryResult {
                sent += request
                return DeliveryResult(ok = true)
            }
        }
        val delivery = EmbeddedDelivery(listOf(telegram)) { settings(null) }

        assertTrue(delivery.deliver(request("telegram")))
        assertEquals("LINK", sent.single().payloadType)
    }
}
