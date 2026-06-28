package kz.mybrain.superkassa.core.domain.exception

/**
 * Исключение, выбрасываемое при нарушении прав доступа к запрашиваемым операциям.
 *
 * Сигнализирует о том, что у текущего пользователя недостаточно полномочий
 * или его роль (например, кассир) не позволяет выполнить данное действие.
 *
 * @param trilingualMessage Локализованное сообщение об ошибке на трех языках.
 * @param code Уникальный строковый код ошибки (по умолчанию "FORBIDDEN").
 */
class ForbiddenException(
    trilingualMessage: TrilingualMessage,
    code: String = "FORBIDDEN"
) : SuperkassaException(code, 403, trilingualMessage)
