package io.github.texport.superkassa.delivery.channels.impl.email

import io.github.texport.superkassa.delivery.channels.api.model.SmtpServer
import io.github.texport.superkassa.delivery.channels.impl.common.reasonOf
import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.MessagingException
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.Properties

internal actual fun platformMailTransfer(server: SmtpServer): MailTransfer? = JakartaMailTransfer(server)

/**
 * Почта через Jakarta Mail (Eclipse Angus) — на JVM и на Android одна и та же.
 *
 * Отправка идёт в потоке ввода-вывода: на Android сетевой вызов из главного
 * потока платформа обрывает, а кассир может отправить чек с экрана.
 */
internal class JakartaMailTransfer(private val server: SmtpServer) : MailTransfer {

    override fun transfer(letter: Letter): TransferFailure? = try {
        runBlocking(Dispatchers.IO) { Transport.send(message(letter)) }
        null
    } catch (e: MessagingException) {
        TransferFailure(reasonOf(e), e.message.toString().trim())
    }

    private fun message(letter: Letter): MimeMessage = MimeMessage(session()).apply {
        setFrom(InternetAddress(letter.from))
        addRecipient(Message.RecipientType.TO, InternetAddress(letter.to))
        setSubject(letter.subject, CHARSET)
        setContent(content(letter))
    }

    private fun content(letter: Letter): MimeMultipart = MimeMultipart().apply {
        addBodyPart(MimeBodyPart().apply { setText(letter.body, CHARSET) })
        letter.attachment?.let { file ->
            addBodyPart(
                MimeBodyPart().apply {
                    setContent(file.bytes, file.mimeType)
                    fileName = file.fileName
                }
            )
        }
    }

    private fun session(): Session {
        val user = server.user?.takeIf { it.isNotBlank() }
        val properties = Properties().apply {
            put("mail.smtp.host", server.host)
            put("mail.smtp.port", server.port.toString())
            put("mail.smtp.auth", (user != null).toString())
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.ssl.enable", (server.port == SMTPS_PORT).toString())
            put("mail.smtp.connectiontimeout", TIMEOUT_MILLIS)
            put("mail.smtp.timeout", TIMEOUT_MILLIS)
            put("mail.smtp.writetimeout", TIMEOUT_MILLIS)
        }
        return Session.getInstance(properties, user?.let { login(it) })
    }

    private fun login(user: String): Authenticator = object : Authenticator() {
        override fun getPasswordAuthentication() = PasswordAuthentication(user, server.password.orEmpty())
    }

    private companion object {
        const val CHARSET = "UTF-8"
        const val SMTPS_PORT = 465

        /** Сервер почты, который не отвечает, не должен держать кассира дольше HTTP-канала. */
        const val TIMEOUT_MILLIS = "10000"
    }
}
