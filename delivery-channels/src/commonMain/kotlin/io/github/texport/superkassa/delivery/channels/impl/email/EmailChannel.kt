package io.github.texport.superkassa.delivery.channels.impl.email

import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.string.api.TrilingualMessage
import io.github.texport.superkassa.delivery.api.model.DeliveryChannel
import io.github.texport.superkassa.delivery.api.model.DeliveryRequest
import io.github.texport.superkassa.delivery.api.model.DeliveryResult
import io.github.texport.superkassa.delivery.api.port.DeliveryPort
import io.github.texport.superkassa.delivery.channels.api.model.SmtpServer
import io.github.texport.superkassa.delivery.channels.impl.common.DeliveryCodes
import io.github.texport.superkassa.delivery.channels.impl.common.Journal
import io.github.texport.superkassa.delivery.channels.impl.common.MaskedJournal
import io.github.texport.superkassa.delivery.channels.impl.common.Secrets
import io.github.texport.superkassa.delivery.channels.impl.common.refusal

/**
 * Чек покупателю почтой.
 *
 * Письмо несёт ссылку на чек, файл чека или то и другое, как у узла.
 * Нет ни ссылки, ни файла — отказ, а не пустое письмо.
 *
 * @param server почтовый сервер.
 * @param transfer почтовый клиент платформы; `null` — на платформе почты нет.
 */
internal class EmailChannel(
    private val server: SmtpServer,
    private val transfer: MailTransfer?,
    journal: Journal
) : DeliveryPort {
    override val channel: DeliveryChannel = DeliveryChannel.EMAIL
    private val secrets = Secrets(listOf(server.password))
    private val journal = MaskedJournal(journal, secrets)

    override fun send(request: DeliveryRequest): DeliveryResult {
        val client = transfer
            ?: return refused(request, DeliveryCodes.EMAIL_UNSUPPORTED, CoreStrings.emailUnsupportedOnPlatform())
        val to = request.destination?.takeIf { it.isNotBlank() }
            ?: return refused(request, DeliveryCodes.recipientRequired(channel), CoreStrings.deliveryRecipientRequired(TITLE))
        val body = bodyOf(request)
            ?: return refused(request, DeliveryCodes.EMAIL_PAYLOAD_MISSING, CoreStrings.emailPayloadMissing())
        val subject = CoreStrings.emailSubject(request.documentId).ru
        val failure = client.transfer(Letter(server.from, to, subject, body, attachmentOf(request)))
            ?: return delivered(request)
        journal.warn("$TITLE failed: documentId=${request.documentId}, reason=${failure.reason}")
        return refusal(DeliveryCodes.EMAIL_SEND_FAILED, CoreStrings.emailSendFailed(secrets.mask(failure.detail)))
    }

    private fun delivered(request: DeliveryRequest): DeliveryResult {
        journal.info("$TITLE delivered: documentId=${request.documentId}")
        return DeliveryResult(ok = true)
    }

    private fun refused(request: DeliveryRequest, code: String, message: TrilingualMessage): DeliveryResult {
        journal.warn("$TITLE refused: documentId=${request.documentId}, code=$code")
        return refusal(code, message)
    }

    private fun bodyOf(request: DeliveryRequest): String? {
        val url = request.payloadUrl
        return when {
            !url.isNullOrBlank() -> CoreStrings.emailBodyLink(url).ru
            request.payloadBytes?.isNotEmpty() == true -> CoreStrings.emailBodyAttachment().ru
            else -> null
        }
    }

    /** Вложение по формату файла: PNG — картинкой, HTML — страницей, прочее — PDF, как у узла. */
    private fun attachmentOf(request: DeliveryRequest): Attachment? {
        val bytes = request.payloadBytes?.takeIf { it.isNotEmpty() } ?: return null
        val name = "receipt-${request.documentId}"
        return when (request.payloadType?.uppercase()) {
            "IMAGE" -> Attachment(bytes, "$name.png", "image/png")
            "HTML" -> Attachment(bytes, "$name.html", "text/html; charset=UTF-8")
            else -> Attachment(bytes, "$name.pdf", "application/pdf")
        }
    }

    private companion object {
        const val TITLE = "Email"
    }
}
