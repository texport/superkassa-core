package io.github.texport.superkassa.delivery.channels.impl.email

import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.ServerSetupTest
import io.github.texport.superkassa.delivery.api.model.DeliveryChannel
import io.github.texport.superkassa.delivery.channels.api.emailChannel
import io.github.texport.superkassa.delivery.channels.api.model.SmtpServer
import io.github.texport.superkassa.delivery.channels.impl.common.DeliveryCodes
import io.github.texport.superkassa.delivery.channels.impl.common.RecordingJournal
import io.github.texport.superkassa.delivery.channels.impl.common.receiptRequest
import jakarta.mail.internet.MimeMultipart
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Почта на поддельном SMTP-сервере: письмо, вложение, вход и отказ сервера. */
class JakartaMailTransferTest {
    private val smtp = GreenMail(ServerSetupTest.SMTP.dynamicPort())

    @BeforeTest
    fun start() = smtp.start()

    @AfterTest
    fun stop() = smtp.stop()

    private fun server(user: String? = null, password: String? = null, port: Int = smtp.smtp.port) =
        SmtpServer("127.0.0.1", port, user, password, "kassa@shop.kz")

    @Test
    fun `чек уходит письмом со ссылкой и файлом`() {
        val request = receiptRequest(
            DeliveryChannel.EMAIL,
            destination = "buyer@mail.kz",
            payloadBytes = "%PDF-1.4".encodeToByteArray()
        )

        val result = emailChannel(server(user = " ")).send(request)

        assertTrue(result.ok, result.message)
        val letter = smtp.receivedMessages.single()
        assertEquals("Чек DOC-1", letter.subject)
        assertEquals("buyer@mail.kz", letter.allRecipients.single().toString())
        val parts = letter.content as MimeMultipart
        assertEquals("Ссылка на чек: https://receipt.test/doc-1", parts.getBodyPart(0).content.toString())
        assertEquals("receipt-DOC-1.pdf", parts.getBodyPart(1).fileName)
    }

    @Test
    fun `касса входит на сервер под именем и паролем владельца`() {
        smtp.setUser("kassa@shop.kz", "kassa", "secret")

        val result = emailChannel(server(user = "kassa", password = "secret"))
            .send(receiptRequest(DeliveryChannel.EMAIL, destination = "buyer@mail.kz"))

        assertTrue(result.ok, result.message)
        assertEquals(1, smtp.receivedMessages.size)
    }

    @Test
    fun `сервер не пустил — отказ с ответом сервера, а не доставленный чек`() {
        smtp.setUser("kassa@shop.kz", "kassa", "secret")

        val result = emailChannel(server(user = "kassa", password = null))
            .send(receiptRequest(DeliveryChannel.EMAIL, destination = "buyer@mail.kz"))

        assertFalse(result.ok)
        assertEquals(DeliveryCodes.EMAIL_SEND_FAILED, result.code)
        assertTrue(result.message.orEmpty().contains("535 5.7.8  Authentication credentials invalid. Проверьте"), result.message)
        assertTrue(smtp.receivedMessages.isEmpty())
    }

    @Test
    fun `сервер недоступен — отказ с причиной, пароль не уходит`() {
        val journal = RecordingJournal()
        val closed = server(user = "kassa", password = "pass-do-not-print", port = SMTPS_PORT)

        val result = EmailChannel(closed, JakartaMailTransfer(closed), journal)
            .send(receiptRequest(DeliveryChannel.EMAIL, destination = "buyer@mail.kz"))

        assertFalse(result.ok)
        assertEquals(DeliveryCodes.EMAIL_SEND_FAILED, result.code)
        assertTrue(journal.written.contains("Email failed: documentId=DOC-1, reason=MailConnectException"), journal.written)
        assertFalse(journal.written.contains("pass-do-not-print"))
        assertFalse(result.message.orEmpty().contains("pass-do-not-print"))
    }

    private companion object {
        /** SMTPS на этой машине не слушается: попытка соединения отказывает сразу. */
        const val SMTPS_PORT = 465
    }
}
