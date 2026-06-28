package kz.mybrain.superkassa.core.domain.model.queue

/**
 * Элемент очереди оффлайн-команд для отправки в ОФД.
 *
 * @property kkmId Идентификатор ККМ, к которой относится команда.
 * @property type Тип команды (например, продажа, возврат, закрытие смены).
 * @property payloadRef Ссылка на полезную нагрузку команды во внутреннем хранилище.
 */
data class OfflineQueueCommandRequest(
    val kkmId: String,
    val type: String,
    val payloadRef: String
)
