package io.github.texport.superkassa.core.domain.usecase.report

import io.github.texport.superkassa.core.domain.model.auth.UserRole
import io.github.texport.superkassa.core.domain.model.delivery.DeliveryStatus
import io.github.texport.superkassa.core.domain.model.ofd.OfdCommandStatus
import io.github.texport.superkassa.core.domain.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.model.queue.OfflineQueueCommandRequest
import io.github.texport.superkassa.core.domain.model.report.ReportResult
import io.github.texport.superkassa.core.domain.port.IdGeneratorPort
import io.github.texport.superkassa.core.domain.port.OfflineQueuePort
import io.github.texport.superkassa.core.domain.port.StoragePort
import io.github.texport.superkassa.core.domain.usecase.auth.AuthorizeUserUseCase
import io.github.texport.superkassa.core.domain.usecase.kkm.RequireOperationalUseCase
import io.github.texport.superkassa.core.domain.usecase.ofd.SendFiscalCommandUseCase

/**
 * Сценарий (Use Case) создания X-отчета (отчета о текущем состоянии счетчиков без гашения) на ККМ.
 */
class ProcessReportUseCase(
    private val storage: StoragePort,
    private val queue: OfflineQueuePort,
    private val sendFiscalCommandUseCase: SendFiscalCommandUseCase,
    private val idGenerator: IdGeneratorPort,
    private val authorizeUser: AuthorizeUserUseCase,
    private val requireOperational: RequireOperationalUseCase
) {
    /**
     * Выполняет генерацию и фискализацию X-отчета.
     *
     * @param kkmId Уникальный идентификатор ККМ.
     * @param pin ПИН-код пользователя для проверки прав доступа.
     * @return [ReportResult] Результат выполнения операции.
     */
    fun execute(kkmId: String, pin: String): ReportResult {
        return storage.inTransaction {
            val kkm = authorizeUser.requireKkm(kkmId)
            requireOperational.execute(kkm)
            authorizeUser.execute(kkm.id, pin, setOf(UserRole.ADMIN, UserRole.CASHIER))

            val documentId = idGenerator.nextId()
            val hasQueue = !queue.canSendDirectly(kkmId)
            if (hasQueue) {
                val command = OfflineQueueCommandRequest(
                    kkmId = kkmId,
                    type = OfdCommandType.REPORT.value,
                    payloadRef = documentId
                )
                queue.enqueueOffline(command)
                ReportResult(
                    documentId = documentId,
                    deliveryStatus = DeliveryStatus.OFFLINE_QUEUED
                )
            } else {
                val result = sendFiscalCommandUseCase.execute(kkmId, OfdCommandType.REPORT, documentId)
                val (status, error) = when (result.status) {
                    OfdCommandStatus.OK -> DeliveryStatus.ONLINE_OK to null
                    OfdCommandStatus.TIMEOUT -> DeliveryStatus.OFFLINE_QUEUED to result.errorMessage
                    OfdCommandStatus.FAILED -> DeliveryStatus.ONLINE_ERROR to result.errorMessage
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
