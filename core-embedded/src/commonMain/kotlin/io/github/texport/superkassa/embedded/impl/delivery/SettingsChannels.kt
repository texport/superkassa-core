package io.github.texport.superkassa.embedded.impl.delivery

import io.github.texport.superkassa.core.domain.api.model.settings.DeliverySettings
import io.github.texport.superkassa.core.domain.api.model.settings.EmailProviderSettings
import io.github.texport.superkassa.delivery.api.port.DeliveryPort
import io.github.texport.superkassa.delivery.channels.api.emailChannel
import io.github.texport.superkassa.delivery.channels.api.model.SmtpServer
import io.github.texport.superkassa.delivery.channels.api.smsChannel
import io.github.texport.superkassa.delivery.channels.api.telegramChannel
import io.github.texport.superkassa.delivery.channels.api.whatsAppChannel

/**
 * Каналы доставки чека из настроек ядра — те же, что собирает узел.
 *
 * Каждый канал есть всегда: канал без настроек отвечает отказом
 * «не настроен», а не пропадает и не отвечает успехом.
 *
 * @param delivery настройки доставки; `null` — ни один канал не настроен.
 */
internal fun settingsChannels(delivery: DeliverySettings?): List<DeliveryPort> = listOf(
    smsChannel(delivery?.sms?.providerUrl, delivery?.sms?.apiKey),
    telegramChannel(delivery?.telegram?.botToken),
    whatsAppChannel(delivery?.whatsapp?.accessToken, delivery?.whatsapp?.phoneNumberId),
    emailChannel(delivery?.email?.toSmtpServer())
)

private fun EmailProviderSettings.toSmtpServer() = SmtpServer(host, port, user, password, from)
