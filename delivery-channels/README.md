# superkassa-delivery-channels

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

### [Documentation in English](#documentation-in-english) &middot; [Документация на русском языке](#документация-на-русском-языке)

---

## Documentation in English

Receipt delivery channels of the Superkassa core: SMS, Telegram, WhatsApp and email.
Each channel implements `DeliveryPort` of the `delivery` module.

| Channel | Transport | JVM | Android | iOS |
|---|---|---|---|---|
| SMS | HTTP gateway, `GET` by a URL template with `{phone}` and `{text}` | OkHttp | OkHttp | Darwin |
| Telegram | Bot API `sendMessage` | OkHttp | OkHttp | Darwin |
| WhatsApp | Cloud API `v18.0/<sender>/messages` | OkHttp | OkHttp | Darwin |
| Email | SMTP (Jakarta Mail, Eclipse Angus) | yes | yes | refused: not supported |

- A channel without settings is not dropped: it refuses with code `DELIVERY_<CHANNEL>_NOT_CONFIGURED`.
- Success is a 2xx answer only; any other answer or a network failure is a refusal with a code
  and a message in Russian, Kazakh and English.
- Channel keys (bot token, WhatsApp token, SMS key, SMTP password) never reach the log or the refusal text.

```kotlin
val channels = listOf(
    smsChannel(providerUrl = "https://sms.example.kz/send?to={phone}&text={text}", apiKey = key),
    telegramChannel(botToken),
    whatsAppChannel(accessToken, phoneNumberId),
    emailChannel(SmtpServer("smtp.example.kz", 587, "kassa", password, "kassa@example.kz"))
)
val delivery = createDeliveryServiceApi(channels)
```

The embedded cash register (`core-embedded`) builds these channels from `CoreSettings.delivery` by itself.

Android: the app packages Angus Mail; if the build reports duplicate `META-INF/LICENSE.md` / `NOTICE.md`,
add `pickFirsts` for them in `packaging.resources`.

## Документация на русском языке

Каналы доставки чека покупателю: SMS, Telegram, WhatsApp и почта. Каждый канал реализует
`DeliveryPort` модуля `delivery`.

- Канал без настроек не пропадает, а отвечает отказом с кодом `DELIVERY_<КАНАЛ>_NOT_CONFIGURED`.
- Успех — только ответ 2xx; иной ответ и сбой связи — отказ с кодом и текстом на трёх языках.
- Ключи каналов (токен бота, ключ WhatsApp, ключ SMS-шлюза, пароль SMTP) не попадают ни в журнал,
  ни в текст отказа.
- Почта на iOS не поддерживается: переносимого SMTP-клиента там нет, и канал честно отказывает.

Касса в приложении (`core-embedded`) собирает каналы из `CoreSettings.delivery` сама.
