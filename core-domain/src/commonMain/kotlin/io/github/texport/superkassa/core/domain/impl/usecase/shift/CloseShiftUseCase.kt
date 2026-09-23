package io.github.texport.superkassa.core.domain.impl.usecase.shift

import io.github.texport.superkassa.core.domain.api.exception.ConflictException
import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryStatus
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmState
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandStatus
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.api.model.queue.OfflineQueueCommandRequest
import io.github.texport.superkassa.core.domain.api.model.report.ReportResult
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftStatus
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.api.port.integration.inTransaction
import io.github.texport.superkassa.core.domain.api.port.internal.IdGeneratorPort
import io.github.texport.superkassa.core.domain.api.port.internal.OfflineQueuePort
import io.github.texport.superkassa.core.domain.impl.helper.common.assignPrintedDocumentNumber
import io.github.texport.superkassa.core.domain.impl.logging.getLogger
import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.ofd.SendFiscalCommandUseCase
import io.github.texport.superkassa.core.string.api.CoreStrings

/**
 * Закрытие смены Z-отчётом — фискальная операция.
 *
 * Правила: касса есть и не в программировании, закрывает администратор
 * или кассир (или сама касса — автозакрытие), смена открыта.
 *
 * Смена закрывается, когда Z-отчёт принят БФД или ушёл в автономную
 * очередь: протокол разрешает закрывать смену без связи (CPCR,
 * `CloseShiftRequest.is_offline`). Отказ БФД смену не закрывает:
 * прежде касса закрывала её при любом ответе, а у БФД она оставалась
 * открытой, и следующая смена кассы расходилась с БФД. Отклонённый
 * Z-отчёт остаётся в журнале с причиной отказа.
 *
 * Изъятие при закрытии едет внутри Z-отчёта — см. [ShiftCloseWithdrawal].
 */
class CloseShiftUseCase(
    private val storage: StoragePort,
    private val queue: OfflineQueuePort,
    private val sendFiscalCommandUseCase: SendFiscalCommandUseCase,
    private val idGenerator: IdGeneratorPort,
    private val clock: ClockPort,
    private val authorizeUser: AuthorizeUserUseCase,
    recalculate: RecalculateShiftCountersUseCase = RecalculateShiftCountersUseCase(storage)
) {
    private val logger = getLogger(CloseShiftUseCase::class)
    private val withdrawal = ShiftCloseWithdrawal(storage, idGenerator, recalculate)

    /**
     * Закрывает смену по просьбе кассира.
     *
     * @throws ValidationException если ККМ не найдена или находится в режиме программирования.
     * @throws ConflictException если смена на ККМ не открыта.
     */
    fun execute(kkmId: String, pin: String): ReportResult = storage.inTransaction {
        val kkm = lockKkm(kkmId)
        authorizeUser.execute(kkm.id, pin, setOf(UserRole.ADMIN, UserRole.CASHIER))
        close(kkm, requireOpenShift(kkmId))
    }

    /** Закрывает смену без кассира — автозакрытием; правила и исход те же. */
    fun executeBySystem(kkmId: String): ReportResult = storage.inTransaction {
        val kkm = lockKkm(kkmId)
        close(kkm, requireOpenShift(kkmId))
    }

    private fun close(kkm: KkmInfo, shift: ShiftInfo): ReportResult {
        val now = clock.now()
        val withdrawalId = withdrawal.record(kkm, shift, now)
        val documentId = idGenerator.nextId()
        storage.saveShiftDocument(kkm.id, "SHIFT_CLOSE", documentId, shift.id, now)
        assignPrintedDocumentNumber(storage, kkm.id, documentId)
        val (result, autonomous) = deliver(kkm.id, documentId, now)
        if (result.deliveryStatus == DeliveryStatus.ONLINE_ERROR) {
            withdrawal.cancel(withdrawalId, shift)
            logger.warn("Z-report of shift {} rejected by the OFD, the shift stays open", shift.shiftNo)
        } else {
            withdrawal.confirm(withdrawalId, shift, autonomous)
            storage.closeShift(shift.id, ShiftStatus.CLOSED, now, documentId)
            logger.info("Shift {} closed, Z-report {}", shift.shiftNo, result.deliveryStatus)
        }
        return result
    }

    /**
     * Отправляет Z-отчёт и записывает исход.
     *
     * @return результат и признак автономного закрытия.
     */
    private fun deliver(kkmId: String, documentId: String, now: Long): Pair<ReportResult, Boolean> {
        if (!queue.canSendDirectly(kkmId)) {
            enqueue(kkmId, documentId)
            storage.updateReceiptStatus(
                documentId,
                null,
                now.toString(),
                PENDING,
                deliveredAt = null,
                isAutonomous = true
            )
            return ReportResult(documentId, DeliveryStatus.OFFLINE_QUEUED) to true
        }
        val result = sendFiscalCommandUseCase.execute(kkmId, OfdCommandType.CLOSE_SHIFT, documentId)
        val accepted = result.status == OfdCommandStatus.OK
        val timeout = result.status == OfdCommandStatus.TIMEOUT
        storage.updateReceiptStatus(
            documentId = documentId,
            fiscalSign = result.fiscalSign,
            autonomousSign = result.autonomousSign,
            ofdStatus = statusOf(result.status),
            ofdErrorCode = result.resultCode?.takeIf { !accepted && !timeout },
            deliveredAt = if (accepted) now else null,
            isAutonomous = timeout,
            ofdErrorText = result.resultText?.takeIf { !accepted && !timeout && it.isNotBlank() }
        )
        if (timeout) enqueue(kkmId, documentId)
        return ReportResult(documentId, deliveryOf(result.status), result.errorMessage) to timeout
    }

    private fun enqueue(kkmId: String, documentId: String) {
        queue.enqueueOffline(OfflineQueueCommandRequest(kkmId, OfdCommandType.CLOSE_SHIFT.value, documentId))
    }

    private fun lockKkm(kkmId: String): KkmInfo {
        val kkm = storage.findKkmForUpdate(kkmId)
            ?: throw ValidationException(CoreStrings.kkmNotFound(), "KKM_NOT_FOUND")
        if (kkm.state == KkmState.PROGRAMMING.name) {
            throw ValidationException(CoreStrings.kkmInProgramming(), "KKM_IN_PROGRAMMING")
        }
        return kkm
    }

    private fun requireOpenShift(kkmId: String): ShiftInfo =
        storage.findOpenShift(kkmId) ?: throw ConflictException(CoreStrings.shiftNotOpen(), "SHIFT_NOT_OPEN")

    private fun statusOf(status: OfdCommandStatus): String = when (status) {
        OfdCommandStatus.OK -> "SENT"
        OfdCommandStatus.TIMEOUT -> PENDING
        OfdCommandStatus.FAILED -> "FAILED"
    }

    private fun deliveryOf(status: OfdCommandStatus): DeliveryStatus = when (status) {
        OfdCommandStatus.OK -> DeliveryStatus.ONLINE_OK
        OfdCommandStatus.TIMEOUT -> DeliveryStatus.OFFLINE_QUEUED
        OfdCommandStatus.FAILED -> DeliveryStatus.ONLINE_ERROR
    }

    private companion object {
        const val PENDING = "PENDING"
    }
}
