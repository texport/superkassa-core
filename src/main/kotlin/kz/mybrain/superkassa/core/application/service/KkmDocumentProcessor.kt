package kz.mybrain.superkassa.core.application.service

import kz.mybrain.superkassa.core.application.error.ConflictException
import kz.mybrain.superkassa.core.application.error.ErrorMessages
import kz.mybrain.superkassa.core.application.error.NotFoundException
import kz.mybrain.superkassa.core.application.error.ValidationException
import kz.mybrain.superkassa.core.application.policy.CounterKeyFormats
import kz.mybrain.superkassa.core.application.policy.CounterScopes
import kz.mybrain.superkassa.core.domain.model.CashOperationRequest
import kz.mybrain.superkassa.core.domain.model.CashOperationResult
import kz.mybrain.superkassa.core.domain.model.CashOperationType
import kz.mybrain.superkassa.core.domain.model.KkmInfo
import kz.mybrain.superkassa.core.domain.model.KkmState
import kz.mybrain.superkassa.core.domain.model.Money
import kz.mybrain.superkassa.core.domain.model.OfdCommandResult
import kz.mybrain.superkassa.core.domain.model.OfdCommandStatus
import kz.mybrain.superkassa.core.domain.model.OfdCommandType
import kz.mybrain.superkassa.core.domain.model.OfflineQueueCommandRequest
import kz.mybrain.superkassa.core.domain.model.ReceiptRequest
import kz.mybrain.superkassa.core.domain.model.ReceiptResult
import kz.mybrain.superkassa.core.domain.model.UserRole
import kz.mybrain.superkassa.core.domain.port.ClockPort
import kz.mybrain.superkassa.core.domain.port.CounterUpdaterPort
import kz.mybrain.superkassa.core.domain.port.OfflineQueuePort
import kz.mybrain.superkassa.core.domain.port.StoragePort

/**
 * Процессор фискальных документов (чеки, внесения, изъятия).
 * Использует ReceiptDeliveryHelper для отправки и печати чеков.
 */
class KkmDocumentProcessor(
    private val storage: StoragePort,
    private val queue: OfflineQueuePort,
    private val fiscalOperationExecutor: FiscalOperationExecutor,
    private val counters: CounterUpdaterPort,
    private val clock: ClockPort,
    private val authorization: AuthorizationService,
    private val kkmCommonHelper: KkmCommonHelper,
    private val autonomousModeService: AutonomousModeService,
    private val shiftService: ShiftService,
    private val receiptDeliveryHelper: ReceiptDeliveryHelper
) {

    fun createReceipt(request: ReceiptRequest): ReceiptResult {
        val kkm = requireKkm(request.kkmId)
        val requestWithTaxSettings = request.copy(
            taxRegime = kkm.taxRegime,
            defaultVatGroup = request.defaultVatGroup ?: kkm.defaultVatGroup
        )
        return fiscalOperationExecutor.executeIdempotentFiscalOperation(
            kkmId = requestWithTaxSettings.kkmId,
            pin = requestWithTaxSettings.pin,
            idempotencyKey = requestWithTaxSettings.idempotencyKey,
            operationType = "CREATE_RECEIPT",
            checkShift = {
                val shift = storage.findOpenShift(requestWithTaxSettings.kkmId)
                    ?: throw ConflictException(ErrorMessages.shiftNotOpen(), "SHIFT_NOT_OPEN")
                shift.id
            },
            saveOperation = { documentId, now, shiftId ->
                storage.saveReceipt(requestWithTaxSettings, documentId, shiftId, now)
            },
            sendOfdCommand = { currentKkm, documentId ->
                val hasQueue = !queue.canSendDirectly(requestWithTaxSettings.kkmId)
                val command = OfflineQueueCommandRequest(
                    kkmId = requestWithTaxSettings.kkmId,
                    type = OfdCommandType.TICKET.value,
                    payloadRef = documentId
                )
                if (hasQueue) {
                    queue.enqueueOffline(command)
                    ofdResultQueuedOffline()
                } else {
                    kkmCommonHelper.sendOfdCommand(
                        kkm = currentKkm,
                        commandType = OfdCommandType.TICKET,
                        payloadRef = documentId
                    )
                }
            },
            processResult = { currentKkm, documentId, currentKkmId, ofdResult, commandType, now, receiptContext ->
                processOfdDocumentResult(
                    kkm = currentKkm,
                    documentId = documentId,
                    kkmId = currentKkmId,
                    ofdResult = ofdResult,
                    commandType = commandType,
                    now = now,
                    receiptContext = receiptContext
                )
                if (commandType == OfdCommandType.TICKET && receiptContext != null && ofdResult.resultCode == 0) {
                    val (receipt, _) = receiptContext
                    val doc = storage.findFiscalDocumentById(documentId)
                    if (doc != null) {
                        receiptDeliveryHelper.deliverReceipt(
                            kkmId = request.kkmId,
                            documentId = documentId,
                            receipt = receipt,
                            docSnapshot = doc,
                            responseJson = ofdResult.responseJson,
                            responseBin = ofdResult.responseBin
                        )
                    }
                }
            },
            buildResult = { documentId, ofdResult, deliveryStatus ->
                val doc = storage.findFiscalDocumentById(documentId)
                ReceiptResult(
                    documentId = documentId,
                    fiscalSign = doc?.fiscalSign ?: ofdResult.fiscalSign,
                    autonomousSign = doc?.autonomousSign ?: ofdResult.autonomousSign,
                    deliveryPayload = ofdResult.responseBin,
                    deliveryStatus = deliveryStatus,
                    deliveryError = ofdResult.errorMessage
                )
            },
            receiptContextProvider = { shiftId -> Pair(requestWithTaxSettings, shiftId) }
        )
    }

    fun cashIn(kkmId: String, pin: String, request: CashOperationRequest): CashOperationResult {
        val requestWithPin = request.copy(pin = pin)
        return createCashOperation(kkmId, requestWithPin, CashOperationType.CASH_IN)
    }

    fun cashOut(kkmId: String, pin: String, request: CashOperationRequest): CashOperationResult {
        val requestWithPin = request.copy(pin = pin)
        return createCashOperation(kkmId, requestWithPin, CashOperationType.CASH_OUT)
    }

    fun retryReceiptDelivery(kkmId: String, documentId: String, pin: String): List<Pair<String, Boolean>> {
        requireKkm(kkmId)
        authorization.requireRole(kkmId, pin, setOf(UserRole.CASHIER, UserRole.ADMIN))
        val (snapshot, receipt) = storage.findFiscalDocumentWithReceiptPayload(documentId)
            ?: throw NotFoundException(ErrorMessages.documentNotFound(), "DOCUMENT_NOT_FOUND")
        if (snapshot.cashboxId != kkmId) {
            throw NotFoundException(ErrorMessages.documentNotFound(), "DOCUMENT_NOT_FOUND")
        }
        return receiptDeliveryHelper.retryDelivery(kkmId, documentId, receipt, snapshot)
    }

    private fun createCashOperation(
        kkmId: String,
        request: CashOperationRequest,
        type: CashOperationType
    ): CashOperationResult {
        val amountMoney = Money.fromTenge(request.amount)
        return fiscalOperationExecutor.executeIdempotentFiscalOperation(
            kkmId = kkmId,
            pin = request.pin,
            idempotencyKey = request.idempotencyKey,
            operationType = type.name,
            checkShift = {
                val shift = storage.findOpenShift(kkmId)
                    ?: throw ConflictException(ErrorMessages.shiftNotOpen(), "SHIFT_NOT_OPEN")
                shift.id
            },
            saveOperation = { documentId, now, shiftId ->
                storage.saveCashOperation(
                    kkmId = kkmId,
                    type = type.name,
                    amount = amountMoney,
                    documentId = documentId,
                    shiftId = shiftId,
                    createdAt = now
                )
                updateCashSumForOperation(
                    kkmId = kkmId,
                    shiftId = shiftId,
                    type = type,
                    amountBills = amountMoney.bills
                )
            },
            sendOfdCommand = { kkm, documentId ->
                val hasQueue = !queue.canSendDirectly(kkmId)
                val command = OfflineQueueCommandRequest(
                    kkmId = kkmId,
                    type = OfdCommandType.MONEY_PLACEMENT.value,
                    payloadRef = documentId
                )
                if (hasQueue) {
                    queue.enqueueOffline(command)
                    ofdResultQueuedOffline()
                } else {
                    kkmCommonHelper.sendOfdCommand(
                        kkm = kkm,
                        commandType = OfdCommandType.MONEY_PLACEMENT,
                        payloadRef = documentId
                    )
                }
            },
            processResult = { kkm, documentId, currentKkmId, ofdResult, commandType, now, _ ->
                processOfdDocumentResult(
                    kkm = kkm,
                    documentId = documentId,
                    kkmId = currentKkmId,
                    ofdResult = ofdResult,
                    commandType = commandType,
                    now = now,
                    receiptContext = null
                )
                val isOffline = ofdResult.status == OfdCommandStatus.TIMEOUT
                updateMoneyPlacementCountersFromDocument(documentId, isOffline)
            },
            buildResult = { documentId, ofdResult, deliveryStatus ->
                CashOperationResult(
                    documentId = documentId,
                    deliveryStatus = deliveryStatus,
                    deliveryError = ofdResult.errorMessage
                )
            }
        )
    }

    private fun ofdResultQueuedOffline(): OfdCommandResult = OfdCommandResult(
        status = OfdCommandStatus.TIMEOUT,
        responseBin = null,
        responseJson = null,
        responseToken = null,
        responseReqNum = null,
        resultCode = null,
        resultText = null,
        errorMessage = "OFFLINE queue not empty; document queued",
        fiscalSign = null,
        autonomousSign = null
    )

    private fun requireKkm(kkmId: String): KkmInfo {
        val kkm = authorization.requireKkm(kkmId)
        requireOperational(kkm)
        return kkm
    }

    private fun requireOperational(kkm: KkmInfo, allowExpiredShift: Boolean = false) {
        kkmCommonHelper.ensureSystemTimeValid()
        if (kkm.state == KkmState.BLOCKED.name) {
            throw ValidationException(ErrorMessages.kkmBlocked(), "KKM_BLOCKED")
        }
        if (kkm.state == KkmState.PROGRAMMING.name) {
            throw ValidationException(ErrorMessages.kkmInProgramming(), "KKM_IN_PROGRAMMING")
        }
        autonomousModeService.enforceAutonomousLimits(kkm)
        if (!allowExpiredShift) {
            shiftService.enforceShiftDuration(kkm)
        }
    }

    private fun processOfdDocumentResult(
        kkm: KkmInfo,
        documentId: String,
        kkmId: String,
        ofdResult: OfdCommandResult,
        commandType: OfdCommandType,
        now: Long,
        receiptContext: Pair<ReceiptRequest, String>?
    ) {
        val resultCode = ofdResult.resultCode
        updateKkmBlockedStateFromOfd(kkm, ofdResult, now)

        if (resultCode != null) {
            val success = resultCode == 0
            storage.updateReceiptStatus(
                documentId = documentId,
                fiscalSign = ofdResult.fiscalSign,
                autonomousSign = ofdResult.autonomousSign,
                ofdStatus = if (success) "SENT" else "FAILED",
                deliveredAt = if (success) now else null,
                isAutonomous = false
            )
            if (success) {
                clearAutonomousIfReady(kkm, now)
                if (commandType == OfdCommandType.TICKET && receiptContext != null) {
                    counters.updateForReceipt(
                        kkmId,
                        receiptContext.second,
                        receiptContext.first,
                        isOffline = false
                    )
                }
            }
            return
        }

        val autonomousSign = java.lang.String.valueOf(clock.now())
        storage.updateReceiptStatus(
            documentId = documentId,
            fiscalSign = null,
            autonomousSign = autonomousSign,
            ofdStatus = "PENDING",
            deliveredAt = null,
            isAutonomous = true
        )
        queue.enqueueOffline(
            OfflineQueueCommandRequest(
                kkmId = kkmId,
                type = commandType.value,
                payloadRef = documentId
            )
        )
        markAutonomousStarted(kkm, now)
        if (commandType == OfdCommandType.TICKET && receiptContext != null) {
            counters.updateForReceipt(
                kkmId,
                receiptContext.second,
                receiptContext.first,
                isOffline = true
            )
        }
    }

    private fun updateKkmBlockedStateFromOfd(kkm: KkmInfo, ofdResult: OfdCommandResult, now: Long) {
        val code = ofdResult.resultCode ?: return
        if (code == 15 && kkm.state != KkmState.BLOCKED.name) {
            storage.updateKkm(kkm.copy(updatedAt = now, state = KkmState.BLOCKED.name))
        } else if (code == 0 && kkm.state == KkmState.BLOCKED.name) {
            storage.updateKkm(kkm.copy(updatedAt = now, state = KkmState.ACTIVE.name))
        }
    }

    private fun markAutonomousStarted(kkm: KkmInfo, now: Long) {
        if (kkm.autonomousSince != null) return
        storage.updateKkm(kkm.copy(updatedAt = now, autonomousSince = now))
    }

    private fun clearAutonomousIfReady(kkm: KkmInfo, now: Long) {
        if (kkm.autonomousSince == null && kkm.state != KkmState.BLOCKED.name) return
        if (!queue.canSendDirectly(kkm.id)) return
        val nextState = if (kkm.state == KkmState.BLOCKED.name) KkmState.ACTIVE.name else kkm.state
        storage.updateKkm(kkm.copy(updatedAt = now, autonomousSince = null, state = nextState))
    }

    private fun updateCashSumForOperation(
        kkmId: String,
        shiftId: String,
        type: CashOperationType,
        amountBills: Long
    ) {
        if (amountBills == 0L) return
        val delta = when (type) {
            CashOperationType.CASH_IN -> amountBills
            CashOperationType.CASH_OUT -> -amountBills
        }
        fun update(scope: String, scopeShiftId: String?) {
            val current = storage.loadCounters(kkmId, scope, scopeShiftId)[CounterKeyFormats.CASH_SUM] ?: 0L
            val next = current + delta
            storage.upsertCounter(kkmId, scope, scopeShiftId, CounterKeyFormats.CASH_SUM, next)
        }
        update(CounterScopes.SHIFT, shiftId)
        update(CounterScopes.GLOBAL, null)
    }

    private fun updateMoneyPlacementCountersFromDocument(documentId: String, isOffline: Boolean) {
        val snapshot = storage.findFiscalDocumentById(documentId) ?: return
        val shiftId = snapshot.shiftId
        if (shiftId.isBlank()) return
        val kkmId = snapshot.cashboxId
        val amount = snapshot.totalAmount ?: 0L
        if (amount == 0L) return

        val opKey = when (snapshot.docType) {
            CashOperationType.CASH_IN.name -> "MONEY_PLACEMENT_DEPOSIT"
            CashOperationType.CASH_OUT.name -> "MONEY_PLACEMENT_WITHDRAWAL"
            else -> return
        }

        fun increment(scope: String, scopeShiftId: String?, key: String, delta: Long) {
            val current = storage.loadCounters(kkmId, scope, scopeShiftId)[key] ?: 0L
            storage.upsertCounter(kkmId, scope, scopeShiftId, key, current + delta)
        }

        fun updateScope(scope: String, scopeShiftId: String?) {
            increment(scope, scopeShiftId, CounterKeyFormats.MONEY_PLACEMENT_TOTAL_COUNT.format(opKey), 1L)
            increment(scope, scopeShiftId, CounterKeyFormats.MONEY_PLACEMENT_COUNT.format(opKey), 1L)
            increment(scope, scopeShiftId, CounterKeyFormats.MONEY_PLACEMENT_SUM.format(opKey), amount)
            if (isOffline) {
                increment(scope, scopeShiftId, CounterKeyFormats.MONEY_PLACEMENT_OFFLINE_COUNT.format(opKey), 1L)
            }
        }

        updateScope(CounterScopes.SHIFT, shiftId)
        updateScope(CounterScopes.GLOBAL, null)
    }
}
