package kz.mybrain.superkassa.core.domain.usecase.receipt

import kz.mybrain.superkassa.core.domain.exception.ErrorMessages
import kz.mybrain.superkassa.core.domain.exception.NotFoundException
import kz.mybrain.superkassa.core.domain.model.auth.UserRole
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.core.domain.usecase.auth.AuthorizeUserUseCase
import kz.mybrain.superkassa.core.domain.helper.ReceiptDeliveryHelper

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
@Suppress("unused", "DuplicatedCode")
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
     */
    fun execute(kkmId: String, documentId: String, pin: String): List<Pair<String, Boolean>> {
        // Проверка существования ККМ и авторизация пользователя со считыванием роли
        authorizeUserUseCase.requireKkm(kkmId)
        authorizeUserUseCase.requireRole(kkmId, pin, setOf(UserRole.CASHIER, UserRole.ADMIN))

        // Загрузка фискального документа и его исходного запроса с полезной нагрузкой чека
        val (snapshot, receipt) = storage.findFiscalDocumentWithReceiptPayload(documentId)
            ?: throw NotFoundException(ErrorMessages.documentNotFound(), "DOCUMENT_NOT_FOUND")

        // Проверка соответствия ККМ в документе и текущего активного кассового аппарата
        if (snapshot.cashboxId != kkmId) {
            throw NotFoundException(ErrorMessages.documentNotFound(), "DOCUMENT_NOT_FOUND")
        }

        // Повторная отправка чека по доступным каналам доставки
        return helper.retryDelivery(kkmId, documentId, receipt, snapshot)
    }
}
