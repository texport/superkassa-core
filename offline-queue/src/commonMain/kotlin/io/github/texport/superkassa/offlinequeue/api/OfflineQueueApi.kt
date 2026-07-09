package io.github.texport.superkassa.offlinequeue.api

import io.github.texport.superkassa.offlinequeue.api.model.QueueCommand
import io.github.texport.superkassa.offlinequeue.api.model.QueueLane
import io.github.texport.superkassa.offlinequeue.api.port.LeaseLockPort
import io.github.texport.superkassa.offlinequeue.api.port.QueueCommandHandlerPort
import io.github.texport.superkassa.offlinequeue.api.port.QueueStoragePort
import io.github.texport.superkassa.offlinequeue.impl.OfflineQueueApiImpl
import io.github.texport.superkassa.offlinequeue.impl.policy.DefaultBackoffPolicy

/**
 * Публичный интерфейс-фасад для взаимодействия с оффлайн-очередью кассы Superkassa.
 *
 * Предоставляет методы для постановки команд в очередь, получения статуса оффлайн-режима
 * и последовательной/пакетной обработки команд в фоновом режиме.
 */
interface OfflineQueueApi {
    /**
     * Помещает новую команду в оффлайн-очередь кассы.
     *
     * Метод потокобезопасен и может вызываться из любого потока.
     *
     * @param command объект команды, помещаемой в очередь.
     * @return true, если команда была успешно сохранена в хранилище, иначе false.
     */
    fun enqueue(command: QueueCommand): Boolean

    /**
     * Проверяет наличие ожидающих (неотправленных) оффлайн-команд для указанной кассы.
     *
     * Метод потокобезопасен.
     *
     * @param cashboxId уникальный идентификатор кассы.
     * @return true, если в очереди есть ожидающие обработки команды, иначе false.
     */
    fun hasOfflineQueue(cashboxId: String): Boolean

    /**
     * Очищает всю очередь команд для указанной кассы.
     *
     * Метод потокобезопасен.
     *
     * @param cashboxId уникальный идентификатор кассы.
     * @return true, если очередь была успешно очищена, иначе false.
     */
    fun clearQueue(cashboxId: String): Boolean

    /**
     * Захватывает эксклюзивную блокировку кассы и обрабатывает одну следующую готовую команду в канале (lane).
     *
     * Метод разработан для вызова из фонового потока (Worker/Coroutine). Обеспечивает
     * синхронизацию: параллельные вызовы для одной кассы будут заблокированы до освобождения аренды.
     *
     * @param cashboxId уникальный идентификатор обрабатываемой кассы.
     * @param lane линия очереди для обработки (например, OFFLINE или ONLINE).
     * @return true, если команда была найдена, успешно обработана и статус обновлен в хранилище, иначе false.
     */
    fun processNext(cashboxId: String, lane: QueueLane): Boolean

    /**
     * Захватывает эксклюзивную блокировку кассы и обрабатывает пачку готовых команд до указанного лимита.
     *
     * Метод разработан для фонового выполнения.
     *
     * @param cashboxId уникальный идентификатор обрабатываемой кассы.
     * @param lane линия очереди для обработки.
     * @param limit максимальное количество команд для обработки за один вызов метода.
     * @return количество успешно обработанных и подтвержденных команд.
     */
    fun processBatch(cashboxId: String, lane: QueueLane, limit: Int): Int
}

/**
 * Фабричная функция для создания экземпляра оффлайн-очереди.
 * Скрывает детали реализации, такие как политики задержек по умолчанию и внутренние провайдеры.
 *
 * @param storage реализация порта хранилища для оффлайн-очереди.
 * @param lockPort реализация порта распределенной или локальной аренды блокировок.
 * @param handler обработчик команд, выполняющий сетевые запросы или бизнес-логику.
 * @param ownerId уникальный идентификатор владельца/процесса, захватывающего блокировку.
 * @return настроенный экземпляр [OfflineQueue].
 */
fun createOfflineQueueApi(
    storage: QueueStoragePort,
    lockPort: LeaseLockPort,
    handler: QueueCommandHandlerPort,
    ownerId: String
): OfflineQueueApi {
    return OfflineQueueApiImpl(
        storage = storage,
        lockPort = lockPort,
        handler = handler,
        backoffPolicy = DefaultBackoffPolicy(),
        ownerId = ownerId
    )
}
