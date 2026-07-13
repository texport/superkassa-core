package io.github.texport.superkassa.core.presentation.api

import io.github.texport.superkassa.core.presentation.api.model.queue.*

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
    @Throws(Exception::class)
    fun canSendDirectly(kkmId: String): Boolean

    /**
     * Получить текущее состояние очереди для указанной кассы.
     *
     * @param request Запрос с уникальным идентификатором ККМ.
     * @return Информация о статусе и размере очереди [QueueStatusResponse].
     */
    @Throws(Exception::class)
    fun getQueueStatus(request: QueueStatusRequest): QueueStatusResponse

    /**
     * Запускает фоновую обработку пакета отложенных команд для указанной ККМ.
     *
     * @param kkmId Уникальный идентификатор ККМ.
     * @param limit Максимальное количество команд для обработки за один вызов.
     * @return Количество успешно отправленных команд.
     */
    @Throws(Exception::class)
    fun processOfflineBatch(kkmId: String, limit: Int = 10): Int

    /**
     * Получить список задач из офлайн-очереди ОФД.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @return Список элементов очереди.
     */
    @Throws(Exception::class)
    fun listQueue(kkmId: String, pin: String): List<QueueItemResponse>

    /**
     * Перезапустить задачи со статусом FAILED.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @return Количество обновленных задач.
     */
    @Throws(Exception::class)
    fun retryFailed(kkmId: String, pin: String): Int
}
