package io.github.texport.superkassa.delivery.channels.api.model

/**
 * Почтовый сервер, через который касса отправляет чек покупателю.
 *
 * Порт 465 — сразу TLS (SMTPS); на прочих портах соединение
 * переходит на TLS командой STARTTLS, если сервер её предлагает.
 *
 * @property host имя или адрес SMTP-сервера.
 * @property port порт SMTP-сервера.
 * @property user имя для входа; пустое — сервер без входа.
 * @property password пароль для входа.
 * @property from адрес отправителя в письме.
 */
data class SmtpServer(
    val host: String,
    val port: Int,
    val user: String?,
    val password: String?,
    val from: String
) {
    /** Без пароля: описание сервера попадает в журналы и сообщения, пароль — никогда. */
    override fun toString(): String = "SmtpServer(host=$host, port=$port, user=$user, from=$from)"
}
