package io.github.texport.superkassa.core.domain.impl.usecase.report

import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptDocumentTypes
import io.github.texport.superkassa.core.domain.impl.helper.common.assignPrintedDocumentNumber
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryStatus
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandStatus
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.api.model.queue.OfflineQueueCommandRequest
import io.github.texport.superkassa.core.domain.api.model.report.ReportResult
import io.github.texport.superkassa.core.domain.api.port.internal.IdGeneratorPort
import io.github.texport.superkassa.core.domain.api.port.internal.OfflineQueuePort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.api.port.integration.inTransaction
import io.github.texport.superkassa.core.domain.impl.helper.ofd.BfdDeliveryFailure
import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.kkm.RequireOperationalUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.ofd.SendFiscalCommandUseCase

import io.github.texport.superkassa.core.domain.impl.logging.getLogger

/**
 * Сценарий (Use Case) создания X-отчета (отчета о текущем состоянии счетчиков без гашения) на ККМ.
 */
class ProcessReportUseCase(
    private val storage: StoragePort,
    private val queue: OfflineQueuePort,
    private val sendFiscalCommandUseCase: SendFiscalCommandUseCase,
    private val idGenerator: IdGeneratorPort,
    private val authorizeUser: AuthorizeUserUseCase,
    private val requireOperational: RequireOperationalUseCase,
    private val clock: io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
) {
    private val logger = getLogger(ProcessReportUseCase::class)

    /**
     * Выполняет генерацию и фискализацию X-отчета.
     *
     * @param kkmId Уникальный идентификатор ККМ.
     * @param pin ПИН-код пользователя для проверки прав доступа.
     * @return [ReportResult] Результат выполнения операции.
     */
    fun execute(kkmId: String, pin: String): ReportResult {
        logger.info("ProcessReportUseCase: executing X-report for kkmId='{}'", kkmId)
        return storage.inTransaction {
            val kkm = authorizeUser.requireKkm(kkmId)
            requireOperational.execute(kkm)
            authorizeUser.execute(kkm.id, pin, setOf(UserRole.ADMIN, UserRole.CASHIER))

            val hasQueue = !queue.canSendDirectly(kkmId)
            val now = clock.now()
            val documentId = saveReport(kkmId, now)
            if (hasQueue) return@inTransaction queued(kkmId, documentId, answer = null)

            val result = sendFiscalCommandUseCase.execute(kkmId, OfdCommandType.REPORT, documentId)
            when (result.status) {
                OfdCommandStatus.OK -> answered(documentId, "SENT", deliveredAt = now, DeliveryStatus.ONLINE_OK, result)
                // Без ответа отчёт досылается, как любой документ: раньше он
                // оставался «ожидает отправки» вне очереди, а ответ кассиру
                // говорил «в очереди».
                OfdCommandStatus.TIMEOUT -> queued(kkmId, documentId, result)
                OfdCommandStatus.FAILED ->
                    answered(documentId, "FAILED", deliveredAt = null, DeliveryStatus.ONLINE_ERROR, result)
            }
        }
    }

    private fun saveReport(kkmId: String, now: Long): String {
        val documentId = idGenerator.nextId()
        val shift = storage.findOpenShift(kkmId)
        storage.saveShiftDocument(
            kkmId = kkmId,
            type = ReceiptDocumentTypes.X_REPORT,
            documentId = documentId,
            shiftId = shift?.id ?: "0",
            createdAt = now
        )
        assignPrintedDocumentNumber(storage, kkmId, documentId)
        return documentId
    }

    /** Отчёт встаёт в очередь досылки и помечается снятым без связи. */
    private fun queued(kkmId: String, documentId: String, answer: OfdCommandResult?): ReportResult {
        queue.enqueueOffline(OfflineQueueCommandRequest(kkmId, OfdCommandType.REPORT.value, documentId))
        storage.updateReceiptStatus(
            documentId = documentId,
            fiscalSign = null,
            autonomousSign = null,
            ofdStatus = "PENDING",
            deliveredAt = null,
            isAutonomous = true
        )
        return reportResult(documentId, DeliveryStatus.OFFLINE_QUEUED, answer)
    }

    private fun answered(
        documentId: String,
        ofdStatus: String,
        deliveredAt: Long?,
        status: DeliveryStatus,
        answer: OfdCommandResult
    ): ReportResult {
        storage.updateReceiptStatus(
            documentId = documentId,
            fiscalSign = null,
            autonomousSign = null,
            ofdStatus = ofdStatus,
            deliveredAt = deliveredAt,
            isAutonomous = false
        )
        return reportResult(documentId, status, answer)
    }

    /** Ответ кассиру: отказ БФД словами и кодом; до попытки обмена отказа нет. */
    private fun reportResult(documentId: String, status: DeliveryStatus, answer: OfdCommandResult?) = ReportResult(
        documentId = documentId,
        deliveryStatus = status,
        deliveryError = answer?.let(BfdDeliveryFailure::reason),
        bfdResultCode = answer?.let(BfdDeliveryFailure::code)
    )
}
