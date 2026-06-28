package kz.mybrain.superkassa.core.domain.exception

/**
 * Базовое абстрактное исключение доменной области Superkassa.
 *
 * Все специфичные бизнес-исключения приложения должны наследоваться от этого класса.
 * Хранит структурированную информацию об ошибке: строковый код, числовой HTTP-статус
 * и локализованное сообщение на трех языках.
 *
 * @property code Уникальный строковый код ошибки для фронтенда/клиентов API.
 * @property status HTTP-статус ответа (например, 400, 404, 500).
 * @property trilingualMessage Объект трехъязычного сообщения об ошибке (RU, KK, EN).
 * @param cause Исходная причина исключения (опционально).
 */
abstract class SuperkassaException(
    val code: String,
    val status: Int,
    val trilingualMessage: TrilingualMessage,
    cause: Throwable? = null
) : RuntimeException(
    "RU: ${trilingualMessage.ru} | KK: ${trilingualMessage.kk} | EN: ${trilingualMessage.en}",
    cause
)
