package io.github.texport.superkassa.core.domain.impl.usecase.delivery

import io.github.texport.superkassa.core.domain.api.exception.NotFoundException
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryTask
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase
import io.github.texport.superkassa.core.string.api.CoreStrings

/**
 * Доставка чека по каналам, как её видит журнал: что ждёт, что дошло,
 * что не удалось и почему.
 */
class GetReceiptDeliveriesUseCase(
    private val storage: StoragePort,
    private val authorizeUser: AuthorizeUserUseCase
) {
    /**
     * Задачи доставки документа [documentId] кассы [kkmId] под пином кассира или администратора.
     *
     * @return задачи по каналам; пусто — доставка чека не заказывалась.
     * @throws NotFoundException если документа нет или он другой кассы.
     * @throws UnsupportedOperationException если хранилище задач не держит.
     */
    fun execute(kkmId: String, documentId: String, pin: String): List<DeliveryTask> {
        authorizeUser.requireKkm(kkmId)
        authorizeUser.requireRole(kkmId, pin, setOf(UserRole.CASHIER, UserRole.ADMIN))
        val document = storage.findFiscalDocumentById(documentId)
        if (document?.cashboxId != kkmId) {
            throw NotFoundException(CoreStrings.documentNotFound(), "DOCUMENT_NOT_FOUND")
        }
        return storage.deliveryTasksOf(documentId)
    }
}
