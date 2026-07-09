package io.github.texport.superkassa.core.presentation.impl

import io.github.texport.superkassa.core.domain.exception.ConflictException
import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.domain.exception.NotFoundException
import io.github.texport.superkassa.core.domain.model.auth.UserRole
import io.github.texport.superkassa.core.domain.model.common.CounterSnapshot
import io.github.texport.superkassa.core.domain.model.common.TaxRegime
import io.github.texport.superkassa.core.domain.model.common.VatGroup
import io.github.texport.superkassa.core.domain.model.kkm.CashOperationRequest
import io.github.texport.superkassa.core.domain.model.kkm.CashOperationResult
import io.github.texport.superkassa.core.domain.model.kkm.CashOperationType
import io.github.texport.superkassa.core.domain.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.model.ofd.OfdCommandStatus
import io.github.texport.superkassa.core.domain.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.model.receipt.ReceiptBranding
import io.github.texport.superkassa.core.domain.model.receipt.ReceiptOperationType
import io.github.texport.superkassa.core.domain.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.model.receipt.ReceiptResult
import io.github.texport.superkassa.core.domain.model.report.ReportResult
import io.github.texport.superkassa.core.domain.model.settings.CoreSettings
import io.github.texport.superkassa.core.domain.model.shift.ShiftInfo
import io.github.texport.superkassa.core.domain.port.ClockPort
import io.github.texport.superkassa.core.domain.port.DeliveryPort
import io.github.texport.superkassa.core.domain.port.DocumentConvertPort
import io.github.texport.superkassa.core.domain.port.IdGeneratorPort
import io.github.texport.superkassa.core.presentation.api.model.NomenclatureLookupResponse
import io.github.texport.superkassa.core.presentation.api.model.NomenclatureLookupRequest
import io.github.texport.superkassa.core.presentation.impl.mapper.NomenclatureMapper
import io.github.texport.superkassa.core.domain.port.OfdConfigPort
import io.github.texport.superkassa.core.domain.port.OfdManagerPort
import io.github.texport.superkassa.core.domain.port.OfflineQueuePort
import io.github.texport.superkassa.core.domain.port.PinHasherPort
import io.github.texport.superkassa.core.domain.port.ReceiptRenderPort
import io.github.texport.superkassa.core.domain.port.StoragePort
import io.github.texport.superkassa.core.domain.port.TimeValidatorPort
import io.github.texport.superkassa.core.domain.port.TokenCodecPort
import io.github.texport.superkassa.core.domain.helper.KkmCommonHelper
import io.github.texport.superkassa.core.domain.helper.ofd.OfdCommandRequestFactory
import io.github.texport.superkassa.core.domain.helper.ReceiptDeliveryHelper
import io.github.texport.superkassa.core.domain.usecase.auth.AuthorizeUserUseCase
import io.github.texport.superkassa.core.domain.helper.common.IdempotentOperationExecutor
import io.github.texport.superkassa.core.domain.usecase.counter.UpdateCountersUseCase
import io.github.texport.superkassa.core.domain.usecase.kkm.DecommissionKkmUseCase
import io.github.texport.superkassa.core.domain.usecase.kkm.EnforceAutonomousLimitsUseCase
import io.github.texport.superkassa.core.domain.usecase.kkm.EnterProgrammingUseCase
import io.github.texport.superkassa.core.domain.usecase.kkm.ExitProgrammingUseCase
import io.github.texport.superkassa.core.domain.usecase.kkm.InitializeKkmRegistrationUseCase
import io.github.texport.superkassa.core.domain.usecase.kkm.RegisterKkmUseCase
import io.github.texport.superkassa.core.domain.usecase.kkm.RequireOperationalUseCase
import io.github.texport.superkassa.core.domain.usecase.kkm.UpdateKkmSettingsUseCase
import io.github.texport.superkassa.core.domain.usecase.report.ProcessReportUseCase
import io.github.texport.superkassa.core.domain.usecase.ofd.CheckOfdConnectionUseCase
import io.github.texport.superkassa.core.domain.usecase.ofd.GenerateRequestNumberUseCase
import io.github.texport.superkassa.core.domain.usecase.ofd.GetOfdAuthInfoUseCase
import io.github.texport.superkassa.core.domain.usecase.ofd.GetOfdInfoUseCase
import io.github.texport.superkassa.core.domain.usecase.ofd.SendFiscalCommandUseCase
import io.github.texport.superkassa.core.domain.usecase.ofd.SyncOfdCountersUseCase
import io.github.texport.superkassa.core.domain.usecase.ofd.SyncOfdServiceInfoUseCase
import io.github.texport.superkassa.core.domain.usecase.ofd.UpdateOfdTokenUseCase
import io.github.texport.superkassa.core.domain.usecase.ofd.LookupNomenclatureUseCase
import io.github.texport.superkassa.core.domain.usecase.receipt.CreateCashOperationUseCase
import io.github.texport.superkassa.core.domain.usecase.receipt.DeliverReceiptUseCase
import io.github.texport.superkassa.core.domain.usecase.receipt.ProcessOfdDocumentResultUseCase
import io.github.texport.superkassa.core.domain.usecase.receipt.ProcessReceiptUseCase
import io.github.texport.superkassa.core.domain.usecase.receipt.CreateReceiptCommand
import io.github.texport.superkassa.core.domain.usecase.shift.CloseShiftUseCase
import io.github.texport.superkassa.core.domain.usecase.shift.OpenShiftUseCase
import io.github.texport.superkassa.core.domain.usecase.user.CreateUserUseCase
import io.github.texport.superkassa.core.domain.usecase.user.DeleteUserUseCase
import io.github.texport.superkassa.core.domain.usecase.user.UpdateUserUseCase
import io.github.texport.superkassa.core.presentation.api.SuperkassaApi
import io.github.texport.superkassa.core.presentation.api.PrintApi
import io.github.texport.superkassa.core.presentation.impl.mapper.ReceiptMapper
import io.github.texport.superkassa.core.presentation.api.model.FactoryNumberResponse
import io.github.texport.superkassa.core.presentation.api.model.KkmInitDirectRequest
import io.github.texport.superkassa.core.presentation.api.model.toDomain
import io.github.texport.superkassa.core.presentation.api.model.KkmInitSimpleRequest
import io.github.texport.superkassa.core.presentation.api.model.KkmListParams
import io.github.texport.superkassa.core.presentation.api.model.KkmListResult
import io.github.texport.superkassa.core.presentation.api.model.OfdAuthInfoResponse
import io.github.texport.superkassa.core.presentation.api.model.OfdAuthInfoRequest
import io.github.texport.superkassa.core.presentation.api.model.ReceiptBuyRequest
import io.github.texport.superkassa.core.presentation.api.model.ReceiptBuyReturnRequest
import io.github.texport.superkassa.core.presentation.api.model.ReceiptSellRequest
import io.github.texport.superkassa.core.presentation.api.model.ReceiptSellReturnRequest
import io.github.texport.superkassa.core.presentation.api.model.UserCreateRequest
import io.github.texport.superkassa.core.presentation.api.model.UserResponse
import io.github.texport.superkassa.core.presentation.api.model.UserRoleDto
import io.github.texport.superkassa.core.presentation.api.model.UserUpdateRequest
import io.github.texport.superkassa.core.presentation.api.model.VatRateResponse

/**
 * Реализация API Superkassa ([SuperkassaApi]), делегирующая выполнение
 * операций соответствующим Use Case'ам предметной области (domain layer).
 *
 * @param storage Порт для доступа к хранилищу (БД).
 * @param queue Порт для работы с офлайн-очередью ОФД.
 * @param ofd Менеджер для выполнения сетевых команд ОФД.
 * @param ofdConfig Конфигурация параметров провайдеров ОФД.
 * @param delivery Порт для отправки чеков клиентам (SMS/Email).
 * @param tokenCodec Кодек для шифрования токенов авторизации ОФД.
 * @param idGenerator Генератор уникальных идентификаторов и номеров.
 * @param clock Провайдер системного времени.
 * @param pinHasher Хэшер ПИН-кодов пользователей.
 * @param coreSettings Общие системные настройки ядра.
 * @param receiptRenderPort Рендерер HTML-представлений чеков.
 * @param documentConvertPort Конвертер документов (HTML в PDF).
 * @param timeValidator Валидатор системного времени ККМ.
 */
class SuperkassaApiImpl(
    private val storage: StoragePort,
    private val queuePort: OfflineQueuePort,
    private val ofd: OfdManagerPort,
    private val ofdConfig: OfdConfigPort,
    private val delivery: DeliveryPort,
    private val tokenCodec: TokenCodecPort,
    private val idGenerator: IdGeneratorPort,
    private val clock: ClockPort,
    pinHasher: PinHasherPort,
    private val coreSettings: CoreSettings,
    private val receiptRenderPort: ReceiptRenderPort,
    private val documentConvertPort: DocumentConvertPort,
    private val timeValidator: TimeValidatorPort,
    private val printApi: PrintApi
) : SuperkassaApi, PrintApi by printApi {

    private val authorization = AuthorizeUserUseCase(storage, pinHasher)
    private val generateRequestNumberUseCase = GenerateRequestNumberUseCase(storage)
    private val ofdCommandRequestFactory = OfdCommandRequestFactory(ofdConfig)
    private val updateCountersUseCase = UpdateCountersUseCase(storage)

    private val kkmCommonHelper = KkmCommonHelper(
        storage = storage,
        clock = clock,
        timeValidator = timeValidator,
        tokenCodec = tokenCodec,
        generateRequestNumberUseCase = generateRequestNumberUseCase,
        ofdCommandRequestFactory = ofdCommandRequestFactory,
        ofd = ofd
    )

    private val receiptDeliveryHelper = ReceiptDeliveryHelper(
        storage = storage,
        delivery = delivery,
        coreSettings = coreSettings,
        documentConvertPort = documentConvertPort,
        receiptRenderPort = receiptRenderPort
    )

    // User Use Cases
    private val createUserUseCase = CreateUserUseCase(storage, idGenerator, clock, pinHasher, authorization)
    private val updateUserUseCase = UpdateUserUseCase(storage, pinHasher, authorization)
    private val deleteUserUseCase = DeleteUserUseCase(storage, authorization)

    // OFD Sync Use Cases
    private val enforceAutonomousLimitsUseCase = EnforceAutonomousLimitsUseCase(storage, queuePort, clock)
    private val sendFiscalCommandUseCase = SendFiscalCommandUseCase(authorization, kkmCommonHelper)
    private val checkOfdConnectionUseCase = CheckOfdConnectionUseCase(authorization, kkmCommonHelper)
    private val getOfdInfoUseCase = GetOfdInfoUseCase(authorization, kkmCommonHelper)
    private val lookupNomenclatureUseCase = LookupNomenclatureUseCase(authorization, kkmCommonHelper)
    private val syncOfdCountersUseCase = SyncOfdCountersUseCase(
        storage = storage,
        queue = queuePort,
        clock = clock,
        idGenerator = idGenerator,
        authorizeUserUseCase = authorization,
        kkmCommonHelper = kkmCommonHelper,
        enforceAutonomousLimitsUseCase = enforceAutonomousLimitsUseCase
    )
    private val syncOfdServiceInfoUseCase = SyncOfdServiceInfoUseCase(
        storage = storage,
        queue = queuePort,
        clock = clock,
        idGenerator = idGenerator,
        authorizeUserUseCase = authorization,
        kkmCommonHelper = kkmCommonHelper
    )
    private val getOfdAuthInfoUseCase = GetOfdAuthInfoUseCase(
        authorizeUserUseCase = authorization,
        tokenCodec = tokenCodec,
        generateRequestNumberUseCase = generateRequestNumberUseCase
    )
    private val updateOfdTokenUseCase = UpdateOfdTokenUseCase(storage, clock, tokenCodec, authorization)

    // KKM Lifecycle & Settings Use Cases
    private val decommissionUseCase = DecommissionKkmUseCase(storage, queuePort)
    private val updateSettingsUseCase = UpdateKkmSettingsUseCase(storage, queuePort, clock)
    private val enterProgrammingUseCase = EnterProgrammingUseCase(storage, clock)
    private val exitProgrammingUseCase = ExitProgrammingUseCase(storage, clock)
    private val requireOperationalUseCase = RequireOperationalUseCase(kkmCommonHelper, enforceAutonomousLimitsUseCase)
    private val processReportUseCase = ProcessReportUseCase(
        storage = storage,
        queue = queuePort,
        sendFiscalCommandUseCase = sendFiscalCommandUseCase,
        idGenerator = idGenerator,
        authorizeUser = authorization,
        requireOperational = requireOperationalUseCase
    )

    // Registration Use Cases
    private val initializeKkmRegistrationUseCase = InitializeKkmRegistrationUseCase(
        storage = storage,
        clock = clock,
        idGenerator = idGenerator,
        tokenCodec = tokenCodec,
        pinHasher = pinHasher,
        kkmCommonHelper = kkmCommonHelper
    )
    private val registerKkmUseCase = RegisterKkmUseCase(
        storage = storage,
        ofdConfig = ofdConfig,
        tokenCodec = tokenCodec,
        idGenerator = idGenerator,
        clock = clock,
        kkmCommonHelper = kkmCommonHelper,
        initializeKkmRegistrationUseCase = initializeKkmRegistrationUseCase
    )

    // Shift Use Cases
    private val openShiftUseCase = OpenShiftUseCase(
        storage = storage,
        idGenerator = idGenerator,
        clock = clock,
        authorizeUser = authorization
    )
    private val closeShiftUseCase = CloseShiftUseCase(
        storage = storage,
        queue = queuePort,
        sendFiscalCommandUseCase = sendFiscalCommandUseCase,
        idGenerator = idGenerator,
        clock = clock,
        authorizeUser = authorization
    )

    // Document Processor Use Cases
    private val deliverReceiptUseCase = DeliverReceiptUseCase(receiptDeliveryHelper)
    private val processOfdDocumentResultUseCase = ProcessOfdDocumentResultUseCase(
        storage = storage,
        queue = queuePort,
        clock = clock,
        updateCountersUseCase = updateCountersUseCase,
        deliverReceipt = deliverReceiptUseCase
    )
    private val processReceiptUseCase = ProcessReceiptUseCase(
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
    private val createCashOperationUseCase = CreateCashOperationUseCase(
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
        VatGroup.entries.map { VatRateResponse.from(it) }

    // Registration & Initialization delegates
    override fun initKkm(pin: String, request: KkmInitDirectRequest): KkmInfo =
        registerKkmUseCase.initKkm(
            pin = pin,
            ofdId = request.ofdId,
            ofdEnvironment = request.ofdEnvironment,
            ofdSystemId = request.ofdSystemId,
            ofdToken = request.ofdToken,
            kkmKgdId = request.kkmKgdId,
            factoryNumber = request.factoryNumber,
            manufactureYear = request.manufactureYear,
            serviceInfo = request.serviceInfo?.toDomain(),
            okved = request.okved
        )

    override fun initKkmSimple(
        pin: String,
        request: KkmInitSimpleRequest
    ): KkmInfo =
        registerKkmUseCase.initKkmSimple(
            pin = pin,
            ofdId = request.ofdId,
            ofdEnvironment = request.ofdEnvironment,
            ofdSystemId = request.ofdSystemId,
            ofdToken = request.ofdToken,
            defaultVatGroup = VatGroup.valueOf(request.defaultVatGroup.name),
            okved = request.okved
        )

    override fun generateFactoryInfo(): FactoryNumberResponse {
        val factoryNumber = idGenerator.generateFactoryNumber(coreSettings.kkmFactoryNumberPrefix)
        return FactoryNumberResponse(
            factoryNumber = factoryNumber,
            manufactureYear = clock.currentYear()
        )
    }

    // KKM Retrieval & Listing
    override fun getKkm(id: String): KkmInfo =
        storage.findKkm(id)
            ?: throw NotFoundException(
                trilingualMessage = CoreStrings.kkmNotFound(),
                code = "KKM_NOT_FOUND"
            )

    override fun listKkms(params: KkmListParams): KkmListResult {
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

    override fun deleteKkm(id: String, pin: String): Boolean {
        val kkm = authorization.requireKkm(id)
        authorization.requireRole(kkm.id, pin, setOf(UserRole.ADMIN))
        return decommissionUseCase.execute(kkm)
    }

    override fun listCounters(kkmId: String, pin: String): List<CounterSnapshot> {
        authorization.requireKkm(kkmId)
        authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN))
        return storage.listCounters(kkmId)
    }

    // Settings
    override fun updateKkmSettings(kkmId: String, pin: String, autoCloseShift: Boolean): KkmInfo {
        kkmCommonHelper.ensureSystemTimeValid()
        authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN))
        val kkm = authorization.requireKkm(kkmId)
        return updateSettingsUseCase.updateGeneralSettings(kkm, autoCloseShift)
    }

    override fun updateTaxSettings(
        kkmId: String,
        pin: String,
        taxRegime: TaxRegime,
        defaultVatGroup: VatGroup
    ): KkmInfo {
        kkmCommonHelper.ensureSystemTimeValid()
        authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN))
        val kkm = authorization.requireKkm(kkmId)
        return updateSettingsUseCase.updateTaxSettings(kkm, taxRegime, defaultVatGroup)
    }

    override fun updateBrandingSettings(
        kkmId: String,
        pin: String,
        branding: ReceiptBranding
    ): KkmInfo {
        kkmCommonHelper.ensureSystemTimeValid()
        authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN))
        val kkm = authorization.requireKkm(kkmId)
        return updateSettingsUseCase.updateBranding(kkm, branding)
    }

    override fun enterProgramming(kkmId: String, pin: String): KkmInfo {
        val kkm = authorization.requireKkm(kkmId)
        authorization.requireRole(kkm.id, pin, setOf(UserRole.ADMIN))
        return enterProgrammingUseCase.execute(kkm)
    }

    override fun exitProgramming(kkmId: String, pin: String): KkmInfo {
        val kkm = authorization.requireKkm(kkmId)
        authorization.requireRole(kkm.id, pin, setOf(UserRole.ADMIN))
        return exitProgrammingUseCase.execute(kkm)
    }

    // User delegates
    override fun listUsers(kkmId: String, pin: String): List<UserResponse> {
        authorization.requireKkm(kkmId)
        authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN), allowDefaultPin = true)
        return storage.listUsers(kkmId).map { UserResponse(it.id, it.name, UserRoleDto.valueOf(it.role.name), it.pin) }
    }

    override fun createUser(kkmId: String, pin: String, request: UserCreateRequest): UserResponse {
        val user = createUserUseCase.execute(
            kkmId,
            pin,
            request.name,
            UserRole.valueOf(request.role.name),
            request.userPin
        )
        return UserResponse(user.id, user.name, UserRoleDto.valueOf(user.role.name), user.pin)
    }

    override fun updateUser(
        kkmId: String,
        userId: String,
        pin: String,
        request: UserUpdateRequest
    ): UserResponse {
        val user = updateUserUseCase.execute(
            kkmId,
            userId,
            pin,
            request.name,
            request.role?.let { UserRole.valueOf(it.name) },
            request.userPin
        )
        return UserResponse(user.id, user.name, UserRoleDto.valueOf(user.role.name), user.pin)
    }

    override fun deleteUser(kkmId: String, userId: String, pin: String): Boolean {
        deleteUserUseCase.execute(kkmId, userId, pin)
        return true
    }

    // OFD delegates
    override fun getOfdAuthInfo(pin: String, request: OfdAuthInfoRequest): OfdAuthInfoResponse {
        val authInfo = getOfdAuthInfoUseCase.execute(request.kkmId, pin)
        return OfdAuthInfoResponse(token = authInfo.token, nextReqNum = authInfo.nextReqNum)
    }

    override fun updateOfdToken(kkmId: String, pin: String, token: String): Boolean =
        updateOfdTokenUseCase.execute(kkmId, pin, token)

    override fun checkOfdConnection(kkmId: String): OfdCommandResult =
        checkOfdConnectionUseCase.execute(kkmId)

    override fun getOfdInfo(kkmId: String): OfdCommandResult =
        getOfdInfoUseCase.execute(kkmId)

    override fun syncOfdServiceInfo(kkmId: String, pin: String): OfdCommandResult =
        syncOfdServiceInfoUseCase.execute(kkmId, pin)

    override fun syncOfdCounters(kkmId: String, pin: String): OfdCommandResult =
        syncOfdCountersUseCase.execute(kkmId, pin)

    // Fiscal Operations / Receipt and Cash processing delegates
    override fun createReceipt(command: CreateReceiptCommand): ReceiptResult =
        processReceiptUseCase.execute(command)

    override fun createSellReceipt(kkmId: String, pin: String, request: ReceiptSellRequest): ReceiptResult {
        val command = ReceiptMapper.toCreateReceiptCommand(
            kkmId = kkmId,
            pin = pin,
            operation = ReceiptOperationType.SELL,
            idempotencyKey = request.idempotencyKey,
            items = request.items,
            discountPercent = request.discountPercent,
            discountSum = request.discountSum,
            markupPercent = request.markupPercent,
            markupSum = request.markupSum,
            payments = request.payments,
            taken = request.taken,
            defaultVatGroup = request.defaultVatGroup,
            customerBin = request.customerBin
        )
        return createReceipt(command)
    }

    override fun createSellReturnReceipt(kkmId: String, pin: String, request: ReceiptSellReturnRequest): ReceiptResult {
        val command = ReceiptMapper.toCreateReceiptCommand(
            kkmId = kkmId,
            pin = pin,
            operation = ReceiptOperationType.SELL_RETURN,
            idempotencyKey = request.idempotencyKey,
            items = request.items,
            discountPercent = request.discountPercent,
            discountSum = request.discountSum,
            markupPercent = request.markupPercent,
            markupSum = request.markupSum,
            payments = request.payments,
            taken = request.taken,
            parentTicket = request.parentTicket,
            defaultVatGroup = request.defaultVatGroup,
            customerBin = request.customerBin
        )
        return createReceipt(command)
    }

    override fun createBuyReceipt(kkmId: String, pin: String, request: ReceiptBuyRequest): ReceiptResult {
        val command = ReceiptMapper.toCreateReceiptCommand(
            kkmId = kkmId,
            pin = pin,
            operation = ReceiptOperationType.BUY,
            idempotencyKey = request.idempotencyKey,
            items = request.items,
            discountPercent = request.discountPercent,
            discountSum = request.discountSum,
            markupPercent = request.markupPercent,
            markupSum = request.markupSum,
            payments = request.payments,
            taken = request.taken,
            defaultVatGroup = request.defaultVatGroup,
            customerBin = request.customerBin
        )
        return createReceipt(command)
    }

    override fun createBuyReturnReceipt(kkmId: String, pin: String, request: ReceiptBuyReturnRequest): ReceiptResult {
        val command = ReceiptMapper.toCreateReceiptCommand(
            kkmId = kkmId,
            pin = pin,
            operation = ReceiptOperationType.BUY_RETURN,
            idempotencyKey = request.idempotencyKey,
            items = request.items,
            discountPercent = request.discountPercent,
            discountSum = request.discountSum,
            markupPercent = request.markupPercent,
            markupSum = request.markupSum,
            payments = request.payments,
            taken = request.taken,
            parentTicket = request.parentTicket,
            defaultVatGroup = request.defaultVatGroup,
            customerBin = request.customerBin
        )
        return createReceipt(command)
    }

    override fun cashIn(kkmId: String, pin: String, request: CashOperationRequest): CashOperationResult =
        createCashOperationUseCase.execute(kkmId, request.copy(pin = pin), CashOperationType.CASH_IN)

    override fun cashOut(kkmId: String, pin: String, request: CashOperationRequest): CashOperationResult =
        createCashOperationUseCase.execute(kkmId, request.copy(pin = pin), CashOperationType.CASH_OUT)

    // Shift Info delegates
    override fun openShift(kkmId: String, pin: String): ShiftInfo =
        openShiftUseCase.execute(kkmId, pin)

    override fun closeShift(kkmId: String, pin: String): ReportResult =
        closeShiftUseCase.execute(kkmId, pin)

    override fun getOpenShift(kkmId: String, pin: String): ShiftInfo {
        kkmCommonHelper.ensureSystemTimeValid()
        val kkm = authorization.requireKkm(kkmId)
        requireOperational(kkm)
        authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN, UserRole.CASHIER))
        return storage.findOpenShift(kkmId)
            ?: throw ConflictException(CoreStrings.shiftNotOpen(), "SHIFT_NOT_OPEN")
    }

    override fun listShifts(
        kkmId: String,
        limit: Int,
        offset: Int,
        pin: String
    ): List<ShiftInfo> {
        authorization.requireKkm(kkmId)
        authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN, UserRole.CASHIER))
        return storage.listShifts(kkmId, limit.coerceIn(1, 500), offset)
    }

    override fun listShiftDocuments(
        kkmId: String,
        shiftId: String,
        limit: Int,
        offset: Int,
        pin: String
    ): List<FiscalDocumentSnapshot> {
        val kkm = authorization.requireKkm(kkmId)
        requireOperational(kkm)
        authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN, UserRole.CASHIER))
        return storage.listFiscalDocumentsByShift(kkmId, shiftId, limit.coerceIn(1, 500), offset)
    }

    override fun listFiscalDocumentsByPeriod(
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

    override fun createReport(kkmId: String, pin: String): ReportResult {
        return processReportUseCase.execute(kkmId, pin)
    }

    private fun requireOperational(kkm: KkmInfo) {
        requireOperationalUseCase.execute(kkm)
    }

    override fun lookupNomenclature(pin: String, request: NomenclatureLookupRequest): NomenclatureLookupResponse {
        val kkm = authorization.requireKkm(request.kkmId)
        authorization.requireRole(kkm.id, pin, setOf(UserRole.CASHIER, UserRole.ADMIN))

        val result = lookupNomenclatureUseCase.execute(request.kkmId, request.barcode)
        return NomenclatureMapper.toDto(result)
    }
}
