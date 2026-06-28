package kz.mybrain.superkassa.core.domain.model.ofd

/**
 * Статус отправки фискальной команды в ОФД.
 */
enum class OfdCommandStatus {
    /** Успешный сетевой обмен с сервером ОФД. */
    OK,
    /** Критическая ошибка сетевого обмена или валидации протокола ОФД. */
    FAILED,
    /** Превышено время ожидания ответа от ОФД. */
    TIMEOUT
}
