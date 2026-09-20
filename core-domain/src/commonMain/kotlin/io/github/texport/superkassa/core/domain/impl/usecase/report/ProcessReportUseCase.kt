package io.github.texport.superkassa.core.domain.impl.usecase.report

import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptDocumentTypes
import io.github.texport.superkassa.core.domain.impl.helper.common.assignPrintedDocumentNumber
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryStatus
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandStatus
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.api.model.queue.OfflineQueueCommandRequest
import io.github.texport.superkassa.core.domain.api.model.report.ReportResult
import io.github.texport.superkassa.core.domain.api.port.internal.IdGeneratorPort
import io.github.texport.superkassa.core.domain.api.port.internal.OfflineQueuePort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.api.port.integration.inTransaction
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

            val documentId = idGenerator.nextId()

            val shift = storage.findOpenShift(kkmId)
            val hasQueue = !queue.canSendDirectly(kkmId)

            val now = clock.now()
            storage.saveShiftDocument(
                kkmId = kkmId,
                type = ReceiptDocumentTypes.X_REPORT,
                documentId = documentId,
                shiftId = shift?.id ?: "0",
                createdAt = now
            )
            assignPrintedDocumentNumber(storage, kkmId, documentId)

            if (hasQueue) {
                val command = OfflineQueueCommandRequest(
                    kkmId = kkmId,
                    type = OfdCommandType.REPORT.value,
                    payloadRef = documentId
                )
                queue.enqueueOffline(command)

                storage.updateReceiptStatus(
                    documentId = documentId,
                    fiscalSign = null,
                    autonomousSign = null,
                    ofdStatus = "PENDING",
                    deliveredAt = null,
                    isAutonomous = true
                )

                ReportResult(
                    documentId = documentId,
                    deliveryStatus = DeliveryStatus.OFFLINE_QUEUED
                )
            } else {
                val result = sendFiscalCommandUseCase.execute(kkmId, OfdCommandType.REPORT, documentId)
                val (status, error) = when (result.status) {
                    OfdCommandStatus.OK -> {
                        storage.updateReceiptStatus(
                            documentId = documentId,
                            fiscalSign = null,
                            autonomousSign = null,
                            ofdStatus = "SENT",
                            deliveredAt = now,
                            isAutonomous = false
                        )
                        DeliveryStatus.ONLINE_OK to null
                    }
                    OfdCommandStatus.TIMEOUT -> {
                        storage.updateReceiptStatus(
                            documentId = documentId,
                            fiscalSign = null,
                            autonomousSign = null,
                            ofdStatus = "PENDING",
                            deliveredAt = null,
                            isAutonomous = false
                        )
                        DeliveryStatus.OFFLINE_QUEUED to result.errorMessage
                    }
                    OfdCommandStatus.FAILED -> {
                        storage.updateReceiptStatus(
                            documentId = documentId,
                            fiscalSign = null,
                            autonomousSign = null,
                            ofdStatus = "FAILED",
                            deliveredAt = null,
                            isAutonomous = false
                        )
                        DeliveryStatus.ONLINE_ERROR to result.errorMessage
                    }
                }
                ReportResult(
                    documentId = documentId,
                    deliveryStatus = status,
                    deliveryError = error
                )
            }
        }
    }
}
