package io.github.texport.superkassa.embedded.impl.delivery

import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryFailure
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryOutcome
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryRequest
import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import io.github.texport.superkassa.core.domain.api.port.integration.DeliveryPort
import io.github.texport.superkassa.core.domain.impl.logging.getLogger
import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.string.api.TrilingualMessage
import io.github.texport.superkassa.delivery.api.createDeliveryServiceApi
import io.github.texport.superkassa.delivery.api.model.DeliveryChannel
import io.github.texport.superkassa.delivery.api.toOutcome
import io.github.texport.superkassa.delivery.api.model.DeliveryRequest as ChannelRequest
import io.github.texport.superkassa.delivery.api.port.DeliveryPort as ChannelPort

/**
 * Доставка чека покупателю в кассе приложения.
 *
 * Успех — только когда канал действительно отправил. Незнакомый канал,
 * ненастроенный канал и принтер без адреса отвечают отказом с кодом
 * и причиной: чек, записанный доставленным, но не ушедший, покупатель
 * не получит, а кассир об этом не узнает.
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

    override fun deliver(request: DeliveryRequest): Boolean = send(request).delivered

    override fun send(request: DeliveryRequest): DeliveryOutcome {
        val channel = DeliveryChannel.entries.firstOrNull { it.name.equals(request.channel, ignoreCase = true) }
            ?: return refuse(request, UNKNOWN_CHANNEL, CoreStrings.deliveryChannelUnknown(request.channel))
        if (channel == DeliveryChannel.PRINT && request.payloadType != ESC_POS) {
            return refuse(request, PRINT_NEEDS_ESC_POS, CoreStrings.printerNeedsEscPos())
        }
        val current = settings()
        // Одноимённый канал, названный позже, заменяет прежний:
        // канал приложения — канал из настроек, сетевой принтер — принтер приложения.
        val available = settingsChannels(current?.delivery) + channels + listOfNotNull(networkPrinter(current))
        val service = createDeliveryServiceApi(available)
        return runCatching { service.deliver(request.toChannelRequest(channel)) }
            .map { it.toOutcome(channel.name) }
            .getOrElse { failed(channel, it) }
    }

    /** Канал упал, не ответив итогом: в журнал — только род ошибки, её текст может нести адрес покупателя. */
    private fun failed(channel: DeliveryChannel, failure: Throwable): DeliveryOutcome {
        val reason = failure::class.simpleName.orEmpty()
        logger.warn("Delivery failed: channel={}, reason={}", channel, reason)
        val message = CoreStrings.deliveryChannelFailed(channel.name, reason)
        return DeliveryOutcome.failed(DeliveryFailure(CHANNEL_FAILED, message), retryable = true)
    }

    /** Сетевой принтер из настроек; без адреса принтера нет, и печать отказывает. */
    private fun networkPrinter(settings: CoreSettings?): ChannelPort? {
        val print = settings?.delivery?.print?.takeIf { it.enabled } ?: return null
        val connection = print.connection?.takeIf { it.type.equals(NETWORK, ignoreCase = true) } ?: return null
        val host = connection.host?.takeIf { it.isNotBlank() } ?: return null
        val port = connection.port ?: return null
        return NetworkPrinterChannel(host, port)
    }

    private fun refuse(request: DeliveryRequest, code: String, message: TrilingualMessage): DeliveryOutcome {
        logger.warn("Delivery refused: channel={}, code={}", request.channel, code)
        return DeliveryOutcome.failed(DeliveryFailure(code, message), retryable = false)
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
        const val UNKNOWN_CHANNEL = "DELIVERY_CHANNEL_UNKNOWN"
        const val PRINT_NEEDS_ESC_POS = "DELIVERY_PRINT_ESC_POS_ONLY"
        const val CHANNEL_FAILED = "DELIVERY_CHANNEL_FAILED"
    }
}
