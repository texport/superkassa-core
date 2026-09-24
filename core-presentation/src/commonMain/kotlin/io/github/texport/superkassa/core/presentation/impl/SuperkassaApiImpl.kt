package io.github.texport.superkassa.core.presentation.impl

import io.github.texport.superkassa.core.presentation.api.model.kkm.DocumentDetailsResponse
import io.github.texport.superkassa.core.domain.api.model.common.VatGroup as DomainVatGroup
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandStatus
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdEnvironment
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdProvider
import io.github.texport.superkassa.core.domain.api.model.receipt.PaymentType
import io.github.texport.superkassa.core.domain.api.model.settings.CoreMode
import io.github.texport.superkassa.core.domain.api.model.kkm.CashOperationType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import io.github.texport.superkassa.core.domain.api.model.settings.DeliverySettings
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
import io.github.texport.superkassa.core.domain.impl.usecase.auth.PinGuard
import io.github.texport.superkassa.core.domain.impl.helper.common.IdempotentOperationExecutor
import io.github.texport.superkassa.core.domain.impl.usecase.counter.UpdateCountersUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.kkm.DecommissionKkmUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.kkm.EnforceAutonomousLimitsUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.kkm.EnterProgrammingUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.kkm.ExitProgrammingUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.kkm.InitializeKkmRegistrationUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.kkm.RegisterKkmUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.kkm.RequireOperationalUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.kkm.UpdateKkmSettingsUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.report.ProcessReportUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.ofd.CheckOfdConnectionUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.ofd.GenerateRequestNumberUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.ofd.GetOfdAuthInfoUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.ofd.GetOfdInfoUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.ofd.LookupNomenclatureUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.ofd.SendFiscalCommandUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.ofd.SyncOfdCountersUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.ofd.SyncOfdServiceInfoUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.ofd.UpdateOfdTokenUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.receipt.CreateCashOperationUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.receipt.DeliverReceiptUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.delivery.ReceiptDeliveryPlan
import io.github.texport.superkassa.core.domain.impl.usecase.receipt.ProcessOfdDocumentResultUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.receipt.ProcessReceiptUseCase
import io.github.texport.superkassa.core.presentation.api.model.receipt.CreateReceiptCommand
import io.github.texport.superkassa.core.domain.impl.usecase.queue.GetQueueStatusUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.queue.ListQueueItemsUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.queue.RetryFailedQueueItemsUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.shift.AutoCloseShiftUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.shift.CloseShiftUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.shift.ShiftDayLimit
import io.github.texport.superkassa.core.domain.impl.usecase.shift.OpenShiftUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.shift.RecalculateShiftCountersUseCase
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
import io.github.texport.superkassa.core.presentation.api.model.reference.*
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.presentation.impl.mapper.UserMapper
import io.github.texport.superkassa.core.presentation.impl.mapper.ReferenceMapper
import io.github.texport.superkassa.core.presentation.impl.mapper.toDomain
import io.github.texport.superkassa.core.domain.api.exception.ForbiddenException
import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.domain.impl.logging.LoggerConfig
import io.github.texport.superkassa.core.domain.impl.logging.LogLevel
import io.github.texport.superkassa.core.domain.impl.logging.LogListener
import io.github.texport.superkassa.core.domain.impl.logging.getLogger

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
    internal val pinHasher: PinHasherPort,
    internal val coreSettings: CoreSettings,
    internal val receiptRenderPort: ReceiptRenderPort,
    internal val documentConvertPort: DocumentConvertPort,
    internal val timeValidator: TimeValidatorPort,
    private val printApi: PrintApi,
    /** Счёт неверных пинов; сборка ядра даёт один на все входы по пину. */
    pinGuard: PinGuard,
    /**
     * Текущие настройки доставки. Читаются при каждой постановке доставки:
     * снимок настроек запуска держал включённый канал выключенным до перезапуска.
     */
    deliverySettings: () -> DeliverySettings? = { coreSettings.delivery }
) : SuperkassaApi, PrintApi by printApi {

    internal val logger = getLogger(SuperkassaApiImpl::class)
    internal val authorization = AuthorizeUserUseCase(storage, pinHasher, pinGuard)

    /**
     * Виды оплаты, которые принимает действующая версия протокола узла.
     * Один источник и для проверки чека, и для справочника: иначе кассир
     * увидел бы в списке вид оплаты, который узел отвергнет.
     */
    internal val supportedPaymentTypes: Set<PaymentType> =
        PaymentType.supportedBy(coreSettings.ofdProtocolVersion)

    override val queue: OfflineQueueApi = OfflineQueueApiImpl(
        queuePort = queuePort,
        getQueueStatusUseCase = GetQueueStatusUseCase(storage),
        listQueueItemsUseCase = ListQueueItemsUseCase(storage, authorization),
        retryFailedQueueItemsUseCase = RetryFailedQueueItemsUseCase(storage, authorization)
    )

    override fun setLogLevel(levelName: String) {
        try {
            val level = LogLevel.valueOf(levelName.uppercase())
            LoggerConfig.minLogLevel = level
        } catch (e: Exception) {
            // Ignore invalid log level and default to INFO or keep current
            println("Invalid log level: $levelName")
        }
    }

    override fun setLogListener(listener: LogListener) {
        LoggerConfig.listener = listener
    }

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
        settings = deliverySettings,
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
        requireOperational = requireOperationalUseCase,
        clock = clock
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
    internal val shiftDayLimit = ShiftDayLimit(storage, clock)
    internal val autoCloseShiftUseCase = AutoCloseShiftUseCase(storage, shiftDayLimit, closeShiftUseCase)

    // Document Processor Use Cases
    internal val deliverReceiptUseCase = DeliverReceiptUseCase(
        helper = receiptDeliveryHelper,
        storage = storage,
        plan = ReceiptDeliveryPlan(deliverySettings),
        clock = clock
    )
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
        authorizeUser = authorization,
        requireOperational = requireOperationalUseCase,
        recalculateShiftCounters = RecalculateShiftCountersUseCase(storage),
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
            // Документ поставлен в очередь, а не доставлен. Отвечать успехом
            // нельзя: касса сообщила бы о фискализации, которой не было.
            // Тот же статус приходит при настоящем обрыве связи.
            OfdCommandResult(status = OfdCommandStatus.TIMEOUT)
        },
        // Состав видов оплаты диктует версия протокола узла: в 2.0.4
        // оплаты в кредит и тарой нет, и чек с ней отвергается сразу.
        supportedPayments = supportedPaymentTypes,
        protocolVersion = coreSettings.ofdProtocolVersion
    )
    internal val createCashOperationUseCase = CreateCashOperationUseCase(
        storage = storage,
        recalculateShiftCounters = RecalculateShiftCountersUseCase(storage),
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

    @Throws(Exception::class)
    override fun listVatRates(): List<VatRateResponse> =
        DomainVatGroup.entries.map { CommonMapper.toResponse(it) }

    // Registration & Initialization delegates
    @Throws(Exception::class)
    override fun initKkm(request: KkmInitDirectRequest): KkmResponse =
        initKkmImpl(request)

    @Throws(Exception::class)
    override fun initKkmSimple(request: KkmInitSimpleRequest): KkmResponse =
        initKkmSimpleImpl(request)

    @Throws(Exception::class)
    override fun generateFactoryInfo(): FactoryNumberResponse =
        generateFactoryInfoImpl()

    // KKM Retrieval & Listing
    @Throws(Exception::class)
    override fun getKkm(id: String): KkmResponse =
        getKkmImpl(id)

    @Throws(Exception::class)
    override fun listKkms(params: KkmListParams): KkmListResponse =
        listKkmsImpl(params)

    @Throws(Exception::class)
    override fun deleteKkm(id: String, pin: String): Boolean =
        deleteKkmImpl(id, pin)

    @Throws(Exception::class)
    override fun validateCanDeleteKkm(id: String, pin: String): Boolean =
        validateCanDeleteKkmImpl(id, pin)

    @Throws(Exception::class)
    override fun listCounters(kkmId: String, pin: String): List<CounterSnapshotResponse> =
        listCountersImpl(kkmId, pin)

    // Settings
    @Throws(Exception::class)
    override fun updateKkmSettings(
        kkmId: String,
        pin: String,
        autoCloseShift: Boolean,
        autoCashout: Boolean
    ): KkmResponse =
        updateKkmSettingsImpl(kkmId, pin, autoCloseShift, autoCashout)

    @Throws(Exception::class)
    override fun updateTaxSettings(
        kkmId: String,
        pin: String,
        taxRegime: TaxRegime,
        defaultVatGroup: VatGroup
    ): KkmResponse =
        updateTaxSettingsImpl(kkmId, pin, taxRegime, defaultVatGroup)

    @Throws(Exception::class)
    override fun updateBrandingSettings(kkmId: String, pin: String, branding: ReceiptBrandingRequest): KkmResponse =
        updateBrandingSettingsImpl(kkmId, pin, branding)

    @Throws(Exception::class)
    override fun updateKkmName(kkmId: String, pin: String, name: String?): KkmResponse =
        updateKkmNameImpl(kkmId, pin, name)

    @Throws(Exception::class)
    override fun enterProgramming(kkmId: String, pin: String): KkmResponse =
        enterProgrammingImpl(kkmId, pin)

    @Throws(Exception::class)
    override fun exitProgramming(kkmId: String, pin: String): KkmResponse =
        exitProgrammingImpl(kkmId, pin)

    // User delegates
    @Throws(Exception::class)
    override fun listUsers(kkmId: String, pin: String): List<UserResponse> =
        listUsersImpl(kkmId, pin)

    @Throws(Exception::class)
    override fun currentUser(kkmId: String, pin: String): UserResponse =
        currentUserImpl(kkmId, pin)

    @Throws(Exception::class)
    override fun createUser(kkmId: String, pin: String, request: UserCreateRequest): UserResponse =
        createUserImpl(kkmId, pin, request)

    @Throws(Exception::class)
    override fun updateUser(kkmId: String, userId: String, pin: String, request: UserUpdateRequest): UserResponse =
        updateUserImpl(kkmId, userId, pin, request)

    @Throws(Exception::class)
    override fun deleteUser(kkmId: String, userId: String, pin: String): Boolean =
        deleteUserImpl(kkmId, userId, pin)

    // OFD delegates
    @Throws(Exception::class)
    override fun getOfdAuthInfo(pin: String, request: OfdAuthInfoRequest): OfdAuthInfoResponse =
        getOfdAuthInfoImpl(pin, request)

    @Throws(Exception::class)
    override fun updateOfdToken(kkmId: String, pin: String, token: String): Boolean =
        updateOfdTokenImpl(kkmId, pin, token)

    @Throws(Exception::class)
    override fun checkOfdConnection(kkmId: String): OfdCommandResponse =
        checkOfdConnectionImpl(kkmId)

    @Throws(Exception::class)
    override fun getOfdInfo(kkmId: String): OfdCommandResponse =
        getOfdInfoImpl(kkmId)

    @Throws(Exception::class)
    override fun syncOfdServiceInfo(kkmId: String, pin: String): OfdCommandResponse =
        syncOfdServiceInfoImpl(kkmId, pin)

    @Throws(Exception::class)
    override fun syncOfdCounters(kkmId: String, pin: String): OfdCommandResponse =
        syncOfdCountersImpl(kkmId, pin)

    // Fiscal Operations / Receipt and Cash processing delegates
    @Throws(Exception::class)
    override fun createReceipt(command: CreateReceiptCommand): ReceiptResponse =
        createReceiptImpl(command)

    @Throws(Exception::class)
    override fun createSellReceipt(kkmId: String, pin: String, request: ReceiptSellRequest): ReceiptResponse =
        createSellReceiptImpl(kkmId, pin, request)

    @Throws(Exception::class)
    override fun createSellReturnReceipt(
        kkmId: String,
        pin: String,
        request: ReceiptSellReturnRequest
    ): ReceiptResponse =
        createSellReturnReceiptImpl(kkmId, pin, request)

    @Throws(Exception::class)
    override fun createBuyReceipt(kkmId: String, pin: String, request: ReceiptBuyRequest): ReceiptResponse =
        createBuyReceiptImpl(kkmId, pin, request)

    @Throws(Exception::class)
    override fun createBuyReturnReceipt(kkmId: String, pin: String, request: ReceiptBuyReturnRequest): ReceiptResponse =
        createBuyReturnReceiptImpl(kkmId, pin, request)

    @Throws(Exception::class)
    override fun cashIn(kkmId: String, pin: String, request: CashOperationRequest): CashOperationResponse =
        cashInImpl(kkmId, pin, request)

    @Throws(Exception::class)
    override fun cashOut(kkmId: String, pin: String, request: CashOperationRequest): CashOperationResponse =
        cashOutImpl(kkmId, pin, request)

    // Shift Info delegates
    @Throws(Exception::class)
    override fun openShift(kkmId: String, pin: String): ShiftResponse =
        openShiftImpl(kkmId, pin)

    @Throws(Exception::class)
    override fun closeShift(kkmId: String, pin: String): ReportResponse =
        closeShiftImpl(kkmId, pin)

    @Throws(Exception::class)
    override fun autoCloseShift(kkmId: String): ReportResponse? =
        autoCloseShiftImpl(kkmId)

    @Throws(Exception::class)
    override fun getOpenShift(kkmId: String, pin: String): ShiftResponse =
        getOpenShiftImpl(kkmId, pin)

    @Throws(Exception::class)
    override fun getLocalOpenShift(kkmId: String, pin: String): ShiftResponse? =
        getLocalOpenShiftImpl(kkmId, pin)

    @Throws(Exception::class)
    override fun listShifts(kkmId: String, limit: Int, offset: Int, pin: String): List<ShiftResponse> =
        listShiftsImpl(kkmId, limit, offset, pin)

    @Throws(Exception::class)
    override fun listShiftDocuments(
        kkmId: String,
        shiftId: String,
        limit: Int,
        offset: Int,
        pin: String
    ): List<FiscalDocumentResponse> =
        listShiftDocumentsImpl(kkmId, shiftId, limit, offset, pin)

    @Throws(Exception::class)
    override fun getDocumentDetails(kkmId: String, documentId: String, pin: String): DocumentDetailsResponse =
        getDocumentDetailsImpl(kkmId, documentId, pin)

    @Throws(Exception::class)
    override fun listFiscalDocumentsByPeriod(
        kkmId: String,
        fromInclusive: Long,
        toExclusive: Long,
        limit: Int,
        offset: Int,
        pin: String
    ): List<FiscalDocumentResponse> =
        listFiscalDocumentsByPeriodImpl(kkmId, fromInclusive, toExclusive, limit, offset, pin)

    @Throws(Exception::class)
    override fun createReport(kkmId: String, pin: String): ReportResponse =
        createReportImpl(kkmId, pin)

    internal fun requireOperational(kkm: KkmInfo) {
        requireOperationalUseCase.execute(kkm)
    }

    @Throws(Exception::class)
    override fun lookupNomenclature(pin: String, request: NomenclatureLookupRequest): NomenclatureLookupResponse =
        lookupNomenclatureImpl(pin, request)

    @Throws(Exception::class)
    override fun authenticate(kkmId: String, pin: String): UserResponse {
        authorization.requireKkm(kkmId)
        authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN, UserRole.CASHIER))
        val pinHash = pinHasher.hash(pin)
        val user = storage.findUserByPin(kkmId, pinHash) ?: throw ForbiddenException(CoreStrings.userNotFound(), "USER_NOT_FOUND")
        return UserMapper.toResponse(user)
    }

    override fun getPaymentTypes(): List<PaymentTypeResponse> =
        io.github.texport.superkassa.core.presentation.api.model.receipt.PaymentType.entries.map {
            ReferenceMapper.toResponse(it, supported = it.toDomain() in supportedPaymentTypes)
        }

    override fun getReceiptDomainTypes(): List<ReceiptDomainTypeResponse> =
        io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptDomainType.entries.map {
            ReferenceMapper.toResponse(it)
        }

    override fun getDocumentTypes(): List<DocumentTypeResponse> =
        io.github.texport.superkassa.core.presentation.api.model.receipt.DocumentType.entries.map {
            ReferenceMapper.toResponse(
                it
            )
        }

    override fun getUserRoles(): List<UserRoleResponse> =
        io.github.texport.superkassa.core.presentation.api.model.user.UserRole.entries.map {
            ReferenceMapper.toResponse(
                it
            )
        }

    override fun getTaxRegimes(): List<TaxRegimeResponse> =
        io.github.texport.superkassa.core.presentation.api.model.kkm.TaxRegime.entries.map {
            ReferenceMapper.toResponse(
                it
            )
        }

    override fun getPaperWidths(): List<PaperWidthResponse> =
        io.github.texport.superkassa.core.presentation.api.model.receipt.PaperWidth.entries.map {
            ReferenceMapper.toResponse(
                it
            )
        }

    override fun getBrandingColors(): List<BrandingColorResponse> =
        io.github.texport.superkassa.core.presentation.api.model.receipt.BrandingColor.entries.map {
            ReferenceMapper.toResponse(
                it
            )
        }

    override fun getKkmStates(): List<KkmStateResponse> =
        io.github.texport.superkassa.core.presentation.api.model.kkm.KkmState.entries.map {
            ReferenceMapper.toResponse(
                it
            )
        }

    override fun getKkmModes(): List<KkmModeResponse> =
        io.github.texport.superkassa.core.presentation.api.model.kkm.KkmMode.entries.map {
            ReferenceMapper.toResponse(
                it
            )
        }

    override fun getShiftStatuses(): List<ShiftStatusResponse> =
        io.github.texport.superkassa.core.presentation.api.model.shift.ShiftStatus.entries.map {
            ReferenceMapper.toResponse(
                it
            )
        }

    override fun getDeliveryStatuses(): List<DeliveryStatusResponse> =
        io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus.entries.map {
            ReferenceMapper.toResponse(
                it
            )
        }

    override fun getOfdCommandStatuses(): List<OfdCommandStatusResponse> =
        io.github.texport.superkassa.core.presentation.api.model.ofd.OfdCommandStatus.entries.map {
            ReferenceMapper.toResponse(
                it
            )
        }

    override fun getReceiptOperationTypes(): List<ReceiptOperationTypeResponse> =
        io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptOperationType.entries.map {
            ReferenceMapper.toResponse(
                it
            )
        }

    override fun getOfdEnvironments(): List<OfdEnvironmentResponse> =
        io.github.texport.superkassa.core.presentation.api.model.ofd.OfdEnvironment.entries.map {
            ReferenceMapper.toResponse(
                it
            )
        }

    override fun getOfdProviders(): List<OfdProviderResponse> =
        io.github.texport.superkassa.core.presentation.api.model.ofd.OfdProvider.entries.map {
            ReferenceMapper.toResponse(
                it
            )
        }

    override fun getCoreModes(): List<CoreModeResponse> =
        io.github.texport.superkassa.core.presentation.api.model.kkm.CoreMode.entries.map {
            ReferenceMapper.toResponse(
                it
            )
        }

    override fun getAuthModes(): List<AuthModeResponse> =
        io.github.texport.superkassa.core.presentation.api.model.auth.AuthMode.entries.map {
            ReferenceMapper.toResponse(
                it
            )
        }

    override fun getReceiptLanguages(): List<ReceiptLanguageResponse> =
        io.github.texport.superkassa.core.presentation.api.model.kkm.ReceiptLanguage.entries.map {
            ReferenceMapper.toResponse(
                it
            )
        }

    override fun getReceiptLayoutTypes(): List<ReceiptLayoutTypeResponse> =
        io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptLayoutType.entries.map {
            ReferenceMapper.toResponse(
                it
            )
        }

    override fun getPrintDocumentTypes(): List<PrintDocumentTypeResponse> =
        io.github.texport.superkassa.core.presentation.api.model.receipt.PrintDocumentType.entries.map {
            ReferenceMapper.toResponse(
                it
            )
        }

    override fun getOfdCommandTypes(): List<OfdCommandTypeResponse> =
        io.github.texport.superkassa.core.presentation.api.model.ofd.OfdCommandType.entries.map {
            ReferenceMapper.toResponse(
                it
            )
        }

    override fun getCashOperationTypes(): List<CashOperationTypeResponse> =
        io.github.texport.superkassa.core.presentation.api.model.kkm.CashOperationType.entries.map {
            ReferenceMapper.toResponse(
                it
            )
        }
}
