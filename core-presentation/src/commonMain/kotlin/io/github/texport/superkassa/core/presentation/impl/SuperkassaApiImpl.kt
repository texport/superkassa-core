package io.github.texport.superkassa.core.presentation.impl

import io.github.texport.superkassa.core.domain.api.model.common.TaxRegime as DomainTaxRegime
import io.github.texport.superkassa.core.domain.api.model.common.VatGroup as DomainVatGroup
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandStatus
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.integration.DeliveryPort
import io.github.texport.superkassa.core.domain.api.port.integration.DocumentConvertPort
import io.github.texport.superkassa.core.domain.api.port.internal.IdGeneratorPort
import io.github.texport.superkassa.core.domain.api.port.internal.OfdConfigPort
import io.github.texport.superkassa.core.domain.api.port.internal.OfdManagerPort
import io.github.texport.superkassa.core.domain.api.port.internal.OfflineQueuePort
import io.github.texport.superkassa.core.domain.api.port.internal.PinHasherPort
import io.github.texport.superkassa.core.domain.api.port.internal.ReceiptRenderPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.api.port.integration.TimeValidatorPort
import io.github.texport.superkassa.core.domain.api.port.internal.TokenCodecPort
import io.github.texport.superkassa.core.domain.impl.helper.KkmCommonHelper
import io.github.texport.superkassa.core.domain.impl.helper.ofd.OfdCommandRequestFactory
import io.github.texport.superkassa.core.domain.impl.helper.ReceiptDeliveryHelper
import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase
import io.github.texport.superkassa.core.domain.impl.helper.common.IdempotentOperationExecutor
import io.github.texport.superkassa.core.domain.impl.usecase.counter.UpdateCountersUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.kkm.*
import io.github.texport.superkassa.core.domain.impl.usecase.report.ProcessReportUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.ofd.*
import io.github.texport.superkassa.core.domain.impl.usecase.receipt.*
import io.github.texport.superkassa.core.domain.impl.usecase.receipt.CreateReceiptCommand as DomainCreateReceiptCommand
import io.github.texport.superkassa.core.presentation.api.model.receipt.CreateReceiptCommand
import io.github.texport.superkassa.core.domain.impl.usecase.queue.GetQueueStatusUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.queue.ListQueueItemsUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.queue.RetryFailedQueueItemsUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.shift.CloseShiftUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.shift.OpenShiftUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.user.CreateUserUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.user.DeleteUserUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.user.UpdateUserUseCase
import io.github.texport.superkassa.core.presentation.api.SuperkassaApi
import io.github.texport.superkassa.core.presentation.api.PrintApi
import io.github.texport.superkassa.core.presentation.api.OfflineQueueApi
import io.github.texport.superkassa.core.presentation.impl.mapper.CommonMapper
import io.github.texport.superkassa.core.presentation.api.model.auth.*
import io.github.texport.superkassa.core.presentation.api.model.common.*
import io.github.texport.superkassa.core.presentation.api.model.kkm.*
import io.github.texport.superkassa.core.presentation.api.model.ofd.*
import io.github.texport.superkassa.core.presentation.api.model.queue.*
import io.github.texport.superkassa.core.presentation.api.model.receipt.*
import io.github.texport.superkassa.core.presentation.api.model.shift.*
import io.github.texport.superkassa.core.presentation.api.model.user.*

/**
 * Реализация API Superkassa ([SuperkassaApi]), делегирующая выполнение
 * операций соответствующим Use Case'ам предметной области (domain layer).
 */
class SuperkassaApiImpl(
    internal val storage: StoragePort,
    internal val queuePort: OfflineQueuePort,
    internal val ofd: OfdManagerPort,
    internal val ofdConfig: OfdConfigPort,
    internal val delivery: DeliveryPort,
    internal val tokenCodec: TokenCodecPort,
    internal val idGenerator: IdGeneratorPort,
    internal val clock: ClockPort,
    pinHasher: PinHasherPort,
    internal val coreSettings: CoreSettings,
    internal val receiptRenderPort: ReceiptRenderPort,
    internal val documentConvertPort: DocumentConvertPort,
    internal val timeValidator: TimeValidatorPort,
    private val printApi: PrintApi
) : SuperkassaApi, PrintApi by printApi {

    internal val authorization = AuthorizeUserUseCase(storage, pinHasher)

    override val queue: OfflineQueueApi = OfflineQueueApiImpl(
        queuePort = queuePort,
        getQueueStatusUseCase = GetQueueStatusUseCase(storage),
        listQueueItemsUseCase = ListQueueItemsUseCase(storage, authorization),
        retryFailedQueueItemsUseCase = RetryFailedQueueItemsUseCase(storage, authorization)
    )

    internal val generateRequestNumberUseCase = GenerateRequestNumberUseCase(storage)
    internal val ofdCommandRequestFactory = OfdCommandRequestFactory(ofdConfig)
    internal val updateCountersUseCase = UpdateCountersUseCase(storage)

    internal val kkmCommonHelper = KkmCommonHelper(
        storage = storage,
        clock = clock,
        timeValidator = timeValidator,
        tokenCodec = tokenCodec,
        generateRequestNumberUseCase = generateRequestNumberUseCase,
        ofdCommandRequestFactory = ofdCommandRequestFactory,
        ofd = ofd
    )

    internal val receiptDeliveryHelper = ReceiptDeliveryHelper(
        storage = storage,
        delivery = delivery,
        coreSettings = coreSettings,
        documentConvertPort = documentConvertPort,
        receiptRenderPort = receiptRenderPort
    )

    // User Use Cases
    internal val createUserUseCase = CreateUserUseCase(storage, idGenerator, clock, pinHasher, authorization)
    internal val updateUserUseCase = UpdateUserUseCase(storage, pinHasher, authorization)
    internal val deleteUserUseCase = DeleteUserUseCase(storage, authorization)

    // OFD Sync Use Cases
    internal val enforceAutonomousLimitsUseCase = EnforceAutonomousLimitsUseCase(storage, queuePort, clock)
    internal val sendFiscalCommandUseCase = SendFiscalCommandUseCase(authorization, kkmCommonHelper)
    internal val checkOfdConnectionUseCase = CheckOfdConnectionUseCase(authorization, kkmCommonHelper)
    internal val getOfdInfoUseCase = GetOfdInfoUseCase(authorization, kkmCommonHelper)
    internal val lookupNomenclatureUseCase = LookupNomenclatureUseCase(authorization, kkmCommonHelper)
    internal val syncOfdCountersUseCase = SyncOfdCountersUseCase(
        storage = storage,
        queue = queuePort,
        clock = clock,
        idGenerator = idGenerator,
        authorizeUserUseCase = authorization,
        kkmCommonHelper = kkmCommonHelper,
        enforceAutonomousLimitsUseCase = enforceAutonomousLimitsUseCase
    )
    internal val syncOfdServiceInfoUseCase = SyncOfdServiceInfoUseCase(
        storage = storage,
        queue = queuePort,
        clock = clock,
        idGenerator = idGenerator,
        authorizeUserUseCase = authorization,
        kkmCommonHelper = kkmCommonHelper
    )
    internal val getOfdAuthInfoUseCase = GetOfdAuthInfoUseCase(
        authorizeUserUseCase = authorization,
        tokenCodec = tokenCodec,
        generateRequestNumberUseCase = generateRequestNumberUseCase
    )
    internal val updateOfdTokenUseCase = UpdateOfdTokenUseCase(storage, clock, tokenCodec, authorization)

    // KKM Lifecycle & Settings Use Cases
    internal val decommissionUseCase = DecommissionKkmUseCase(storage, queuePort)
    internal val updateSettingsUseCase = UpdateKkmSettingsUseCase(storage, queuePort, clock)
    internal val enterProgrammingUseCase = EnterProgrammingUseCase(storage, clock)
    internal val exitProgrammingUseCase = ExitProgrammingUseCase(storage, clock)
    internal val requireOperationalUseCase = RequireOperationalUseCase(kkmCommonHelper, enforceAutonomousLimitsUseCase)
    internal val processReportUseCase = ProcessReportUseCase(
        storage = storage,
        queue = queuePort,
        sendFiscalCommandUseCase = sendFiscalCommandUseCase,
        idGenerator = idGenerator,
        authorizeUser = authorization,
        requireOperational = requireOperationalUseCase
    )

    // Registration Use Cases
    internal val initializeKkmRegistrationUseCase = InitializeKkmRegistrationUseCase(
        storage = storage,
        clock = clock,
        idGenerator = idGenerator,
        tokenCodec = tokenCodec,
        pinHasher = pinHasher,
        kkmCommonHelper = kkmCommonHelper
    )
    internal val registerKkmUseCase = RegisterKkmUseCase(
        storage = storage,
        ofdConfig = ofdConfig,
        tokenCodec = tokenCodec,
        idGenerator = idGenerator,
        clock = clock,
        kkmCommonHelper = kkmCommonHelper,
        initializeKkmRegistrationUseCase = initializeKkmRegistrationUseCase
    )

    // Shift Use Cases
    internal val openShiftUseCase = OpenShiftUseCase(
        storage = storage,
        idGenerator = idGenerator,
        clock = clock,
        authorizeUser = authorization
    )
    internal val closeShiftUseCase = CloseShiftUseCase(
        storage = storage,
        queue = queuePort,
        sendFiscalCommandUseCase = sendFiscalCommandUseCase,
        idGenerator = idGenerator,
        clock = clock,
        authorizeUser = authorization
    )

    // Document Processor Use Cases
    internal val deliverReceiptUseCase = DeliverReceiptUseCase(receiptDeliveryHelper)
    internal val processOfdDocumentResultUseCase = ProcessOfdDocumentResultUseCase(
        storage = storage,
        queue = queuePort,
        clock = clock,
        updateCountersUseCase = updateCountersUseCase,
        deliverReceipt = deliverReceiptUseCase
    )
    internal val processReceiptUseCase = ProcessReceiptUseCase(
        storage = storage,
        queue = queuePort,
        fiscalOperationExecutor = IdempotentOperationExecutor(
            storage = storage,
            idGenerator = idGenerator,
            clock = clock,
            authorizeUserUseCase = authorization,
            requireOperationalUseCase = requireOperationalUseCase
        ),
        kkmCommonHelper = kkmCommonHelper,
        receiptDeliveryHelper = receiptDeliveryHelper,
        authorizeUser = authorization,
        requireOperational = requireOperationalUseCase,
        processOfdDocumentResult = { kkm: KkmInfo,
                                     docId: String,
                                     currentKkmId: String,
                                     ofdResult: OfdCommandResult,
                                     cmdType: OfdCommandType,
                                     time: Long,
                                     ctx: Pair<ReceiptRequest, String>? ->
            processOfdDocumentResultUseCase.execute(kkm, docId, currentKkmId, ofdResult, cmdType, time, ctx)
        },
        ofdResultQueuedOffline = {
            OfdCommandResult(status = OfdCommandStatus.OK)
        }
    )
    internal val createCashOperationUseCase = CreateCashOperationUseCase(
        storage = storage,
        queue = queuePort,
        executor = IdempotentOperationExecutor(
            storage = storage,
            idGenerator = idGenerator,
            clock = clock,
            authorizeUserUseCase = authorization,
            requireOperationalUseCase = requireOperationalUseCase
        ),
        kkmCommonHelper = kkmCommonHelper,
        processOfdDocumentResult = processOfdDocumentResultUseCase
    )

    override fun listVatRates(): List<VatRateResponse> =
        DomainVatGroup.entries.map { CommonMapper.toResponse(it) }

    // Registration & Initialization delegates
    override fun initKkm(pin: String, request: KkmInitDirectRequest): KkmResponse =
        initKkmImpl(pin, request)

    override fun initKkmSimple(pin: String, request: KkmInitSimpleRequest): KkmResponse =
        initKkmSimpleImpl(pin, request)

    override fun generateFactoryInfo(): FactoryNumberResponse =
        generateFactoryInfoImpl()

    // KKM Retrieval & Listing
    override fun getKkm(id: String): KkmResponse =
        getKkmImpl(id)

    override fun listKkms(params: KkmListParams): KkmListResponse =
        listKkmsImpl(params)

    override fun deleteKkm(id: String, pin: String): Boolean =
        deleteKkmImpl(id, pin)

    override fun listCounters(kkmId: String, pin: String): List<CounterSnapshotResponse> =
        listCountersImpl(kkmId, pin)

    // Settings
    override fun updateKkmSettings(kkmId: String, pin: String, autoCloseShift: Boolean): KkmResponse =
        updateKkmSettingsImpl(kkmId, pin, autoCloseShift)

    override fun updateTaxSettings(kkmId: String, pin: String, taxRegime: TaxRegime, defaultVatGroup: VatGroup): KkmResponse =
        updateTaxSettingsImpl(kkmId, pin, taxRegime, defaultVatGroup)

    override fun updateBrandingSettings(kkmId: String, pin: String, branding: ReceiptBrandingRequest): KkmResponse =
        updateBrandingSettingsImpl(kkmId, pin, branding)

    override fun enterProgramming(kkmId: String, pin: String): KkmResponse =
        enterProgrammingImpl(kkmId, pin)

    override fun exitProgramming(kkmId: String, pin: String): KkmResponse =
        exitProgrammingImpl(kkmId, pin)

    // User delegates
    override fun listUsers(kkmId: String, pin: String): List<UserResponse> =
        listUsersImpl(kkmId, pin)

    override fun createUser(kkmId: String, pin: String, request: UserCreateRequest): UserResponse =
        createUserImpl(kkmId, pin, request)

    override fun updateUser(kkmId: String, userId: String, pin: String, request: UserUpdateRequest): UserResponse =
        updateUserImpl(kkmId, userId, pin, request)

    override fun deleteUser(kkmId: String, userId: String, pin: String): Boolean =
        deleteUserImpl(kkmId, userId, pin)

    // OFD delegates
    override fun getOfdAuthInfo(pin: String, request: OfdAuthInfoRequest): OfdAuthInfoResponse =
        getOfdAuthInfoImpl(pin, request)

    override fun updateOfdToken(kkmId: String, pin: String, token: String): Boolean =
        updateOfdTokenImpl(kkmId, pin, token)

    override fun checkOfdConnection(kkmId: String): OfdCommandResponse =
        checkOfdConnectionImpl(kkmId)

    override fun getOfdInfo(kkmId: String): OfdCommandResponse =
        getOfdInfoImpl(kkmId)

    override fun syncOfdServiceInfo(kkmId: String, pin: String): OfdCommandResponse =
        syncOfdServiceInfoImpl(kkmId, pin)

    override fun syncOfdCounters(kkmId: String, pin: String): OfdCommandResponse =
        syncOfdCountersImpl(kkmId, pin)

    // Fiscal Operations / Receipt and Cash processing delegates
    override fun createReceipt(command: CreateReceiptCommand): ReceiptResponse =
        createReceiptImpl(command)

    override fun createSellReceipt(kkmId: String, pin: String, request: ReceiptSellRequest): ReceiptResponse =
        createSellReceiptImpl(kkmId, pin, request)

    override fun createSellReturnReceipt(kkmId: String, pin: String, request: ReceiptSellReturnRequest): ReceiptResponse =
        createSellReturnReceiptImpl(kkmId, pin, request)

    override fun createBuyReceipt(kkmId: String, pin: String, request: ReceiptBuyRequest): ReceiptResponse =
        createBuyReceiptImpl(kkmId, pin, request)

    override fun createBuyReturnReceipt(kkmId: String, pin: String, request: ReceiptBuyReturnRequest): ReceiptResponse =
        createBuyReturnReceiptImpl(kkmId, pin, request)

    override fun cashIn(kkmId: String, pin: String, request: CashOperationRequest): CashOperationResponse =
        cashInImpl(kkmId, pin, request)

    override fun cashOut(kkmId: String, pin: String, request: CashOperationRequest): CashOperationResponse =
        cashOutImpl(kkmId, pin, request)

    // Shift Info delegates
    override fun openShift(kkmId: String, pin: String): ShiftResponse =
        openShiftImpl(kkmId, pin)

    override fun closeShift(kkmId: String, pin: String): ReportResponse =
        closeShiftImpl(kkmId, pin)

    override fun getOpenShift(kkmId: String, pin: String): ShiftResponse =
        getOpenShiftImpl(kkmId, pin)

    override fun listShifts(kkmId: String, limit: Int, offset: Int, pin: String): List<ShiftResponse> =
        listShiftsImpl(kkmId, limit, offset, pin)

    override fun listShiftDocuments(kkmId: String, shiftId: String, limit: Int, offset: Int, pin: String): List<FiscalDocumentResponse> =
        listShiftDocumentsImpl(kkmId, shiftId, limit, offset, pin)

    override fun listFiscalDocumentsByPeriod(
        kkmId: String,
        fromInclusive: Long,
        toExclusive: Long,
        limit: Int,
        offset: Int,
        pin: String
    ): List<FiscalDocumentResponse> =
        listFiscalDocumentsByPeriodImpl(kkmId, fromInclusive, toExclusive, limit, offset, pin)

    override fun createReport(kkmId: String, pin: String): ReportResponse =
        createReportImpl(kkmId, pin)

    internal fun requireOperational(kkm: KkmInfo) {
        requireOperationalUseCase.execute(kkm)
    }

    override fun lookupNomenclature(pin: String, request: NomenclatureLookupRequest): NomenclatureLookupResponse =
        lookupNomenclatureImpl(pin, request)
}
