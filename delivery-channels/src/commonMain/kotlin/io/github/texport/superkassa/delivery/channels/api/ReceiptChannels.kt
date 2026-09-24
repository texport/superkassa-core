package io.github.texport.superkassa.delivery.channels.api

import io.github.texport.superkassa.delivery.api.model.DeliveryChannel
import io.github.texport.superkassa.delivery.api.port.DeliveryPort
import io.github.texport.superkassa.delivery.channels.api.model.SmtpServer
import io.github.texport.superkassa.delivery.channels.impl.common.NotConfiguredChannel
import io.github.texport.superkassa.delivery.channels.impl.common.platformJournal
import io.github.texport.superkassa.delivery.channels.impl.email.EmailChannel
import io.github.texport.superkassa.delivery.channels.impl.email.platformMailTransfer
import io.github.texport.superkassa.delivery.channels.impl.http.HttpClients
import io.github.texport.superkassa.delivery.channels.impl.http.SmsChannel
import io.github.texport.superkassa.delivery.channels.impl.http.TelegramChannel
import io.github.texport.superkassa.delivery.channels.impl.http.WhatsAppChannel

/*
 * Каналы доставки чека покупателю. Канал без настроек не исчезает, а отвечает
 * отказом «не настроен»: чек, записанный доставленным, но не ушедший,
 * покупатель не получит, а кассир об этом не узнает.
 *
 * Каналы синхронны: [DeliveryPort.send] возвращается, когда провайдер ответил
 * или вышло время ожидания. Ключи каналов не попадают ни в журнал, ни в текст отказа.
 */

/**
 * SMS через HTTP-шлюз провайдера: запрос `GET` по адресу-шаблону.
 *
 * @param providerUrl адрес шлюза с местами `{phone}` и `{text}`; `null` или пустой — канал не настроен.
 * @param apiKey ключ шлюза: уходит заголовком `Authorization: Bearer`; `null` — без заголовка.
 * @return канал SMS.
 */
fun smsChannel(providerUrl: String?, apiKey: String?): DeliveryPort {
    val url = providerUrl?.takeIf { it.isNotBlank() } ?: return notConfigured(DeliveryChannel.SMS)
    return SmsChannel(url, apiKey?.takeIf { it.isNotBlank() }, HttpClients.Default, journal())
}

/**
 * Telegram через Bot API: сообщение `sendMessage` в чат покупателя.
 *
 * @param botToken токен бота; `null` или пустой — канал не настроен.
 * @return канал Telegram.
 */
fun telegramChannel(botToken: String?): DeliveryPort {
    val token = botToken?.takeIf { it.isNotBlank() } ?: return notConfigured(DeliveryChannel.TELEGRAM)
    return TelegramChannel(token, HttpClients.Default, journal())
}

/**
 * WhatsApp через Cloud API: текстовое сообщение на номер покупателя.
 *
 * @param accessToken ключ доступа Cloud API; `null` или пустой — канал не настроен.
 * @param phoneNumberId идентификатор номера отправителя; `null` или пустой — канал не настроен.
 * @return канал WhatsApp.
 */
fun whatsAppChannel(accessToken: String?, phoneNumberId: String?): DeliveryPort {
    val token = accessToken?.takeIf { it.isNotBlank() } ?: return notConfigured(DeliveryChannel.WHATSAPP)
    val sender = phoneNumberId?.takeIf { it.isNotBlank() } ?: return notConfigured(DeliveryChannel.WHATSAPP)
    return WhatsAppChannel(token, sender, HttpClients.Default, journal())
}

/**
 * Почта через SMTP.
 *
 * На iOS почтового клиента нет, и канал отвечает отказом «не поддерживается
 * на этой платформе».
 *
 * @param server почтовый сервер; `null` — канал не настроен.
 * @return канал почты.
 */
fun emailChannel(server: SmtpServer?): DeliveryPort {
    if (server == null) return notConfigured(DeliveryChannel.EMAIL)
    return EmailChannel(server, platformMailTransfer(server), journal())
}

private fun notConfigured(channel: DeliveryChannel): DeliveryPort = NotConfiguredChannel(channel, journal())

private fun journal() = platformJournal("io.github.texport.superkassa.delivery.channels")
