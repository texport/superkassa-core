package kz.mybrain.superkassa.core.domain.exception

/**
 * Исключение, выбрасываемое при конфликте бизнес-состояний домена.
 *
 * Указывает на то, что операция не может быть выполнена из-за несовместимого
 * текущего состояния системы (например, попытка выполнить действие, требующее закрытой смены,
 * при открытой смене).
 *
 * @param trilingualMessage Локализованное сообщение об ошибке на трех языках.
 * @param code Уникальный строковый код ошибки (по умолчанию "CONFLICT").
 */
class ConflictException(
    trilingualMessage: TrilingualMessage,
    code: String = "CONFLICT"
) : SuperkassaException(code, 409, trilingualMessage)
