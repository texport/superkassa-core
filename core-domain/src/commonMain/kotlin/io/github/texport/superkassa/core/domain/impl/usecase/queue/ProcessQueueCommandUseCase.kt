package io.github.texport.superkassa.core.domain.impl.usecase.queue

import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandStatus
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.api.model.queue.QueueDispatchResult
import io.github.texport.superkassa.core.domain.api.model.queue.QueueDispatchStatus
import io.github.texport.superkassa.core.domain.api.model.queue.QueueTask
import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.usecase.ofd.SendFiscalCommandUseCase

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
     * Отправляет команду в ОФД, анализирует результат и возвращает [QueueDispatchResult]
     * с новым статусом задачи и временем для повторной попытки в случае сбоя.
     *
     * @param command Команда из очереди [QueueTask], подлежащая обработке.
     * @return Результат диспетчеризации [QueueDispatchResult] со статусом выполнения.
     */
    fun execute(command: QueueTask): QueueDispatchResult {
        val ofdType = toOfdCommandType(command.type)
        val result = sendFiscalCommand.execute(command.cashboxId, ofdType, command.payloadRef)
        
        updateKkmBlockedStateFromOfd(command.cashboxId, result.resultCode, clock.now())
        
        return when (result.status) {
            OfdCommandStatus.OK -> {
                val code = result.resultCode
                if (code == 254 || code == 255) {
                    val trilingual = CoreStrings.ofdTimeout()
                    QueueDispatchResult(
                        status = QueueDispatchStatus.FAILED,
                        errorMessage = "Service temporarily unavailable ($code)",
                        retryAt = clock.now() + 30_000,
                        errorRu = trilingual.ru,
                        errorKk = trilingual.kk,
                        errorEn = trilingual.en
                    )
                } else if (code != null && code != 0 && code != 13 && code != 14 && code != 17) {
                    val errorMsg = "OFD returned code $code"
                    val trilingual = CoreStrings.ofdDeliveryFailure(errorMsg)
                    QueueDispatchResult(
                        status = QueueDispatchStatus.FAILED,
                        errorMessage = errorMsg,
                        retryAt = clock.now() + 60_000,
                        errorRu = trilingual.ru,
                        errorKk = trilingual.kk,
                        errorEn = trilingual.en
                    )
                } else {
                    updateDocumentOnSuccess(command, result)
                    QueueDispatchResult(QueueDispatchStatus.SENT)
                }
            }
            OfdCommandStatus.FAILED -> {
                val errorMsg = result.errorMessage ?: "OFD command failed"
                val trilingual = CoreStrings.ofdDeliveryFailure(errorMsg)
                QueueDispatchResult(
                    status = QueueDispatchStatus.FAILED,
                    errorMessage = errorMsg,
                    retryAt = clock.now() + 60_000,
                    errorRu = trilingual.ru,
                    errorKk = trilingual.kk,
                    errorEn = trilingual.en
                )
            }
            OfdCommandStatus.TIMEOUT -> {
                val trilingual = CoreStrings.ofdTimeout()
                QueueDispatchResult(
                    status = QueueDispatchStatus.FAILED,
                    errorMessage = "OFD timeout",
                    retryAt = clock.now() + 30_000,
                    errorRu = trilingual.ru,
                    errorKk = trilingual.kk,
                    errorEn = trilingual.en
                )
            }
        }
    }

    /**
     * Преобразует строковый тип команды очереди в тип фискальной команды ОФД [OfdCommandType].
     */
    private fun toOfdCommandType(type: String): OfdCommandType = when (type) {
        "TICKET" -> OfdCommandType.TICKET
        "MONEY_PLACEMENT" -> OfdCommandType.MONEY_PLACEMENT
        "REPORT_X", "REPORT_Z" -> OfdCommandType.REPORT
        "CLOSE_SHIFT" -> OfdCommandType.CLOSE_SHIFT
        "INFO" -> OfdCommandType.INFO
        else -> OfdCommandType.SYSTEM
    }

    private fun updateDocumentOnSuccess(command: QueueTask, result: OfdCommandResult) {
        if (command.type != "TICKET" && command.type != "MONEY_PLACEMENT") return
        
        val code = result.resultCode
        val success = code == 0
        val isFailed = code == 13 || code == 14 || code == 17
        val status = if (success) "SENT" else if (isFailed) "FAILED" else "PENDING"
        
        if (status == "PENDING") return // Do not update if we didn't succeed or permanently fail
        
        val now = clock.now()
        val doc = storage.findFiscalDocumentById(command.payloadRef)
        storage.updateReceiptStatus(
            documentId = command.payloadRef,
            fiscalSign = result.fiscalSign,
            autonomousSign = doc?.autonomousSign ?: result.autonomousSign,
            ofdStatus = status,
            ofdErrorCode = if (isFailed) code else null,
            deliveredAt = if (success) now else null,
            isAutonomous = false
        )
    }

    /**
     * Обновляет состояние блокировки ККМ на основе ответа ОФД.
     */
    private fun updateKkmBlockedStateFromOfd(cashboxId: String, resultCode: Int?, now: Long) {
        val code = resultCode ?: return
        val kkm = storage.findKkmForUpdate(cashboxId) ?: return
        val shouldBlock = code in 1..7 || code == 11 || code == 12 || code == 15
        if (shouldBlock && kkm.state != "BLOCKED") {
            storage.updateKkm(
                kkm.copy(
                    updatedAt = now,
                    state = "BLOCKED",
                    blockReasonCode = code + 1000
                )
            )
        } else if (code == 0 && kkm.state == "BLOCKED" && (kkm.blockReasonCode ?: 0) >= 1000) {
            storage.updateKkm(
                kkm.copy(
                    updatedAt = now,
                    state = "ACTIVE",
                    blockReasonCode = null
                )
            )
        }
    }
}
