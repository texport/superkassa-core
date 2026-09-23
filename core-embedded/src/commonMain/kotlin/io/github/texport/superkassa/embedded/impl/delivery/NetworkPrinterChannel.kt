package io.github.texport.superkassa.embedded.impl.delivery

import io.github.texport.superkassa.delivery.api.model.DeliveryChannel
import io.github.texport.superkassa.delivery.api.model.DeliveryRequest
import io.github.texport.superkassa.delivery.api.model.DeliveryResult
import io.github.texport.superkassa.delivery.api.port.DeliveryPort

/**
 * Термопринтер в сети: байты ESC/POS уходят сокетом на его порт, обычно 9100.
 *
 * Успех — только когда байты приняты принтером; обрыв и отказ соединения
 * возвращаются отказом с причиной.
 */
internal class NetworkPrinterChannel(private val host: String, private val port: Int) : DeliveryPort {
    override val channel: DeliveryChannel = DeliveryChannel.PRINT

    override fun send(request: DeliveryRequest): DeliveryResult {
        val bytes = request.payloadBytes ?: return DeliveryResult(false, "Nothing to print")
        return try {
            RawSocket.send(host, port, bytes)
            DeliveryResult(true)
        } catch (e: Exception) {
            DeliveryResult(false, "Printer $host:$port is unreachable: ${e::class.simpleName}")
        }
    }
}

/** Отправка байтов на адрес в сети. */
internal expect object RawSocket {
    fun send(host: String, port: Int, bytes: ByteArray)
}
