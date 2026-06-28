package kz.mybrain.superkassa.core.domain.exception

/**
 * Исключение, выбрасываемое при ошибках доступа или манипулирования в хранилище данных (базе данных).
 *
 * Сигнализирует о сбоях на уровне инфраструктуры хранения (например, ошибки подключения к СУБД,
 * нарушение уникальных ограничений или сбои транзакций).
 *
 * @param trilingualMessage Локализованное сообщение об ошибке на трех языках.
 * @param code Уникальный строковый код ошибки (по умолчанию "STORAGE_ERROR").
 * @param cause Первопричина исключения (например, SQLException).
 */
@Suppress("unused") // Исключение выбрасывается при ошибках инфраструктуры БД и обрабатывается глобально
class StorageException(
    trilingualMessage: TrilingualMessage,
    code: String = "STORAGE_ERROR",
    cause: Throwable? = null
) : SuperkassaException(code, 500, trilingualMessage, cause)
