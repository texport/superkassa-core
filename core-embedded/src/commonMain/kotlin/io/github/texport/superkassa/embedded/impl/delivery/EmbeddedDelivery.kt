package io.github.texport.superkassa.embedded.impl.delivery

import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryRequest
import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import io.github.texport.superkassa.core.domain.api.port.integration.DeliveryPort
import io.github.texport.superkassa.core.domain.impl.logging.getLogger
import io.github.texport.superkassa.delivery.api.createDeliveryServiceApi
import io.github.texport.superkassa.delivery.api.model.DeliveryChannel
import io.github.texport.superkassa.delivery.api.model.DeliveryRequest as ChannelRequest
import io.github.texport.superkassa.delivery.api.port.DeliveryPort as ChannelPort

/**
 * Доставка чека покупателю в кассе приложения.
 *
 * Успех — только когда канал действительно отправил. Незнакомый канал,
 * ненастроенный канал и принтер без адреса отвечают отказом: чек,
 * записанный доставленным, но не ушедший, покупатель не получит,
 * а кассир об этом не узнает.
 *
 * Каналы собираются из настроек на каждую доставку: владелец меняет
 * настройки доставки без перезапуска кассы.
 *
 * @param channels каналы приложения: заменяют одноимённые каналы из настроек.
 * @param settings текущие настройки ядра: из них берутся каналы и сетевой принтер.
 */
internal class EmbeddedDelivery(
    private val channels: List<ChannelPort>,
    private val settings: () -> CoreSettings?
) : DeliveryPort {
    private val logger = getLogger(EmbeddedDelivery::class)

    override fun deliver(request: DeliveryRequest): Boolean {
        val channel = DeliveryChannel.entries.firstOrNull { it.name.equals(request.channel, ignoreCase = true) }
            ?: return refuse(request, "unknown channel")
        if (channel == DeliveryChannel.PRINT && request.payloadType != ESC_POS) {
            return refuse(request, "printer accepts ESC/POS only")
        }
        val current = settings()
        // Одноимённый канал, названный позже, заменяет прежний:
        // канал приложения — канал из настроек, сетевой принтер — принтер приложения.
        val available = settingsChannels(current?.delivery) + channels + listOfNotNull(networkPrinter(current))
        val service = createDeliveryServiceApi(available)
        return try {
            service.deliver(request.toChannelRequest(channel)).ok
        } catch (e: Exception) {
            logger.warn("Delivery failed: channel={}, reason={}", channel, e::class.simpleName)
            false
        }
    }

    /** Сетевой принтер из настроек; без адреса принтера нет, и печать отказывает. */
    private fun networkPrinter(settings: CoreSettings?): ChannelPort? {
        val print = settings?.delivery?.print?.takeIf { it.enabled } ?: return null
        val connection = print.connection?.takeIf { it.type.equals(NETWORK, ignoreCase = true) } ?: return null
        val host = connection.host?.takeIf { it.isNotBlank() } ?: return null
        val port = connection.port ?: return null
        return NetworkPrinterChannel(host, port)
    }

    private fun refuse(request: DeliveryRequest, reason: String): Boolean {
        logger.warn("Delivery refused: channel={}, reason={}", request.channel, reason)
        return false
    }

    private fun DeliveryRequest.toChannelRequest(channel: DeliveryChannel) = ChannelRequest(
        cashboxId = kkmId,
        documentId = documentId,
        channel = channel,
        destination = destination,
        payloadUrl = payloadUrl,
        payloadBytes = payloadBytes,
        payloadType = payloadType
    )

    private companion object {
        const val ESC_POS = "ESC_POS"
        const val NETWORK = "NETWORK"
    }
}
