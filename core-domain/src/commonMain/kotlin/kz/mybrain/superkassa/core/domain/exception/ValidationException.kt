package kz.mybrain.superkassa.core.domain.exception

/**
 * Исключение, выбрасываемое при нарушении бизнес-правил или формата данных (валидации).
 *
 * Указывает на некорректность переданных параметров запроса, заголовков или структуры данных.
 *
 * @param trilingualMessage Локализованное сообщение об ошибке на трех языках.
 * @param code Уникальный строковый код ошибки (по умолчанию "BAD_REQUEST").
 */
class ValidationException(
    trilingualMessage: TrilingualMessage,
    code: String = "BAD_REQUEST"
) : SuperkassaException(code, 400, trilingualMessage)
