package kz.mybrain.superkassa.core.data.exception

/**
 * Исключение, указывающее на временную (преходящую) ошибку при взаимодействии с ОФД.
 *
 * Данная ошибка обычно возникает из-за сетевых сбоев, временной недоступности сервера ОФД
 * или таймаутов запроса. Операции, завершившиеся данным исключением, могут быть повторены позже.
 *
 * @param message Детальное сообщение об ошибке.
 * @param cause Первопричина исключения (если имеется).
 */
class OfdTransientException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
