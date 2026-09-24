package io.github.texport.superkassa.delivery.channels.impl.common

import io.github.texport.superkassa.core.string.api.TrilingualMessage
import io.github.texport.superkassa.delivery.api.model.DeliveryChannel
import io.github.texport.superkassa.delivery.api.model.DeliveryResult

/** Коды отказов каналов: по ним приложение и журнал различают причины, не разбирая текст. */
internal object DeliveryCodes {
    const val PROVIDER_REJECTED = "DELIVERY_PROVIDER_REJECTED"
    const val CHANNEL_FAILED = "DELIVERY_CHANNEL_FAILED"
    const val EMAIL_PAYLOAD_MISSING = "DELIVERY_EMAIL_PAYLOAD_MISSING"
    const val EMAIL_SEND_FAILED = "DELIVERY_EMAIL_SEND_FAILED"
    const val EMAIL_UNSUPPORTED = "DELIVERY_EMAIL_UNSUPPORTED_PLATFORM"

    fun notConfigured(channel: DeliveryChannel): String = "DELIVERY_${channel.name}_NOT_CONFIGURED"

    fun recipientRequired(channel: DeliveryChannel): String = "DELIVERY_${channel.name}_DESTINATION_REQUIRED"
}

/** Отказ с кодом и трёхъязычным текстом в виде [TrilingualMessage.compact]. */
internal fun refusal(code: String, message: TrilingualMessage): DeliveryResult =
    DeliveryResult(ok = false, message = message.compact(), code = code)

/** Имя канала в сообщениях кассиру и в журнале. */
internal val DeliveryChannel.title: String
    get() = when (this) {
        DeliveryChannel.TELEGRAM -> "Telegram"
        DeliveryChannel.WHATSAPP -> "WhatsApp"
        DeliveryChannel.EMAIL -> "Email"
        else -> name
    }

/**
 * Род отказа: класс ошибки и, если он известен, класс её причины.
 *
 * Текст исключения наружу не идёт: HTTP-клиент вкладывает в него адрес
 * запроса, а в адресе — ключ канала и номер покупателя.
 */
internal fun reasonOf(failure: Throwable): String {
    val kind = failure::class.simpleName
    val cause = failure.cause?.let { it::class.simpleName }
    return if (cause == null) "$kind" else "$kind ($cause)"
}
