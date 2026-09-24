package io.github.texport.superkassa.delivery.channels.impl.email

import io.github.texport.superkassa.delivery.api.model.DeliveryChannel
import io.github.texport.superkassa.delivery.channels.api.model.SmtpServer
import io.github.texport.superkassa.delivery.channels.impl.common.DeliveryCodes
import io.github.texport.superkassa.delivery.channels.impl.common.RecordingJournal
import io.github.texport.superkassa.delivery.channels.impl.common.receiptRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EmailChannelTest {
    private val journal = RecordingJournal()
    private val server = SmtpServer("smtp.test", 587, "kassa", PASSWORD, "kassa@shop.kz")
    private val letters = mutableListOf<Letter>()
    private var failure: TransferFailure? = null
    private val channel = EmailChannel(server, { letters += it; failure }, journal)

    private fun request(url: String? = null, bytes: ByteArray? = null, type: String? = null, to: String? = "buyer@mail.kz") =
        receiptRequest(DeliveryChannel.EMAIL, destination = to, payloadUrl = url, payloadBytes = bytes, payloadType = type)

    @Test
    fun `письмо со ссылкой уходит без вложения`() {
        val result = channel.send(request(url = "https://receipt.test/1"))

        assertTrue(result.ok)
        val letter = letters.single()
        assertEquals("kassa@shop.kz", letter.from)
        assertEquals("buyer@mail.kz", letter.to)
        assertEquals("Чек DOC-1", letter.subject)
        assertEquals("Ссылка на чек: https://receipt.test/1", letter.body)
        assertNull(letter.attachment)
        assertTrue(journal.written.contains("Email delivered: documentId=DOC-1"))
    }

    @Test
    fun `файл чека уходит вложением с типом по формату`() {
        channel.send(request(bytes = byteArrayOf(1)))
        channel.send(request(bytes = byteArrayOf(1), type = "image"))
        channel.send(request(url = "https://receipt.test/1", bytes = byteArrayOf(1), type = "HTML"))

        val files = letters.map { it.attachment }
        assertEquals(listOf("receipt-DOC-1.pdf", "receipt-DOC-1.png", "receipt-DOC-1.html"), files.map { it?.fileName })
        assertEquals(listOf("application/pdf", "image/png", "text/html; charset=UTF-8"), files.map { it?.mimeType })
        assertEquals("Чек во вложении.", letters.first().body)
        assertTrue(files.first()!!.bytes.contentEquals(byteArrayOf(1)))
    }

    @Test
    fun `пустой файл при ссылке не вкладывается`() {
        channel.send(request(url = "https://receipt.test/1", bytes = ByteArray(0)))

        assertNull(letters.single().attachment)
    }

    @Test
    fun `без ссылки и без файла письма нет`() {
        val empty = channel.send(request(bytes = ByteArray(0)))
        val none = channel.send(request())

        assertFalse(empty.ok)
        assertEquals(DeliveryCodes.EMAIL_PAYLOAD_MISSING, empty.code)
        assertEquals(DeliveryCodes.EMAIL_PAYLOAD_MISSING, none.code)
        assertEquals(false, none.retryable)
        assertTrue(letters.isEmpty())
    }

    @Test
    fun `без адреса покупателя письма нет`() {
        val result = channel.send(request(url = "https://receipt.test/1", to = null))
        val blank = channel.send(request(url = "https://receipt.test/1", to = " "))

        assertEquals("DELIVERY_EMAIL_DESTINATION_REQUIRED", result.code)
        assertEquals("DELIVERY_EMAIL_DESTINATION_REQUIRED", blank.code)
        assertTrue(letters.isEmpty())
    }

    @Test
    fun `отказ сервера — отказ доставки, пароль не уходит ни наружу, ни в журнал`() {
        failure = TransferFailure("AuthenticationFailedException", "535 bad credentials for kassa/$PASSWORD")

        val result = channel.send(request(url = "https://receipt.test/1"))

        assertFalse(result.ok)
        assertEquals(DeliveryCodes.EMAIL_SEND_FAILED, result.code)
        assertEquals(true, result.retryable)
        val message = result.message.orEmpty()
        assertTrue(message.contains("535 bad credentials"), message)
        assertFalse(message.contains(PASSWORD), message)
        assertTrue(journal.written.contains("reason=AuthenticationFailedException"))
        assertFalse(journal.written.contains(PASSWORD))
    }

    @Test
    fun `на платформе без почты — честный отказ`() {
        val result = EmailChannel(server, null, journal).send(request(url = "https://receipt.test/1"))

        assertFalse(result.ok)
        assertEquals(DeliveryCodes.EMAIL_UNSUPPORTED, result.code)
        assertEquals(false, result.retryable)
        assertTrue(result.message.orEmpty().contains("не поддерживается"))
    }

    @Test
    fun `описание сервера не несёт пароля`() {
        assertFalse(server.toString().contains(PASSWORD))
        assertTrue(server.toString().contains("smtp.test"))
    }

    private companion object {
        const val PASSWORD = "smtp-pass-do-not-print"
    }
}
