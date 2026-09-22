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
                    // Ответ получен, и он отказной. Документ фискальным не стал,
                    // и оставлять ему «ожидает отправки» нельзя: в журнале
                    // он висел бы так, пока задача бесконечно повторяется.
                    markDocumentRejected(command, code, result.resultText)
                    val errorMsg = "BFD returned code $code"
                    val trilingual = CoreStrings.ofdDeliveryFailure(errorMsg)
                    // Повтора не будет: по спецификации любой код, кроме 0,
                    // 254 и 255, означает негодный документ, а не временную
                    // помеху. Повторять его — занимать очередь навсегда.
                    QueueDispatchResult(
                        status = QueueDispatchStatus.REJECTED,
                        errorMessage = errorMsg,
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
                val errorMsg = result.errorMessage ?: "BFD command failed"
                val trilingual = CoreStrings.ofdDeliveryFailure(errorMsg)
                // Обмена не было: запрос не удалось ни собрать, ни отправить.
                // Это состояние кассы, а не негодный документ: X-отчёт,
                // снятый без связи, не собирался после закрытия смены
                // и уходил в отбраковку навсегда — фискальный документ
                // терялся молча. Поэтому здесь повтор, а не отбраковка;
                // бесконечным он не станет — число попыток ограничено,
                // и исчерпавшая их задача остаётся видимой в очереди.
                QueueDispatchResult(
                    status = QueueDispatchStatus.FAILED,
                    errorMessage = errorMsg,
                    errorRu = trilingual.ru,
                    errorKk = trilingual.kk,
                    errorEn = trilingual.en
                )
            }
            OfdCommandStatus.TIMEOUT -> {
                val trilingual = CoreStrings.ofdTimeout()
                QueueDispatchResult(
                    status = QueueDispatchStatus.FAILED,
                    errorMessage = "BFD timeout",
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
        "REPORT_X", "X_REPORT", "REPORT_Z" -> OfdCommandType.REPORT
        "CLOSE_SHIFT" -> OfdCommandType.CLOSE_SHIFT
        "INFO" -> OfdCommandType.INFO
        else -> OfdCommandType.SYSTEM
    }

    private fun updateDocumentOnSuccess(command: QueueTask, result: OfdCommandResult) {
        // Документ есть у чека, денег, отчёта и закрытия смены; у служебных
        // команд его нет — по ссылке ничего не найдётся. Раньше здесь стоял
        // список из двух типов, и Z-отчёт, досланный из очереди, навсегда
        // оставался «в очереди», хотя ОФД его принял.
        val doc = storage.findFiscalDocumentById(command.payloadRef) ?: return

        // Сюда приходят только ответы, снимающие задачу с очереди: приём (0)
        // и окончательные отказы. Оставлять документ в PENDING после
        // полученного ответа нельзя — он больше никем не будет подхвачен.
        val code = result.resultCode
        val success = code == 0
        val status = if (success) "SENT" else "FAILED"

        val now = clock.now()
        storage.updateReceiptStatus(
            documentId = command.payloadRef,
            fiscalSign = result.fiscalSign,
            autonomousSign = doc.autonomousSign ?: result.autonomousSign,
            ofdStatus = status,
            ofdErrorCode = if (success) null else code,
            deliveredAt = if (success) now else null,
            // Признак автономности снимать нельзя: он говорит не о том, доставлен
            // ли документ, а о том, что он был фискализирован в разрыве связи.
            // Доставка снимает PENDING, но истории оформления не отменяет.
            isAutonomous = doc.isAutonomous,
            ofdErrorText = if (success) null else result.resultText?.takeIf { it.isNotBlank() }
        )
    }

    /**
     * Помечает документ отвергнутым с кодом отказа ОФД.
     */
    private fun markDocumentRejected(command: QueueTask, code: Int, reason: String?) {
        val doc = storage.findFiscalDocumentById(command.payloadRef) ?: return
        storage.updateReceiptStatus(
            documentId = command.payloadRef,
            fiscalSign = null,
            autonomousSign = doc.autonomousSign,
            ofdStatus = "FAILED",
            ofdErrorCode = code,
            deliveredAt = null,
            // Признак автономности снимать нельзя: он говорит о том, что
            // документ был оформлен в разрыве связи, а не о его доставке.
            isAutonomous = doc.isAutonomous,
            ofdErrorText = reason?.takeIf { it.isNotBlank() }
        )
    }

    /**
     * Обновляет состояние блокировки ККМ на основе ответа ОФД.
     */
    private fun updateKkmBlockedStateFromOfd(cashboxId: String, resultCode: Int?, now: Long) {
        val code = resultCode ?: return
        val kkm = storage.findKkmForUpdate(cashboxId) ?: return
        // Правило досылки накопленной очереди из спецификации CPCR, раздел
        // «Работа в автономном режиме»: любой ответ, кроме OK, временной
        // недоступности сервиса и неизвестной ошибки, переводит кассу
        // в блокировку. Раньше коды 13, 14 и 17 снимали документ с очереди
        // и касса продолжала работать, будто ничего не произошло.
        val shouldBlock = code != RESULT_OK &&
            code != SERVICE_TEMPORARILY_UNAVAILABLE &&
            code != UNKNOWN_ERROR
        if (shouldBlock && kkm.state != "BLOCKED") {
            storage.updateKkm(
                kkm.copy(
                    updatedAt = now,
                    state = "BLOCKED",
                    blockReasonCode = code + 1000
                )
            )
        } else if (code == RESULT_OK && kkm.state == "BLOCKED" && (kkm.blockReasonCode ?: 0) >= 1000) {
            storage.updateKkm(
                kkm.copy(
                    updatedAt = now,
                    state = "ACTIVE",
                    blockReasonCode = null
                )
            )
        }
    }

    private companion object {
        /** Команда выполнена успешно. */
        const val RESULT_OK = 0

        /** Сервис временно недоступен: отправку следует повторить. */
        const val SERVICE_TEMPORARILY_UNAVAILABLE = 254

        /** Неизвестная ошибка: отправку следует повторить. */
        const val UNKNOWN_ERROR = 255
    }
}
