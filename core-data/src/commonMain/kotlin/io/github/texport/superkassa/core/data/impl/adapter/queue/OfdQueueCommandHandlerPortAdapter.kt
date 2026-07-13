package io.github.texport.superkassa.core.data.impl.adapter.queue

import io.github.texport.superkassa.core.domain.api.model.queue.QueueDispatchStatus
import io.github.texport.superkassa.core.domain.api.model.queue.QueueTask
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.usecase.ofd.SendFiscalCommandUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.queue.ProcessQueueCommandUseCase
import io.github.texport.superkassa.core.string.api.TrilingualMessage
import io.github.texport.superkassa.offlinequeue.api.model.DispatchResult
import io.github.texport.superkassa.offlinequeue.api.model.QueueCommand
import io.github.texport.superkassa.offlinequeue.api.model.QueueStatus
import io.github.texport.superkassa.offlinequeue.api.port.QueueCommandHandlerPort

/**
 * Реализация QueueCommandHandlerPort — отправляет команды из очереди в ОФД.
 * Делегирует выполнение сценарию [ProcessQueueCommandUseCase] доменного слоя.
 */
internal class OfdQueueCommandHandlerPortAdapter(
    sendFiscalCommand: SendFiscalCommandUseCase,
    storage: StoragePort,
    clock: ClockPort
) : QueueCommandHandlerPort {
    private val processUseCase = ProcessQueueCommandUseCase(sendFiscalCommand, storage, clock)

    override fun handle(command: QueueCommand, renewLock: () -> Boolean): DispatchResult {
        val task = QueueTask(
            id = command.id,
            cashboxId = command.cashboxId,
            lane = command.lane.name,
            type = command.type.name,
            payloadRef = command.payloadRef,
            status = command.status.name,
            attempt = command.attempt,
            nextAttemptAt = command.nextAttemptAt,
            lastError = command.lastError,
            createdAt = command.createdAt
        )

        val result = processUseCase.execute(task)

        return DispatchResult(
            status = when (result.status) {
                QueueDispatchStatus.SENT -> QueueStatus.SENT
                QueueDispatchStatus.FAILED -> QueueStatus.FAILED
            },
            errorMessage = result.errorMessage,
            retryAt = result.retryAt,
            error = if (result.errorRu != null || result.errorKk != null || result.errorEn != null) {
                TrilingualMessage(
                    ru = result.errorRu ?: "",
                    kk = result.errorKk ?: "",
                    en = result.errorEn ?: ""
                )
            } else {
                null
            }
        )
    }
}
