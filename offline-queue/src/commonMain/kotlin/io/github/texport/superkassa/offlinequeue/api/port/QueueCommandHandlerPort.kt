package io.github.texport.superkassa.offlinequeue.api.port

import io.github.texport.superkassa.offlinequeue.api.model.DispatchResult
import io.github.texport.superkassa.offlinequeue.api.model.QueueCommand

/**
 * Функциональный интерфейс для отправки/обработки отдельной команды из очереди.
 *
 * Связывает оффлайн-очередь с транспортным слоем или прикладным сценарием выполнения команд.
 */
fun interface QueueCommandHandlerPort {
    /**
     * Выполняет обработку одной команды очереди.
     *
     * Метод вызывается внутри транзакции блокировки аренды кассы из фонового потока.
     * Возбуждение любых исключений внутри метода handle перехватывается очередью и трактуется как сбой отправки.
     *
     * @param command объект обрабатываемой команды из очереди.
     * @param renewLock функция обратного вызова для продления аренды блокировки кассы (актуально для длительных операций).
     *                  Возвращает true, если аренда успешно продлена, иначе false.
     * @return результат обработки [DispatchResult] со статусом выполнения и деталями ошибки.
     */
    fun handle(command: QueueCommand, renewLock: () -> Boolean): DispatchResult
}
