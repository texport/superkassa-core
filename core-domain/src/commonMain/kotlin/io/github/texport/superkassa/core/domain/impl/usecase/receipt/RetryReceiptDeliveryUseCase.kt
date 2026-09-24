package io.github.texport.superkassa.core.domain.impl.usecase.receipt

import io.github.texport.superkassa.core.domain.api.exception.ConflictException
import io.github.texport.superkassa.core.domain.api.exception.NotFoundException
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryTask
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryTaskStatus
import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.helper.ReceiptDeliveryHelper
import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.delivery.ReceiptDeliveryPlan
import io.github.texport.superkassa.core.domain.impl.usecase.delivery.SendDeliveryTasksUseCase
import io.github.texport.superkassa.core.string.api.CoreStrings

/**
 * Повтор доставки чека покупателю по требованию кассира.
 *
 * Идёт через те же задачи, что и фон: недостающие по нынешним настройкам
 * задачи ставятся, окончательно не удавшиеся получают попытки заново,
 * и задачи документа отправляются сразу. Доставленное второй раз не
 * уходит. Хранилище без задач — повтор сразу по каналам, как прежде.
 *
 * @property sender отправка задач документа.
 * @property plan какие задачи ставит чек по нынешним настройкам.
 */
class RetryReceiptDeliveryUseCase(
    private val storage: StoragePort,
    private val authorizeUserUseCase: AuthorizeUserUseCase,
    private val helper: ReceiptDeliveryHelper,
    private val plan: ReceiptDeliveryPlan,
    private val sender: SendDeliveryTasksUseCase,
    private val clock: ClockPort
) {
    /**
     * Повторяет доставку и отвечает, дошёл ли чек по каждому каналу.
     *
     * @return пары «канал — доставлен ли».
     * @throws NotFoundException если документ не найден или принадлежит другой ККМ.
     * @throws ConflictException если документ не фискализирован либо ни один канал доставки не настроен.
     */
    fun execute(kkmId: String, documentId: String, pin: String): List<Pair<String, Boolean>> {
        val (snapshot, receipt) = fiscalDocument(kkmId, documentId, pin)
        val tasks = runCatching { resendTasks(snapshot) }
        if (tasks.exceptionOrNull() is UnsupportedOperationException) {
            return helper.retryDelivery(kkmId, documentId, receipt, snapshot).ifEmpty { throw notConfigured() }
        }
        return tasks.getOrThrow().map { it.channel to (it.status == DeliveryTaskStatus.DELIVERED) }
    }

    /**
     * Повторяет доставку и отвечает задачами документа — с причинами отказов.
     *
     * @throws NotFoundException если документ не найден или принадлежит другой ККМ.
     * @throws ConflictException если документ не фискализирован либо ни один канал доставки не настроен.
     * @throws UnsupportedOperationException если хранилище задач не держит.
     */
    fun resend(kkmId: String, documentId: String, pin: String): List<DeliveryTask> =
        resendTasks(fiscalDocument(kkmId, documentId, pin).first)

    private fun resendTasks(snapshot: FiscalDocumentSnapshot): List<DeliveryTask> {
        val now = clock.now()
        storage.addDeliveryTasks(plan.tasksFor(snapshot.cashboxId, snapshot.id, snapshot.receiptUrl != null, now))
        storage.deliveryTasksOf(snapshot.id)
            .filter { it.status == DeliveryTaskStatus.FAILED }
            .forEach {
                storage.saveDeliveryTask(
                    it.copy(status = DeliveryTaskStatus.PENDING, attempts = 0, nextAttemptAt = now, updatedAt = now)
                )
            }
        return sender.sendDocument(snapshot.id).ifEmpty { throw notConfigured() }
    }

    private fun fiscalDocument(kkmId: String, documentId: String, pin: String): Pair<FiscalDocumentSnapshot, ReceiptRequest> {
        authorizeUserUseCase.requireKkm(kkmId)
        authorizeUserUseCase.requireRole(kkmId, pin, setOf(UserRole.CASHIER, UserRole.ADMIN))
        val (snapshot, receipt) = storage.findFiscalDocumentWithReceiptPayload(documentId)
            ?: throw NotFoundException(CoreStrings.documentNotFound(), "DOCUMENT_NOT_FOUND")
        if (snapshot.cashboxId != kkmId) {
            throw NotFoundException(CoreStrings.documentNotFound(), "DOCUMENT_NOT_FOUND")
        }
        // Документ, который так и не стал фискальным, отправлять нечем:
        // ни фискального признака, ни автономного у него нет. Ответить
        // успехом означало бы сообщить кассиру о доставке чека, которого
        // не существует.
        if (snapshot.fiscalSign == null && snapshot.autonomousSign == null) {
            throw ConflictException(CoreStrings.documentNotFiscalized(), "DOCUMENT_NOT_FISCALIZED")
        }
        return snapshot to receipt
    }

    // Пустой список означает, что ни один канал даже не пробовали.
    // Прежде это отдавалось как успех повтора.
    private fun notConfigured() = ConflictException(
        CoreStrings.deliveryChannelsNotConfigured(),
        "DELIVERY_NOT_CONFIGURED"
    )
}
