package io.github.texport.superkassa.delivery.channels.impl.email

import io.github.texport.superkassa.delivery.channels.api.model.SmtpServer

/**
 * Письмо покупателю в том виде, в каком его отправляет почтовый клиент платформы.
 *
 * @property from адрес отправителя.
 * @property to адрес покупателя.
 * @property subject тема.
 * @property body текст письма.
 * @property attachment файл чека; `null` — письмо со ссылкой без вложения.
 */
internal class Letter(
    val from: String,
    val to: String,
    val subject: String,
    val body: String,
    val attachment: Attachment?
)

/**
 * Файл чека во вложении.
 *
 * @property bytes содержимое.
 * @property fileName имя файла у покупателя.
 * @property mimeType тип содержимого.
 */
internal class Attachment(val bytes: ByteArray, val fileName: String, val mimeType: String)

/**
 * Отказ почтового клиента.
 *
 * @property reason род отказа — классы ошибки и причины; идёт в журнал.
 * @property detail текст ошибки сервера без ключей; идёт в текст отказа.
 */
internal class TransferFailure(val reason: String, val detail: String)

/** Почтовый клиент платформы. */
internal fun interface MailTransfer {
    /**
     * Отправляет письмо.
     *
     * @return `null`, если сервер письмо принял; иначе — отказ.
     */
    fun transfer(letter: Letter): TransferFailure?
}

/**
 * Почтовый клиент платформы для сервера.
 *
 * @return `null` там, где переносимого почтового клиента нет (iOS).
 */
internal expect fun platformMailTransfer(server: SmtpServer): MailTransfer?
