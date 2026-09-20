package io.github.texport.superkassa.core.domain.impl.usecase.receipt

import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.domain.api.exception.ConflictException
import io.github.texport.superkassa.core.domain.api.exception.NotFoundException
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase
import io.github.texport.superkassa.core.domain.impl.helper.ReceiptDeliveryHelper

/**
 * Сценарий повторной доставки чека покупателю по различным каналам связи.
 *
 * Позволяет повторно отправить фискальный чек (например, если первичная отправка
 * по электронной почте или SMS завершилась ошибкой). Перед выполнением проверяет права доступа.
 *
 * @property storage Порт для доступа к персистентному хранилищу данных.
 * @property authorizeUserUseCase Сценарий авторизации и проверки прав доступа пользователя.
 * @property helper Вспомогательный компонент для управления отправкой чека.
 */
class RetryReceiptDeliveryUseCase(
    private val storage: StoragePort,
    private val authorizeUserUseCase: AuthorizeUserUseCase,
    private val helper: ReceiptDeliveryHelper
) {
    /**
     * Выполняет повторную отправку фискального чека покупателю.
     *
     * @param kkmId Идентификатор кассового аппарата (ККМ).
     * @param documentId Идентификатор повторно отправляемого документа.
     * @param pin PIN-код кассира для верификации прав доступа.
     * @return Список пар, где первый элемент — имя канала доставки (например, "EMAIL", "SMS"),
     *         а второй — статус успеха отправки (true, если отправлено успешно).
     * @throws NotFoundException если документ с указанным идентификатором не найден или принадлежит другой ККМ.
     * @throws ConflictException если документ не был фискализирован либо ни один канал доставки не настроен.
     */
    fun execute(kkmId: String, documentId: String, pin: String): List<Pair<String, Boolean>> {
        // Проверка существования ККМ и авторизация пользователя со считыванием роли
        authorizeUserUseCase.requireKkm(kkmId)
        authorizeUserUseCase.requireRole(kkmId, pin, setOf(UserRole.CASHIER, UserRole.ADMIN))

        // Загрузка фискального документа и его исходного запроса с полезной нагрузкой чека
        val (snapshot, receipt) = storage.findFiscalDocumentWithReceiptPayload(documentId)
            ?: throw NotFoundException(CoreStrings.documentNotFound(), "DOCUMENT_NOT_FOUND")

        // Проверка соответствия ККМ в документе и текущего активного кассового аппарата
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

        // Повторная отправка чека по доступным каналам доставки
        val results = helper.retryDelivery(kkmId, documentId, receipt, snapshot)

        // Пустой список означает, что ни один канал даже не пробовали.
        // Прежде это отдавалось как успех повтора.
        if (results.isEmpty()) {
            throw ConflictException(CoreStrings.deliveryChannelsNotConfigured(), "DELIVERY_NOT_CONFIGURED")
        }
        return results
    }
}
