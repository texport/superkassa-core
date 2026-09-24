package io.github.texport.superkassa.embedded.impl.delivery

import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.delivery.api.model.DeliveryChannel
import io.github.texport.superkassa.delivery.api.model.DeliveryRequest
import io.github.texport.superkassa.delivery.api.model.DeliveryResult
import io.github.texport.superkassa.delivery.api.port.DeliveryPort

/**
 * Термопринтер в сети: байты ESC/POS уходят сокетом на его порт, обычно 9100.
 *
 * Успех — только когда байты приняты принтером; обрыв и отказ соединения
 * возвращаются отказом с кодом и причиной, и касса повторит печать.
 */
internal class NetworkPrinterChannel(private val host: String, private val port: Int) : DeliveryPort {
    override val channel: DeliveryChannel = DeliveryChannel.PRINT

    override fun send(request: DeliveryRequest): DeliveryResult {
        val bytes = request.payloadBytes
            ?: return DeliveryResult(false, CoreStrings.printPayloadMissing().compact(), PAYLOAD_MISSING, retryable = false)
        return runCatching { RawSocket.send(host, port, bytes) }.fold(
            onSuccess = { DeliveryResult(true) },
            onFailure = { DeliveryResult(false, CoreStrings.printerUnreachable().compact(), UNREACHABLE) }
        )
    }

    private companion object {
        const val PAYLOAD_MISSING = "DELIVERY_PRINT_PAYLOAD_MISSING"
        const val UNREACHABLE = "DELIVERY_PRINT_UNREACHABLE"
    }
}

/** Отправка байтов на адрес в сети. */
internal expect object RawSocket {
    fun send(host: String, port: Int, bytes: ByteArray)
}
