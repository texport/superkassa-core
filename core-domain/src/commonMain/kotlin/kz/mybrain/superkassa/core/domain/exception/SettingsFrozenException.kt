package kz.mybrain.superkassa.core.domain.exception

/**
 * Исключение, выбрасываемое при попытке изменить замороженные (заблокированные для редактирования) настройки кассы.
 *
 * Некоторые критические параметры ККМ могут быть изменены только в специальном режиме
 * программирования (например, PROGRAMMING). В обычном рабочем режиме данные настройки заморожены.
 *
 * @property messageText Текст сообщения об ошибке (будет продублирован для всех языков).
 * @param code Уникальный строковый код ошибки (по умолчанию "SETTINGS_FROZEN").
 */
@Suppress("unused") // Исключение выбрасывается динамически в рантайме и обрабатывается глобальными перехватчиками
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
