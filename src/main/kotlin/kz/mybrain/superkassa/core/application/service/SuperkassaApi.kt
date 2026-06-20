package kz.mybrain.superkassa.core.application.service

import kz.mybrain.superkassa.core.application.model.FactoryNumberResponse
import kz.mybrain.superkassa.core.application.model.KkmInitDirectRequest
import kz.mybrain.superkassa.core.application.model.KkmInitSimpleRequest
import kz.mybrain.superkassa.core.application.model.KkmListParams
import kz.mybrain.superkassa.core.application.model.KkmListResult
import kz.mybrain.superkassa.core.application.model.OfdAuthInfoResponse
import kz.mybrain.superkassa.core.application.model.PrintDocumentType
import kz.mybrain.superkassa.core.application.model.UserCreateRequest
import kz.mybrain.superkassa.core.application.model.UserResponse
import kz.mybrain.superkassa.core.application.model.UserUpdateRequest
import kz.mybrain.superkassa.core.application.model.receipt.*
import kz.mybrain.superkassa.core.domain.model.*
import kz.mybrain.superkassa.core.domain.model.OfdCommandResult

interface SuperkassaApi {
    fun initKkm(pin: String, request: KkmInitDirectRequest): KkmInfo
    fun initKkmSimple(pin: String, request: KkmInitSimpleRequest): KkmInfo
    fun generateFactoryInfo(): FactoryNumberResponse
    fun getKkm(id: String): KkmInfo
    fun listKkms(params: KkmListParams): KkmListResult
    fun deleteKkm(id: String, pin: String): Boolean
    fun listCounters(kkmId: String, pin: String): List<CounterSnapshot>
    fun updateKkmSettings(kkmId: String, pin: String, autoCloseShift: Boolean): KkmInfo
    fun updateTaxSettings(kkmId: String, pin: String, taxRegime: TaxRegime, defaultVatGroup: VatGroup): KkmInfo
    fun enterProgramming(kkmId: String, pin: String): KkmInfo
    fun exitProgramming(kkmId: String, pin: String): KkmInfo

    fun listUsers(kkmId: String, pin: String): List<UserResponse>
    fun createUser(kkmId: String, pin: String, request: UserCreateRequest): UserResponse
    fun updateUser(kkmId: String, userId: String, pin: String, request: UserUpdateRequest): UserResponse
    fun deleteUser(kkmId: String, userId: String, pin: String): Boolean

    fun getOfdAuthInfo(kkmId: String, pin: String): OfdAuthInfoResponse
    fun updateOfdToken(kkmId: String, pin: String, token: String): Boolean
    fun checkOfdConnection(kkmId: String): OfdCommandResult
    fun getOfdInfo(kkmId: String): OfdCommandResult
    fun syncOfdServiceInfo(kkmId: String, pin: String): OfdCommandResult
    fun syncOfdCounters(kkmId: String, pin: String): OfdCommandResult

    fun getReceiptHtml(kkmId: String, documentId: String, pin: String): String
    fun getPrintHtml(kkmId: String, type: PrintDocumentType, documentId: String?, shiftId: String?, pin: String): String
    fun getPrintPdf(kkmId: String, type: PrintDocumentType, documentId: String?, shiftId: String?, pin: String): ByteArray

    fun createReceipt(request: ReceiptRequest): ReceiptResult
    fun createSellReceipt(kkmId: String, pin: String, request: ReceiptSellRequest): ReceiptResult
    fun createSellReturnReceipt(kkmId: String, pin: String, request: ReceiptSellReturnRequest): ReceiptResult
    fun createBuyReceipt(kkmId: String, pin: String, request: ReceiptBuyRequest): ReceiptResult
    fun createBuyReturnReceipt(kkmId: String, pin: String, request: ReceiptBuyReturnRequest): ReceiptResult

    fun cashIn(kkmId: String, pin: String, request: CashOperationRequest): CashOperationResult
    fun cashOut(kkmId: String, pin: String, request: CashOperationRequest): CashOperationResult
    fun retryReceiptDelivery(kkmId: String, documentId: String, pin: String): List<Pair<String, Boolean>>

    fun openShift(kkmId: String, pin: String): ShiftInfo
    fun closeShift(kkmId: String, pin: String): ReportResult
    fun getOpenShift(kkmId: String, pin: String): ShiftInfo
    fun listShifts(kkmId: String, limit: Int, offset: Int, pin: String): List<ShiftInfo>
    fun listShiftDocuments(kkmId: String, shiftId: String, limit: Int, offset: Int, pin: String): List<FiscalDocumentSnapshot>
    fun listFiscalDocumentsByPeriod(kkmId: String, fromInclusive: Long, toExclusive: Long, limit: Int, offset: Int, pin: String): List<FiscalDocumentSnapshot>
    fun createReport(kkmId: String, pin: String): ReportResult
}
