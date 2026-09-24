package io.github.texport.superkassa.delivery.channels.impl.common

import io.github.texport.superkassa.delivery.api.model.DeliveryChannel
import io.github.texport.superkassa.delivery.api.model.DeliveryRequest

/** Журнал, который проверка читает целиком: что попало сюда, попало бы в журнал платформы. */
internal class RecordingJournal : Journal {
    private val lines = mutableListOf<String>()

    val written: String get() = lines.joinToString("\n")

    override fun info(line: String) {
        lines += "INFO $line"
    }

    override fun warn(line: String) {
        lines += "WARN $line"
    }
}

/** Запрос доставки чека для проверок. */
fun receiptRequest(
    channel: DeliveryChannel,
    destination: String? = "+7 (777) 000-11-22",
    payloadUrl: String? = "https://receipt.test/doc-1",
    payloadBytes: ByteArray? = null,
    payloadType: String? = null
) = DeliveryRequest(
    cashboxId = "kkm-1",
    documentId = "DOC-1",
    channel = channel,
    destination = destination,
    payloadUrl = payloadUrl,
    payloadBytes = payloadBytes,
    payloadType = payloadType
)
