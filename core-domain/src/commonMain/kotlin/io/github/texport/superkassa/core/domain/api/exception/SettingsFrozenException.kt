package io.github.texport.superkassa.core.domain.api.exception

import io.github.texport.superkassa.core.string.api.TrilingualMessage

/**
 * Отказ изменить настройки, которые сейчас менять нельзя.
 *
 * Настройки ядра заморожены в режиме SERVER и там, где их правку запретил
 * владелец; поля, которыми владеет запуск, через API не меняются вовсе.
 *
 * @param message причина отказа на трёх языках.
 * @param code уникальный строковый код ошибки (по умолчанию "SETTINGS_FROZEN").
 */
class SettingsFrozenException(
    message: TrilingualMessage,
    code: String = "SETTINGS_FROZEN"
) : SuperkassaException(
    code = code,
    status = 403,
    trilingualMessage = message
) {
    /**
     * Отказ с одной строкой причины, одинаковой для всех языков.
     *
     * @param messageText текст причины.
     * @param code уникальный строковый код ошибки.
     */
    constructor(messageText: String, code: String = "SETTINGS_FROZEN") :
        this(TrilingualMessage(ru = messageText, kk = messageText, en = messageText), code)

    /** Причина отказа по-русски. */
    val messageText: String get() = trilingualMessage.ru
}
