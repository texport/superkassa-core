package io.github.texport.superkassa.core.domain.api.exception

import io.github.texport.superkassa.core.string.api.TrilingualMessage
/**
 * Исключение, выбрасываемое при попытке изменить замороженные (заблокированные для редактирования) настройки кассы.
 *
 * Некоторые критические параметры ККМ могут быть изменены только в специальном режиме
 * программирования (например, PROGRAMMING). В обычном рабочем режиме данные настройки заморожены.
 *
 * @property messageText Текст сообщения об ошибке (будет продублирован для всех языков).
 * @param code Уникальный строковый код ошибки (по умолчанию "SETTINGS_FROZEN").
 */
class SettingsFrozenException(
    val messageText: String,
    code: String = "SETTINGS_FROZEN"
) : SuperkassaException(
    code = code,
    status = 403,
    trilingualMessage = TrilingualMessage(
        ru = messageText,
        kk = messageText,
        en = messageText
    )
)
