package kz.mybrain.superkassa.core.domain.exception

/**
 * Исключение, выбрасываемое при отсутствии запрашиваемой сущности в системе.
 *
 * Указывает на то, что ресурс (например, конкретная ККМ или документ) не был найден
 * в репозитории или базе данных по указанному идентификатору.
 *
 * @param trilingualMessage Локализованное сообщение об ошибке на трех языках.
 * @param code Уникальный строковый код ошибки (по умолчанию "NOT_FOUND").
 */
class NotFoundException(
    trilingualMessage: TrilingualMessage,
    code: String = "NOT_FOUND"
) : SuperkassaException(code, 404, trilingualMessage)
