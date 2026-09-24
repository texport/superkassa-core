package io.github.texport.superkassa.core.string.impl

import io.github.texport.superkassa.core.string.api.TrilingualMessage

/**
 * Отказы каналов доставки чека и тексты, которые уходят покупателю.
 *
 * Подстановки — шаблонами строк, а не [String.format]: ответ провайдера
 * приходит извне, и знак `%` или `$` в нём не должен ломать сообщение.
 */
internal object DeliveryStrings {
    fun recipientRequired(channel: String): TrilingualMessage = TrilingualMessage(
        ru = "Не указан получатель чека для канала $channel. Укажите телефон, чат или почту покупателя.",
        kk = "$channel арнасы үшін чек алушы көрсетілмеген. Сатып алушының телефонын, чатын немесе поштасын көрсетіңіз.",
        en = "No receipt recipient for channel $channel. Enter the buyer's phone, chat or email."
    )

    fun channelNotConfigured(channel: String): TrilingualMessage = TrilingualMessage(
        ru = "Канал доставки $channel не настроен. Заполните его параметры в настройках доставки.",
        kk = "$channel жеткізу арнасы бапталмаған. Оның параметрлерін жеткізу баптауларында толтырыңыз.",
        en = "Delivery channel $channel is not configured. Fill in its parameters in the delivery settings."
    )

    fun providerRejected(channel: String, status: Int, answer: String): TrilingualMessage = TrilingualMessage(
        ru = "Доставка через $channel завершилась ошибкой с кодом $status. Проверьте настройки канала. Ответ: $answer",
        kk = "$channel арқылы жеткізу $status кодымен қате аяқталды. Арна баптауларын тексеріңіз. Жауап: $answer",
        en = "Delivery via $channel failed with status $status. Check the channel settings. Response: $answer"
    )

    fun channelFailed(channel: String, reason: String): TrilingualMessage = TrilingualMessage(
        ru = "Ошибка доставки через $channel: $reason. Проверьте связь с интернетом и повторите отправку.",
        kk = "$channel арқылы жеткізу қатесі: $reason. Интернет байланысын тексеріп, жіберуді қайталаңыз.",
        en = "Delivery via $channel failed: $reason. Check the internet connection and send again."
    )

    fun emailPayloadMissing(): TrilingualMessage = TrilingualMessage(
        ru = "Нечего отправить почтой: нет ни ссылки на чек, ни файла чека",
        kk = "Поштамен жіберетін ештеңе жоқ: чекке сілтеме де, чек файлы да жоқ",
        en = "Nothing to email: there is neither a receipt link nor a receipt file"
    )

    fun emailSendFailed(reason: String): TrilingualMessage = TrilingualMessage(
        ru = "Ошибка отправки почты: $reason. Проверьте сервер, порт, логин и пароль почты в настройках.",
        kk = "Пошта жіберу қатесі: $reason. Баптаулардағы пошта серверін, портын, логині мен құпиясөзін тексеріңіз.",
        en = "Email sending failed: $reason. Check the mail server, port, login and password in the settings."
    )

    fun emailUnsupportedOnPlatform(): TrilingualMessage = TrilingualMessage(
        ru = "Отправка чека почтой на этой платформе не поддерживается. Выберите SMS, Telegram или WhatsApp.",
        kk = "Бұл платформада чекті поштамен жіберуге қолдау жоқ. SMS, Telegram немесе WhatsApp таңдаңыз.",
        en = "Emailing receipts is not supported on this platform. Choose SMS, Telegram or WhatsApp."
    )

    fun receiptLinkText(url: String): TrilingualMessage = TrilingualMessage(
        ru = "Чек: $url",
        kk = "Чек: $url",
        en = "Receipt: $url"
    )

    fun receiptReadyText(documentId: String): TrilingualMessage = TrilingualMessage(
        ru = "Чек $documentId готов",
        kk = "$documentId чегі дайын",
        en = "Receipt $documentId is ready"
    )

    fun emailSubject(documentId: String): TrilingualMessage = TrilingualMessage(
        ru = "Чек $documentId",
        kk = "Чек $documentId",
        en = "Receipt $documentId"
    )

    fun emailBodyLink(url: String): TrilingualMessage = TrilingualMessage(
        ru = "Ссылка на чек: $url",
        kk = "Чекке сілтеме: $url",
        en = "Link to receipt: $url"
    )

    fun emailBodyAttachment(): TrilingualMessage = TrilingualMessage(
        ru = "Чек во вложении.",
        kk = "Чек қосымшада.",
        en = "Receipt is in attachment."
    )
}
