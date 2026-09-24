package io.github.texport.superkassa.offlinequeue.api.model

import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.string.api.TrilingualMessage
import io.github.texport.superkassa.offlinequeue.impl.QueueDispatchException

/**
 * Результат обработки попытки выполнения команды очереди.
 *
 * @property status совместимый статус выполнения (SENT или FAILED).
 * @property errorMessage сообщение об ошибке при сбое выполнения (или null).
 * @property retryAt абсолютное системное время повторной попытки в миллисекундах (или null для авторасчета по backoff).
 * @property error локализованное сообщение об ошибке на трех языках (русский, казахский, английский).
 * @property errorCode код отказа получателя (или null): хранится в команде рядом с текстом.
 */
data class DispatchResult(
    val status: QueueStatus,
    val errorMessage: String? = null,
    val retryAt: Long? = null,
    val error: TrilingualMessage? = errorMessage?.let { TrilingualMessage.mono(it) },
    val errorCode: Int? = null
) {
    init {
        if (status == QueueStatus.PENDING || status == QueueStatus.IN_PROGRESS) {
            throw QueueDispatchException(CoreStrings.invalidDispatchStatus(status.name))
        }
    }

    /**
     * Создает результат обработки без раскрытия внутренних статусов очереди (PENDING, IN_PROGRESS) обработчикам.
     *
     * @param status статус результата попытки обработки.
     * @param errorMessage сообщение об ошибке.
     * @param retryAt время повторной попытки выполнения.
     * @param error локализованное сообщение об ошибке.
     * @param errorCode код отказа получателя.
     */
    constructor(
        status: DispatchStatus,
        errorMessage: String? = null,
        retryAt: Long? = null,
        error: TrilingualMessage? = errorMessage?.let { TrilingualMessage.mono(it) },
        errorCode: Int? = null
    ) : this(
        status = when (status) {
            DispatchStatus.SENT -> QueueStatus.SENT
            DispatchStatus.FAILED -> QueueStatus.FAILED
            DispatchStatus.REJECTED -> QueueStatus.REJECTED
        },
        errorMessage = errorMessage,
        retryAt = retryAt,
        error = error,
        errorCode = errorCode
    )

    /**
     * Возвращает статус попытки выполнения, предназначенный для использования во внешних обработчиках.
     */
    val dispatchStatus: DispatchStatus
        get() = when (status) {
            QueueStatus.SENT -> DispatchStatus.SENT
            QueueStatus.REJECTED -> DispatchStatus.REJECTED
            else -> DispatchStatus.FAILED
        }
}
