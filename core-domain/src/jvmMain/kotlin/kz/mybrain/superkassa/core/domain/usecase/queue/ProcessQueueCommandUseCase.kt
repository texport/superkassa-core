package kz.mybrain.superkassa.core.domain.usecase.queue

import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandResult
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandStatus
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandType
import kz.mybrain.superkassa.core.domain.port.ClockPort
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.core.domain.usecase.ofd.SendFiscalCommandUseCase
import kz.mybrain.superkassa.offline_queue.application.model.DispatchResult
import kz.mybrain.superkassa.offline_queue.domain.model.QueueCommand
import kz.mybrain.superkassa.offline_queue.domain.model.QueueCommandType
import kz.mybrain.superkassa.offline_queue.domain.model.QueueStatus

/**
 * Сценарий (Use Case) обработки офлайн-команды из очереди для отправки в ОФД.
 *
 * Отвечает за отправку фискального документа или команды (например, чека, X/Z-отчета) на сервер ОФД,
 * обработку результата отправки и обновление статуса локального документа при успешной доставке.
 *
 * @property sendFiscalCommand Сценарий отправки фискальных команд в ОФД.
 * @property storage Порт для доступа к локальному хранилищу данных ККМ и документов.
 * @property clock Порт для работы с системным временем.
 */
class ProcessQueueCommandUseCase(
    private val sendFiscalCommand: SendFiscalCommandUseCase,
    private val storage: StoragePort,
    private val clock: ClockPort
) {
    /**
     * Выполняет обработку команды из очереди.
     *
     * Отправляет команду в ОФД, анализирует результат и возвращает [DispatchResult]
     * с новым статусом задачи и временем для повторной попытки в случае сбоя.
     *
     * @param command Команда из очереди [QueueCommand], подлежащая обработке.
     * @return Результат диспетчеризации [DispatchResult] со статусом выполнения.
     */
    fun execute(command: QueueCommand): DispatchResult {
        val ofdType = toOfdCommandType(command.type)
        val result = sendFiscalCommand.execute(command.cashboxId, ofdType, command.payloadRef)
        return when (result.status) {
            OfdCommandStatus.OK -> {
                updateDocumentOnSuccess(command, result)
                DispatchResult(QueueStatus.SENT)
            }
            OfdCommandStatus.FAILED -> DispatchResult(
                QueueStatus.FAILED,
                errorMessage = result.errorMessage,
                retryAt = clock.now() + 60_000
            )
            OfdCommandStatus.TIMEOUT -> DispatchResult(
                QueueStatus.FAILED,
                errorMessage = "OFD timeout",
                retryAt = clock.now() + 30_000
            )
        }
    }

    /**
     * Преобразует тип команды очереди [QueueCommandType] в тип фискальной команды ОФД [OfdCommandType].
     */
    private fun toOfdCommandType(type: QueueCommandType): OfdCommandType = when (type) {
        QueueCommandType.TICKET -> OfdCommandType.TICKET
        QueueCommandType.MONEY_PLACEMENT -> OfdCommandType.MONEY_PLACEMENT
        QueueCommandType.REPORT_X, QueueCommandType.REPORT_Z -> OfdCommandType.REPORT
        QueueCommandType.CLOSE_SHIFT -> OfdCommandType.CLOSE_SHIFT
        QueueCommandType.INFO -> OfdCommandType.INFO
        QueueCommandType.SYSTEM -> OfdCommandType.SYSTEM
    }

    /**
     * Обновляет статус локального документа в БД при успешной отправке в ОФД.
     *
     * Актуально только для чеков (TICKET) и операций с наличными (MONEY_PLACEMENT).
     */
    private fun updateDocumentOnSuccess(command: QueueCommand, result: OfdCommandResult) {
        if (command.type != QueueCommandType.TICKET && command.type != QueueCommandType.MONEY_PLACEMENT) return
        val now = clock.now()
        storage.updateReceiptStatus(
            documentId = command.payloadRef,
            fiscalSign = result.fiscalSign,
            autonomousSign = result.autonomousSign,
            ofdStatus = "SENT",
            deliveredAt = now,
            isAutonomous = false
        )
    }
}
