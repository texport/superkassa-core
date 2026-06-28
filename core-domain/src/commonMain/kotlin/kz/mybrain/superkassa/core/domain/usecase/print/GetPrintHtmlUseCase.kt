package kz.mybrain.superkassa.core.domain.usecase.print

import kz.mybrain.superkassa.core.domain.exception.ConflictException
import kz.mybrain.superkassa.core.domain.exception.ErrorMessages
import kz.mybrain.superkassa.core.domain.exception.NotFoundException
import kz.mybrain.superkassa.core.domain.exception.ValidationException
import kz.mybrain.superkassa.core.domain.model.auth.UserRole
import kz.mybrain.superkassa.core.domain.model.common.CounterScopes
import kz.mybrain.superkassa.core.domain.model.kkm.KkmInfo
import kz.mybrain.superkassa.core.domain.model.kkm.KkmState
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptLayoutType
import kz.mybrain.superkassa.core.domain.model.report.PrintDocumentType
import kz.mybrain.superkassa.core.domain.model.shift.ShiftInfo
import kz.mybrain.superkassa.core.domain.port.ReceiptRenderPort
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.core.domain.usecase.auth.AuthorizeUserUseCase
import kz.mybrain.superkassa.core.domain.helper.KkmCommonHelper

/**
 * Сценарий (Use Case) получения печатной формы документа в формате HTML.
 *
 * Поддерживает генерацию печатных форм для различных типов документов ККМ:
 * - Фискальные чеки (продажа, покупка, возвраты).
 * - Нефискальные документы операций с наличными (внесение, изъятие).
 * - Сменные отчеты (X-отчет, отчет о закрытии смены / Z-отчет).
 * - Документы об открытии смены.
 *
 * @property storage Порт доступа к локальному хранилищу данных ККМ, смен и документов.
 * @property receiptRenderPort Порт рендеринга печатных форм документов.
 * @property authorizeUserUseCase Сценарий авторизации оператора ККМ.
 * @property kkmCommonHelper Вспомогательный класс для общих проверок кассы (например, времени).
 * @property getReceiptHtml Сценарий получения HTML-представления фискального чека.
 */
class GetPrintHtmlUseCase(
    private val storage: StoragePort,
    private val receiptRenderPort: ReceiptRenderPort,
    private val authorizeUserUseCase: AuthorizeUserUseCase,
    private val kkmCommonHelper: KkmCommonHelper,
    private val getReceiptHtml: GetReceiptHtmlUseCase
) {
    /**
     * Выполняет сценарий генерации печатной формы документа в формате HTML.
     *
     * Требует авторизации с ролью [UserRole.CASHIER] или [UserRole.ADMIN].
     *
     * @param kkmId Идентификатор кассового аппарата (ККМ).
     * @param type Тип запрашиваемого печатного документа [PrintDocumentType].
     * @param documentId Идентификатор документа (обязателен для типа [PrintDocumentType.DOCUMENT]).
     * @param shiftId Идентификатор смены (обязателен для типа [PrintDocumentType.CLOSE_SHIFT]).
     * @param pin ПИН-код оператора для проверки прав.
     * @param layout Шаблон/макет чека (опционально).
     * @return HTML-строка печатного документа.
     * @throws ValidationException Если не переданы обязательные идентификаторы документов/смен, либо касса заблокирована.
     * @throws NotFoundException Если документ или смена не найдены в БД.
     * @throws ConflictException Если запрашивается X-отчет или отчет открытия смены, но смена не открыта.
     */
    fun execute(
        kkmId: String,
        type: PrintDocumentType,
        documentId: String?,
        shiftId: String?,
        pin: String,
        layout: ReceiptLayoutType? = null
    ): String {
        val kkm = authorizeUserUseCase.requireKkm(kkmId)
        authorizeUserUseCase.requireRole(kkmId, pin, setOf(UserRole.CASHIER, UserRole.ADMIN))
        return when (type) {
            PrintDocumentType.DOCUMENT -> {
                val id = documentId ?: throw ValidationException(ErrorMessages.badRequest(), "DOCUMENT_ID_REQUIRED")
                getDocumentPrintHtmlInternal(kkm, id, pin, layout)
            }
            PrintDocumentType.X_REPORT -> {
                val shift = getOpenShift(kkmId, pin)
                val counters = storage.loadCounters(kkmId, CounterScopes.SHIFT, shift.id)
                receiptRenderPort.renderXReportHtml(shift, counters, kkm, null, layout)
            }
            PrintDocumentType.OPEN_SHIFT -> {
                val shift = getOpenShift(kkmId, pin)
                val ofdStatus = resolveOfdStatus(kkmId, shift.openDocumentId)
                val docNo = shift.openDocumentId?.let { storage.findFiscalDocumentById(it)?.docNo?.toString() }
                receiptRenderPort.renderOpenShiftHtml(shift, kkm, ofdStatus, docNo, layout)
            }
            PrintDocumentType.CLOSE_SHIFT -> {
                val sid = shiftId ?: throw ValidationException(ErrorMessages.badRequest(), "SHIFT_ID_REQUIRED")
                val shift = storage.findShiftById(sid)
                    ?: throw NotFoundException(ErrorMessages.documentNotFound(), "SHIFT_NOT_FOUND")
                if (shift.kkmId != kkmId) throw NotFoundException(ErrorMessages.documentNotFound(), "SHIFT_NOT_FOUND")
                val counters = storage.loadCounters(kkmId, CounterScopes.SHIFT, sid)
                val ofdStatus = resolveOfdStatus(kkmId, shift.closeDocumentId)
                val docNo = shift.closeDocumentId?.let { storage.findFiscalDocumentById(it)?.docNo?.toString() }
                receiptRenderPort.renderCloseShiftHtml(shift, counters, kkm, ofdStatus, docNo, layout)
            }
        }
    }

    /**
     * Внутренний метод генерации HTML-представления для конкретного документа (чека или кассовой операции).
     */
    private fun getDocumentPrintHtmlInternal(
        kkm: KkmInfo,
        documentId: String,
        pin: String,
        layout: ReceiptLayoutType? = null
    ): String {
        val doc = storage.findFiscalDocumentById(documentId)
            ?: throw NotFoundException(ErrorMessages.documentNotFound(), "DOCUMENT_NOT_FOUND")
        if (doc.cashboxId != kkm.id) throw NotFoundException(ErrorMessages.documentNotFound(), "DOCUMENT_NOT_FOUND")
        return when (doc.docType) {
            "CHECK" -> getReceiptHtml.execute(kkm.id, documentId, pin, layout)
            "CASH_IN", "CASH_OUT" -> receiptRenderPort.renderCashOperationHtml(doc, kkm, layout)
            else -> throw NotFoundException(ErrorMessages.documentNotFound(), "DOCUMENT_NOT_FOUND")
        }
    }

    /**
     * Получает информацию об открытой смене с валидацией состояния ККМ.
     */
    private fun getOpenShift(kkmId: String, pin: String): ShiftInfo {
        kkmCommonHelper.ensureSystemTimeValid()
        val kkm = authorizeUserUseCase.requireKkm(kkmId)
        if (kkm.state == KkmState.BLOCKED.name) {
            throw ValidationException(ErrorMessages.kkmBlocked(), "KKM_BLOCKED")
        }
        if (kkm.state == KkmState.PROGRAMMING.name) {
            throw ValidationException(ErrorMessages.kkmInProgramming(), "KKM_IN_PROGRAMMING")
        }
        authorizeUserUseCase.requireRole(kkmId, pin, setOf(UserRole.ADMIN, UserRole.CASHIER))
        return storage.findOpenShift(kkmId)
            ?: throw ConflictException(ErrorMessages.shiftNotOpen(), "SHIFT_NOT_OPEN")
    }

    /**
     * Определяет статус отправки документа в ОФД ("DELIVERED" или "OFFLINE") на основе задач в очереди.
     */
    private fun resolveOfdStatus(kkmId: String, documentId: String?): String {
        val docId = documentId ?: return "DELIVERED"
        val queueTasks = storage.listQueueTasksByCashbox(kkmId, "OFFLINE", 100)
        val task = queueTasks.firstOrNull { it.payloadRef == docId } ?: return "DELIVERED"
        return if (task.status == "SENT") "DELIVERED" else "OFFLINE"
    }
}
