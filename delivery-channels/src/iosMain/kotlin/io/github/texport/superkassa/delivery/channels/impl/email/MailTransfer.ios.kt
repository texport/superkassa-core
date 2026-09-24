package io.github.texport.superkassa.delivery.channels.impl.email

import io.github.texport.superkassa.delivery.channels.api.model.SmtpServer

/**
 * На iOS переносимого SMTP-клиента нет, а системная почта отправляет письмо
 * только руками пользователя: канал отвечает отказом «не поддерживается».
 */
internal actual fun platformMailTransfer(server: SmtpServer): MailTransfer? = null
