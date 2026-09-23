package io.github.texport.superkassa.core.domain.impl.usecase.queue

import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandStatus
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.api.model.queue.QueueDispatchResult
import io.github.texport.superkassa.core.domain.api.model.queue.QueueDispatchStatus
import io.github.texport.superkassa.core.domain.api.model.queue.QueueTask
import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.string.api.TrilingualMessage
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

        val code = result.resultCode
        return when {
            // Нет ответа, 254 или 255: по спецификации (п. 5.2) документ досылается
            // снова через интервал восстановления связи, сколько бы это ни длилось.
            result.status == OfdCommandStatus.TIMEOUT || code == SERVICE_TEMPORARILY_UNAVAILABLE || code == UNKNOWN_ERROR ->
                retry(CoreStrings.ofdTimeout(), "BFD timeout", after = true)
            code == RESULT_OK -> {
                updateDocumentOnSuccess(command, result)
                QueueDispatchResult(QueueDispatchStatus.SENT)
            }
            code != null -> rejected(command, code, result.resultText)
            // Обмена не было: запрос не удалось ни собрать, ни отправить.
            // Это состояние кассы, а не негодный документ, поэтому повтор.
            else -> (result.errorMessage ?: "BFD command failed").let { retry(CoreStrings.ofdDeliveryFailure(it), it) }
        }
    }

    /**
     * Ответ получен, и он отказной.
     *
     * Повтора не будет: по спецификации (п. 5.2) любой код, кроме 0, 254
     * и 255, означает негодный документ, а не временную помеху. Раньше
     * такой ответ приходил сюда как сбой отправки и повторялся без конца —
     * документ вставал в голове очереди, и за ним стояли все следующие.
     */
    private fun rejected(command: QueueTask, code: Int, reason: String?): QueueDispatchResult {
        markDocumentRejected(command, code, reason)
        val error = CoreStrings.ofdDeliveryFailure("BFD returned code $code")
        return QueueDispatchResult(
            status = QueueDispatchStatus.REJECTED,
            errorMessage = "BFD returned code $code",
            errorRu = error.ru,
            errorKk = error.kk,
            errorEn = error.en
        )
    }

    /** Повтор: без ответа — через паузу, при сбое самой кассы — по расписанию очереди. */
    private fun retry(error: TrilingualMessage, message: String, after: Boolean = false) = QueueDispatchResult(
        status = QueueDispatchStatus.FAILED,
        errorMessage = message,
        retryAt = if (after) clock.now() + RETRY_DELAY_MILLIS else null,
        errorRu = error.ru,
        errorKk = error.kk,
        errorEn = error.en
    )

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
        // команд его нет — по ссылке ничего не найдётся.
        val doc = storage.findFiscalDocumentById(command.payloadRef) ?: return
        storage.updateReceiptStatus(
            documentId = command.payloadRef,
            fiscalSign = result.fiscalSign,
            autonomousSign = doc.autonomousSign ?: result.autonomousSign,
            ofdStatus = "SENT",
            ofdErrorCode = null,
            deliveredAt = clock.now(),
            // Признак автономности снимать нельзя: он говорит не о том, доставлен
            // ли документ, а о том, что он был фискализирован в разрыве связи.
            isAutonomous = doc.isAutonomous,
            ofdErrorText = null
        )
        // Досланный из очереди чек получает ссылку только теперь: без неё
        // перепечатанный чек выходил без QR-кода проверки.
        result.receiptUrl?.let { storage.saveReceiptUrl(command.payloadRef, it) }
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

        /** Пауза перед повтором досылки. */
        const val RETRY_DELAY_MILLIS = 30_000L
    }
}
