package kz.mybrain.superkassa.core.data.adapter

import kz.mybrain.superkassa.core.domain.port.ClockPort
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.core.domain.usecase.ofd.SendFiscalCommandUseCase
import kz.mybrain.superkassa.core.domain.usecase.queue.ProcessQueueCommandUseCase
import kz.mybrain.superkassa.offline_queue.application.model.DispatchResult
import kz.mybrain.superkassa.offline_queue.application.service.QueueCommandHandler
import kz.mybrain.superkassa.offline_queue.domain.model.QueueCommand

/**
 * Реализация QueueCommandHandler — отправляет команды из очереди в ОФД.
 * Делегирует выполнение сценарию [ProcessQueueCommandUseCase].
 */
@Suppress("unused") // Создается динамически фреймворком Spring как компонент системы
class OfdQueueCommandHandlerAdapter(
    sendFiscalCommand: SendFiscalCommandUseCase,
    storage: StoragePort,
    clock: ClockPort
) : QueueCommandHandler {
    private val processUseCase = ProcessQueueCommandUseCase(sendFiscalCommand, storage, clock)

    override fun handle(command: QueueCommand): DispatchResult {
        return processUseCase.execute(command)
    }
}
