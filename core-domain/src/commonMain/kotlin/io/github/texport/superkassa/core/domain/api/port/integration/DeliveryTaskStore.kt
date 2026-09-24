package io.github.texport.superkassa.core.domain.api.port.integration

import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryTask

/**
 * Задачи доставки чека покупателю в хранилище кассы.
 *
 * Часть [StoragePort]. Хранилище, которое задач не держит, отвечает на
 * каждый вызов [UnsupportedOperationException] — так делает реализация
 * по умолчанию, и ядро тогда доставляет чек сразу, в потоке пробития,
 * как до появления задач. Хранилище, которое их держит, реализует все
 * методы вместе: одни без других фон не досылает.
 *
 * Вызовы блокирующие и безопасны из нескольких потоков: фон и ручной
 * повтор обращаются к одной задаче одновременно.
 */
interface DeliveryTaskStore {

    /**
     * Ставит задачи. Задача с уже известным идентификатором остаётся как
     * была — доставленная не превращается снова в ожидающую.
     *
     * @throws UnsupportedOperationException если хранилище задач не держит.
     */
    fun addDeliveryTasks(tasks: List<DeliveryTask>): Unit = unsupported()

    /**
     * Ожидающие задачи всех касс, срок которых наступил к [now], — самые
     * давние первыми, не больше [limit].
     *
     * @throws UnsupportedOperationException если хранилище задач не держит.
     */
    fun dueDeliveryTasks(now: Long, limit: Int): List<DeliveryTask> = unsupported()

    /**
     * Занимает задачу под отправку: только ожидающую, чей срок наступил.
     * Одним изменением записи увеличивает число попыток и переносит срок
     * на [leaseUntil] — второй отправитель ту же задачу не займёт.
     *
     * @return `true`, если задача занята этим вызовом.
     * @throws UnsupportedOperationException если хранилище задач не держит.
     */
    fun claimDeliveryTask(id: String, now: Long, leaseUntil: Long): Boolean = unsupported()

    /**
     * Записывает задачу целиком поверх записанной с тем же идентификатором.
     *
     * @throws UnsupportedOperationException если хранилище задач не держит.
     */
    fun saveDeliveryTask(task: DeliveryTask): Unit = unsupported()

    /**
     * Задачи документа [documentId] по всем каналам.
     *
     * @throws UnsupportedOperationException если хранилище задач не держит.
     */
    fun deliveryTasksOf(documentId: String): List<DeliveryTask> = unsupported()
}

private fun unsupported(): Nothing =
    throw UnsupportedOperationException("Delivery tasks are not supported by this storage")
