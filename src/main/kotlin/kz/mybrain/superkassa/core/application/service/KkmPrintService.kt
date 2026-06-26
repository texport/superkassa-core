package kz.mybrain.superkassa.core.application.service

import kz.mybrain.superkassa.core.application.error.ConflictException
import kz.mybrain.superkassa.core.application.error.ErrorMessages
import kz.mybrain.superkassa.core.application.error.NotFoundException
import kz.mybrain.superkassa.core.application.error.ValidationException
import kz.mybrain.superkassa.core.application.model.PrintDocumentType
import kz.mybrain.superkassa.core.application.policy.CounterScopes
import kz.mybrain.superkassa.core.domain.model.ShiftInfo
import kz.mybrain.superkassa.core.domain.model.UserRole
import kz.mybrain.superkassa.core.domain.port.ClockPort
import kz.mybrain.superkassa.core.domain.port.DocumentConvertPort
import kz.mybrain.superkassa.core.domain.port.ReceiptRenderPort
import kz.mybrain.superkassa.core.domain.port.StoragePort

import kz.mybrain.superkassa.core.domain.model.ReceiptLayoutType

/**
 * Сервис для формирования печатных форм документов (HTML, PDF).
 */
class KkmPrintService(
    private val storage: StoragePort,
    private val receiptRenderPort: ReceiptRenderPort,
    private val documentConvertPort: DocumentConvertPort,
    private val authorization: AuthorizationService,
    clock: ClockPort,
    private val kkmCommonHelper: KkmCommonHelper
) {

    fun getReceiptHtml(kkmId: String, documentId: String, pin: String, layout: ReceiptLayoutType? = null): String {
        val kkm = authorization.requireKkm(kkmId)
        authorization.requireRole(kkmId, pin, setOf(UserRole.CASHIER, UserRole.ADMIN))
        val (snapshot, receipt) = storage.findFiscalDocumentWithReceiptPayload(documentId)
            ?: throw NotFoundException(ErrorMessages.documentNotFound(), "DOCUMENT_NOT_FOUND")
        if (snapshot.cashboxId != kkmId) {
            throw NotFoundException(ErrorMessages.documentNotFound(), "DOCUMENT_NOT_FOUND")
        }
        return receiptRenderPort.renderHtml(receipt, snapshot, kkm, layout)
    }

    fun getPrintHtml(
        kkmId: String,
        type: PrintDocumentType,
        documentId: String?,
        shiftId: String?,
        pin: String,
        layout: ReceiptLayoutType? = null
    ): String {
        val kkm = authorization.requireKkm(kkmId)
        authorization.requireRole(kkmId, pin, setOf(UserRole.CASHIER, UserRole.ADMIN))
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

    fun getPrintPdf(
        kkmId: String,
        type: PrintDocumentType,
        documentId: String?,
        shiftId: String?,
        pin: String,
        layout: ReceiptLayoutType? = null
    ): ByteArray {
        val html = getPrintHtml(kkmId, type, documentId, shiftId, pin, layout)
        return documentConvertPort.htmlToPdf(html)
    }

    private fun getDocumentPrintHtmlInternal(
        kkm: kz.mybrain.superkassa.core.domain.model.KkmInfo,
        documentId: String,
        pin: String,
        layout: ReceiptLayoutType? = null
    ): String {
        val doc = storage.findFiscalDocumentById(documentId)
            ?: throw NotFoundException(ErrorMessages.documentNotFound(), "DOCUMENT_NOT_FOUND")
        if (doc.cashboxId != kkm.id) throw NotFoundException(ErrorMessages.documentNotFound(), "DOCUMENT_NOT_FOUND")
        return when (doc.docType) {
            "CHECK" -> getReceiptHtml(kkm.id, documentId, pin, layout)
            "CASH_IN", "CASH_OUT" -> receiptRenderPort.renderCashOperationHtml(doc, kkm, layout)
            else -> throw NotFoundException(ErrorMessages.documentNotFound(), "DOCUMENT_NOT_FOUND")
        }
    }

    private fun getOpenShift(kkmId: String, pin: String): ShiftInfo {
        kkmCommonHelper.ensureSystemTimeValid()
        val kkm = authorization.requireKkm(kkmId)
        if (kkm.state == kz.mybrain.superkassa.core.domain.model.KkmState.BLOCKED.name) {
            throw ValidationException(ErrorMessages.kkmBlocked(), "KKM_BLOCKED")
        }
        if (kkm.state == kz.mybrain.superkassa.core.domain.model.KkmState.PROGRAMMING.name) {
            throw ValidationException(ErrorMessages.kkmInProgramming(), "KKM_IN_PROGRAMMING")
        }
        authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN, UserRole.CASHIER))
        return storage.findOpenShift(kkmId)
            ?: throw ConflictException(ErrorMessages.shiftNotOpen(), "SHIFT_NOT_OPEN")
    }

    private fun resolveOfdStatus(kkmId: String, documentId: String?): String {
        val docId = documentId ?: return "DELIVERED"
        val queueTasks = storage.listQueueTasksByCashbox(kkmId, "OFFLINE", 100)
        val task = queueTasks.firstOrNull { it.payloadRef == docId } ?: return "DELIVERED"
        return if (task.status == "SENT") "DELIVERED" else "OFFLINE"
    }
}
