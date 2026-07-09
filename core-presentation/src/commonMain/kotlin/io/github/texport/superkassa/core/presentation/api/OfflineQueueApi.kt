package io.github.texport.superkassa.core.presentation.api

import io.github.texport.superkassa.core.presentation.api.model.QueueStatusRequest
import io.github.texport.superkassa.core.presentation.api.model.QueueStatusResponse

/**
 * Интерфейс API управления автономной (оффлайн) очередью.
 *
 * Предоставляет методы для проверки состояния и запуска обработки фоновых задач.
 */
interface OfflineQueueApi {
    /**
     * Проверяет, разрешено ли отправлять документ напрямую в ОФД для указанной кассы.
     *
     * @param kkmId Уникальный идентификатор ККМ.
     * @return true, если прямая отправка разрешена.
     */
    fun canSendDirectly(kkmId: String): Boolean

    /**
     * Получить текущее состояние очереди для указанной кассы.
     *
     * @param request Запрос с уникальным идентификатором ККМ.
     * @return Информация о статусе и размере очереди [QueueStatusResponse].
     */
    fun getQueueStatus(request: QueueStatusRequest): QueueStatusResponse

    /**
     * Запускает фоновую обработку пакета отложенных команд для указанной ККМ.
     *
     * @param kkmId Уникальный идентификатор ККМ.
     * @param limit Максимальное количество команд для обработки за один вызов.
     * @return Количество успешно отправленных команд.
     */
    fun processOfflineBatch(kkmId: String, limit: Int = 10): Int
}
