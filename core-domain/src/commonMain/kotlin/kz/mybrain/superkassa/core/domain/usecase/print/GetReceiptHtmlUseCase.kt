package kz.mybrain.superkassa.core.domain.usecase.print

import kz.mybrain.superkassa.core.domain.exception.ErrorMessages
import kz.mybrain.superkassa.core.domain.exception.NotFoundException
import kz.mybrain.superkassa.core.domain.model.auth.UserRole
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptLayoutType
import kz.mybrain.superkassa.core.domain.port.ReceiptRenderPort
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.core.domain.usecase.auth.AuthorizeUserUseCase

/**
 * Сценарий (Use Case) получения печатной формы чека в HTML.
 *
 * Отвечает за загрузку данных фискального документа и его чековой нагрузки (payload) из хранилища,
 * проверку принадлежности документа к указанной ККМ и рендеринг в HTML с использованием заданного макета.
 *
 * @property storage Порт доступа к локальному хранилищу документов.
 * @property receiptRenderPort Порт рендеринга печатных форм чеков.
 * @property authorizeUserUseCase Сценарий авторизации оператора ККМ.
 */
class GetReceiptHtmlUseCase(
    private val storage: StoragePort,
    private val receiptRenderPort: ReceiptRenderPort,
    private val authorizeUserUseCase: AuthorizeUserUseCase
) {
    /**
     * Выполняет сценарий генерации HTML-представления чека.
     *
     * Требует авторизации с ролью [UserRole.CASHIER] или [UserRole.ADMIN].
     *
     * @param kkmId Идентификатор кассового аппарата (ККМ).
     * @param documentId Идентификатор фискального чека.
     * @param pin ПИН-код оператора для проверки прав.
     * @param layout Шаблон/макет чека (опционально).
     * @return HTML-строка печатного чека.
     * @throws NotFoundException Если документ не найден или принадлежит другой кассе.
     */
    fun execute(kkmId: String, documentId: String, pin: String, layout: ReceiptLayoutType? = null): String {
        val kkm = authorizeUserUseCase.requireKkm(kkmId)
        authorizeUserUseCase.requireRole(kkmId, pin, setOf(UserRole.CASHIER, UserRole.ADMIN))
        val (snapshot, receipt) = storage.findFiscalDocumentWithReceiptPayload(documentId)
            ?: throw NotFoundException(ErrorMessages.documentNotFound(), "DOCUMENT_NOT_FOUND")
        if (snapshot.cashboxId != kkmId) {
            throw NotFoundException(ErrorMessages.documentNotFound(), "DOCUMENT_NOT_FOUND")
        }
        return receiptRenderPort.renderHtml(receipt, snapshot, kkm, layout)
    }
}
