package kz.mybrain.superkassa.core.application.service

import kz.mybrain.superkassa.core.application.error.ConflictException
import kz.mybrain.superkassa.core.application.error.ErrorMessages
import kz.mybrain.superkassa.core.application.error.NotFoundException
import kz.mybrain.superkassa.core.application.error.ValidationException
import kz.mybrain.superkassa.core.application.model.FactoryNumberResponse
import kz.mybrain.superkassa.core.application.model.KkmInitDirectRequest
import kz.mybrain.superkassa.core.application.model.KkmListParams
import kz.mybrain.superkassa.core.application.model.KkmListResult
import kz.mybrain.superkassa.core.application.model.OfdAuthInfoResponse
import kz.mybrain.superkassa.core.application.model.PrintDocumentType
import kz.mybrain.superkassa.core.application.model.UserCreateRequest
import kz.mybrain.superkassa.core.application.model.UserResponse
import kz.mybrain.superkassa.core.application.model.UserUpdateRequest
import kz.mybrain.superkassa.core.domain.model.*
import kz.mybrain.superkassa.core.domain.port.*
import kz.mybrain.superkassa.core.application.model.CoreSettings

/**
 * Основной сервис ККМ: фасадно делегирует работу специализированным подсервисам
 * для соблюдения лимитов на размер классов и следования принципу Single Responsibility.
 */
class KkmService(
    private val storage: StoragePort,
    private val queue: OfflineQueuePort,
    private val ofd: OfdManagerPort,
    ofdConfig: OfdConfigPort,
    private val delivery: DeliveryPort,
    private val kkmUserService: KkmUserService,
    private val shiftService: ShiftService,
    private val ofdSyncService: OfdSyncService,
    private val kkmRegistrationService: KkmRegistrationService,
    private val tokenCodec: TokenCodecPort,
    private val autonomousModeService: AutonomousModeService,
    private val fiscalOperationExecutor: FiscalOperationExecutor,
    private val reqNumService: ReqNumService,
    private val counters: CounterUpdaterPort,
    private val idGenerator: IdGenerator,
    private val clock: ClockPort,
    pinHasher: PinHasherPort,
    private val authorization: AuthorizationService,
    private val ofdCommandRequestBuilder: OfdCommandRequestBuilder,
    private val coreSettings: CoreSettings,
    private val receiptRenderPort: ReceiptRenderPort,
    private val documentConvertPort: DocumentConvertPort,
    private val timeValidator: TimeValidatorPort
) {

    private val kkmCommonHelper = KkmCommonHelper(
        storage = storage,
        clock = clock,
        timeValidator = timeValidator,
        tokenCodec = tokenCodec,
        reqNumService = reqNumService,
        ofdCommandRequestBuilder = ofdCommandRequestBuilder,
        ofd = ofd
    )

    private val receiptDeliveryHelper = ReceiptDeliveryHelper(
        delivery = delivery,
        coreSettings = coreSettings,
        documentConvertPort = documentConvertPort,
        receiptRenderPort = receiptRenderPort
    )

    private val kkmPrintService = KkmPrintService(
        storage = storage,
        receiptRenderPort = receiptRenderPort,
        documentConvertPort = documentConvertPort,
        authorization = authorization,
        clock = clock,
        kkmCommonHelper = kkmCommonHelper
    )

    private val kkmLifecycleService = KkmLifecycleService(
        storage = storage,
        queue = queue,
        clock = clock,
        authorization = authorization,
        kkmCommonHelper = kkmCommonHelper
    )

    private val kkmDocumentProcessor = KkmDocumentProcessor(
        storage = storage,
        queue = queue,
        fiscalOperationExecutor = fiscalOperationExecutor,
        counters = counters,
        clock = clock,
        authorization = authorization,
        kkmCommonHelper = kkmCommonHelper,
        autonomousModeService = autonomousModeService,
        shiftService = shiftService,
        receiptDeliveryHelper = receiptDeliveryHelper
    )

    // Registration & Initialization delegates
    fun initKkm(pin: String, request: KkmInitDirectRequest): KkmInfo =
        kkmRegistrationService.initKkm(pin, request)

    fun initKkmSimple(
        pin: String,
        request: kz.mybrain.superkassa.core.application.model.KkmInitSimpleRequest
    ): KkmInfo =
        kkmRegistrationService.initKkmSimple(pin, request)

    fun generateFactoryInfo(): FactoryNumberResponse =
        kkmRegistrationService.generateFactoryInfo()

    // KKM Retrieval & Listing
    fun getKkm(id: String): KkmInfo =
        storage.findKkm(id)
            ?: throw NotFoundException(
                message = ErrorMessages.kkmNotFound(),
                code = "KKM_NOT_FOUND"
            )

    fun listKkms(params: KkmListParams): KkmListResult {
        val items = storage.listKkms(
            limit = params.limit,
            offset = params.offset,
            state = params.state,
            search = params.search,
            sortBy = params.sortBy,
            sortOrder = params.sortOrder
        )
        val total = storage.countKkms(state = params.state, search = params.search)
        return KkmListResult(items = items, total = total)
    }

    fun deleteKkm(id: String, pin: String): Boolean =
        kkmLifecycleService.deleteKkm(id, pin)

    fun listCounters(kkmId: String, pin: String): List<CounterSnapshot> {
        authorization.requireKkm(kkmId)
        authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN))
        return storage.listCounters(kkmId)
    }

    // Settings
    fun updateKkmSettings(kkmId: String, pin: String, autoCloseShift: Boolean): KkmInfo =
        kkmLifecycleService.updateKkmSettings(kkmId, pin, autoCloseShift)

    fun updateTaxSettings(
        kkmId: String,
        pin: String,
        taxRegime: TaxRegime,
        defaultVatGroup: VatGroup
    ): KkmInfo =
        kkmLifecycleService.updateTaxSettings(kkmId, pin, taxRegime, defaultVatGroup)

    fun enterProgramming(kkmId: String, pin: String): KkmInfo =
        kkmLifecycleService.enterProgramming(kkmId, pin)

    fun exitProgramming(kkmId: String, pin: String): KkmInfo =
        kkmLifecycleService.exitProgramming(kkmId, pin)

    // User delegates
    fun listUsers(kkmId: String, pin: String): List<UserResponse> =
        kkmUserService.listUsers(kkmId, pin)

    fun createUser(kkmId: String, pin: String, request: UserCreateRequest): UserResponse =
        kkmUserService.createUser(kkmId, pin, request)

    fun updateUser(
        kkmId: String,
        userId: String,
        pin: String,
        request: UserUpdateRequest
    ): UserResponse =
        kkmUserService.updateUser(kkmId, userId, pin, request)

    fun deleteUser(kkmId: String, userId: String, pin: String): Boolean =
        kkmUserService.deleteUser(kkmId, userId, pin)

    // OFD delegates
    fun getOfdAuthInfo(kkmId: String, pin: String): OfdAuthInfoResponse =
        ofdSyncService.getOfdAuthInfo(kkmId, pin)

    fun updateOfdToken(kkmId: String, pin: String, token: String): Boolean =
        ofdSyncService.updateOfdToken(kkmId, pin, token)

    fun checkOfdConnection(kkmId: String): OfdCommandResult =
        ofdSyncService.checkOfdConnection(kkmId)

    fun getOfdInfo(kkmId: String): OfdCommandResult =
        ofdSyncService.getOfdInfo(kkmId)

    fun syncOfdServiceInfo(kkmId: String, pin: String): OfdCommandResult =
        ofdSyncService.syncOfdServiceInfo(kkmId, pin)

    fun syncOfdCounters(kkmId: String, pin: String): OfdCommandResult =
        ofdSyncService.syncOfdCounters(kkmId, pin)

    // Print & HTML & PDF delegates
    fun getReceiptHtml(kkmId: String, documentId: String, pin: String): String =
        kkmPrintService.getReceiptHtml(kkmId, documentId, pin)

    fun getPrintHtml(
        kkmId: String,
        type: PrintDocumentType,
        documentId: String?,
        shiftId: String?,
        pin: String
    ): String =
        kkmPrintService.getPrintHtml(kkmId, type, documentId, shiftId, pin)

    fun getPrintPdf(
        kkmId: String,
        type: PrintDocumentType,
        documentId: String?,
        shiftId: String?,
        pin: String
    ): ByteArray =
        kkmPrintService.getPrintPdf(kkmId, type, documentId, shiftId, pin)

    // Fiscal Operations / Receipt and Cash processing delegates
    fun createReceipt(request: ReceiptRequest): ReceiptResult =
        kkmDocumentProcessor.createReceipt(request)

    fun cashIn(kkmId: String, pin: String, request: CashOperationRequest): CashOperationResult =
        kkmDocumentProcessor.cashIn(kkmId, pin, request)

    fun cashOut(kkmId: String, pin: String, request: CashOperationRequest): CashOperationResult =
        kkmDocumentProcessor.cashOut(kkmId, pin, request)

    fun retryReceiptDelivery(kkmId: String, documentId: String, pin: String): List<Pair<String, Boolean>> =
        kkmDocumentProcessor.retryReceiptDelivery(kkmId, documentId, pin)

    // Shift Info delegates
    fun openShift(kkmId: String, pin: String): ShiftInfo =
        shiftService.openShift(kkmId, pin)

    fun closeShift(kkmId: String, pin: String): ReportResult =
        shiftService.closeShift(kkmId, pin)

    fun getOpenShift(kkmId: String, pin: String): ShiftInfo {
        kkmCommonHelper.ensureSystemTimeValid()
        val kkm = authorization.requireKkm(kkmId)
        requireOperational(kkm, allowExpiredShift = true)
        authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN, UserRole.CASHIER))
        return storage.findOpenShift(kkmId)
            ?: throw ConflictException(ErrorMessages.shiftNotOpen(), "SHIFT_NOT_OPEN")
    }

    fun listShifts(
        kkmId: String,
        limit: Int,
        offset: Int,
        pin: String
    ): List<ShiftInfo> {
        authorization.requireKkm(kkmId)
        authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN, UserRole.CASHIER))
        return storage.listShifts(kkmId, limit.coerceIn(1, 500), offset)
    }

    fun listShiftDocuments(
        kkmId: String,
        shiftId: String,
        limit: Int,
        offset: Int,
        pin: String
    ): List<FiscalDocumentSnapshot> {
        val kkm = authorization.requireKkm(kkmId)
        requireOperational(kkm, allowExpiredShift = true)
        authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN, UserRole.CASHIER))
        return storage.listFiscalDocumentsByShift(kkmId, shiftId, limit.coerceIn(1, 500), offset)
    }

    fun listFiscalDocumentsByPeriod(
        kkmId: String,
        fromInclusive: Long,
        toExclusive: Long,
        limit: Int,
        offset: Int,
        pin: String
    ): List<FiscalDocumentSnapshot> {
        authorization.requireKkm(kkmId)
        authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN, UserRole.CASHIER))
        return storage.listFiscalDocumentsByPeriod(
            kkmId,
            fromInclusive,
            toExclusive,
            limit.coerceIn(1, 500),
            offset
        )
    }

    fun createReport(kkmId: String, pin: String): ReportResult {
        return storage.inTransaction {
            val kkm = authorization.requireKkm(kkmId)
            requireOperational(kkm, allowExpiredShift = true)
            authorization.requireRole(kkm.id, pin, setOf(UserRole.ADMIN, UserRole.CASHIER))

            val documentId = idGenerator.nextId()
            val hasQueue = !queue.canSendDirectly(kkmId)
            if (hasQueue) {
                val command = OfflineQueueCommandRequest(
                    kkmId = kkmId,
                    type = OfdCommandType.REPORT.value,
                    payloadRef = documentId
                )
                queue.enqueueOffline(command)
                ReportResult(
                    documentId = documentId,
                    deliveryStatus = DeliveryStatus.OFFLINE_QUEUED
                )
            } else {
                val result = ofdSyncService.sendFiscalCommand(kkmId, OfdCommandType.REPORT, documentId)
                val (status, error) = when (result.status) {
                    OfdCommandStatus.OK -> DeliveryStatus.ONLINE_OK to null
                    OfdCommandStatus.TIMEOUT -> DeliveryStatus.OFFLINE_QUEUED to result.errorMessage
                    OfdCommandStatus.FAILED -> DeliveryStatus.ONLINE_ERROR to result.errorMessage
                }
                ReportResult(
                    documentId = documentId,
                    deliveryStatus = status,
                    deliveryError = error
                )
            }
        }
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
}
