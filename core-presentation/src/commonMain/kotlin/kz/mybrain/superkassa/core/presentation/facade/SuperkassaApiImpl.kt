package kz.mybrain.superkassa.core.presentation.facade

import kz.mybrain.superkassa.core.domain.exception.ConflictException
import kz.mybrain.superkassa.core.domain.exception.ErrorMessages
import kz.mybrain.superkassa.core.domain.exception.NotFoundException
import kz.mybrain.superkassa.core.domain.exception.ValidationException
import kz.mybrain.superkassa.core.domain.model.auth.UserRole
import kz.mybrain.superkassa.core.domain.model.common.CounterSnapshot
import kz.mybrain.superkassa.core.domain.model.common.TaxRegime
import kz.mybrain.superkassa.core.domain.model.common.VatGroup
import kz.mybrain.superkassa.core.domain.model.delivery.DeliveryStatus
import kz.mybrain.superkassa.core.domain.model.kkm.CashOperationRequest
import kz.mybrain.superkassa.core.domain.model.kkm.CashOperationResult
import kz.mybrain.superkassa.core.domain.model.kkm.CashOperationType
import kz.mybrain.superkassa.core.domain.model.kkm.FiscalDocumentSnapshot
import kz.mybrain.superkassa.core.domain.model.kkm.KkmInfo
import kz.mybrain.superkassa.core.domain.model.kkm.KkmState
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandResult
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandStatus
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandType
import kz.mybrain.superkassa.core.domain.model.queue.OfflineQueueCommandRequest
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptBranding
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptLayoutType
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptOperationType
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptRequest
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptResult
import kz.mybrain.superkassa.core.domain.model.report.PrintDocumentType
import kz.mybrain.superkassa.core.domain.model.report.ReportResult
import kz.mybrain.superkassa.core.domain.model.settings.CoreSettings
import kz.mybrain.superkassa.core.domain.model.shift.ShiftInfo
import kz.mybrain.superkassa.core.domain.port.ClockPort
import kz.mybrain.superkassa.core.domain.port.DeliveryPort
import kz.mybrain.superkassa.core.domain.port.DocumentConvertPort
import kz.mybrain.superkassa.core.domain.port.IdGeneratorPort
import kz.mybrain.superkassa.core.domain.port.OfdConfigPort
import kz.mybrain.superkassa.core.domain.port.OfdManagerPort
import kz.mybrain.superkassa.core.domain.port.OfflineQueuePort
import kz.mybrain.superkassa.core.domain.port.PinHasherPort
import kz.mybrain.superkassa.core.domain.port.ReceiptRenderPort
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.core.domain.port.TimeValidatorPort
import kz.mybrain.superkassa.core.domain.port.TokenCodecPort
import kz.mybrain.superkassa.core.domain.helper.KkmCommonHelper
import kz.mybrain.superkassa.core.domain.helper.ofd.OfdCommandRequestFactory
import kz.mybrain.superkassa.core.domain.helper.ReceiptDeliveryHelper
import kz.mybrain.superkassa.core.domain.usecase.auth.AuthorizeUserUseCase
import kz.mybrain.superkassa.core.domain.helper.common.IdempotentOperationExecutor
import kz.mybrain.superkassa.core.domain.usecase.counter.UpdateCountersUseCase
import kz.mybrain.superkassa.core.domain.usecase.kkm.DecommissionKkmUseCase
import kz.mybrain.superkassa.core.domain.usecase.kkm.EnforceAutonomousLimitsUseCase
import kz.mybrain.superkassa.core.domain.usecase.kkm.EnterProgrammingUseCase
import kz.mybrain.superkassa.core.domain.usecase.kkm.ExitProgrammingUseCase
import kz.mybrain.superkassa.core.domain.usecase.kkm.InitializeKkmRegistrationUseCase
import kz.mybrain.superkassa.core.domain.usecase.kkm.RegisterKkmUseCase
import kz.mybrain.superkassa.core.domain.usecase.kkm.UpdateKkmSettingsUseCase
import kz.mybrain.superkassa.core.domain.usecase.ofd.CheckOfdConnectionUseCase
import kz.mybrain.superkassa.core.domain.usecase.ofd.GenerateRequestNumberUseCase
import kz.mybrain.superkassa.core.domain.usecase.ofd.GetOfdAuthInfoUseCase
import kz.mybrain.superkassa.core.domain.usecase.ofd.GetOfdInfoUseCase
import kz.mybrain.superkassa.core.domain.usecase.ofd.SendFiscalCommandUseCase
import kz.mybrain.superkassa.core.domain.usecase.ofd.SyncOfdCountersUseCase
import kz.mybrain.superkassa.core.domain.usecase.ofd.SyncOfdServiceInfoUseCase
import kz.mybrain.superkassa.core.domain.usecase.ofd.UpdateOfdTokenUseCase
import kz.mybrain.superkassa.core.domain.usecase.print.GetPrintHtmlUseCase
import kz.mybrain.superkassa.core.domain.usecase.print.GetPrintPdfUseCase
import kz.mybrain.superkassa.core.domain.usecase.print.GetReceiptHtmlUseCase
import kz.mybrain.superkassa.core.domain.usecase.receipt.CreateCashOperationUseCase
import kz.mybrain.superkassa.core.domain.usecase.receipt.DeliverReceiptUseCase
import kz.mybrain.superkassa.core.domain.usecase.receipt.ProcessOfdDocumentResultUseCase
import kz.mybrain.superkassa.core.domain.usecase.receipt.ProcessReceiptUseCase
import kz.mybrain.superkassa.core.domain.usecase.receipt.RetryReceiptDeliveryUseCase
import kz.mybrain.superkassa.core.domain.usecase.shift.CloseShiftUseCase
import kz.mybrain.superkassa.core.domain.usecase.shift.OpenShiftUseCase
import kz.mybrain.superkassa.core.domain.usecase.user.CreateUserUseCase
import kz.mybrain.superkassa.core.domain.usecase.user.DeleteUserUseCase
import kz.mybrain.superkassa.core.domain.usecase.user.UpdateUserUseCase
import kz.mybrain.superkassa.core.presentation.mapper.ReceiptMapper
import kz.mybrain.superkassa.core.presentation.model.FactoryNumberResponse
import kz.mybrain.superkassa.core.presentation.model.KkmInitDirectRequest
import kz.mybrain.superkassa.core.presentation.model.KkmInitSimpleRequest
import kz.mybrain.superkassa.core.presentation.model.KkmListParams
import kz.mybrain.superkassa.core.presentation.model.KkmListResult
import kz.mybrain.superkassa.core.presentation.model.OfdAuthInfoResponse
import kz.mybrain.superkassa.core.presentation.model.ReceiptBuyRequest
import kz.mybrain.superkassa.core.presentation.model.ReceiptBuyReturnRequest
import kz.mybrain.superkassa.core.presentation.model.ReceiptSellRequest
import kz.mybrain.superkassa.core.presentation.model.ReceiptSellReturnRequest
import kz.mybrain.superkassa.core.presentation.model.UserCreateRequest
import kz.mybrain.superkassa.core.presentation.model.UserResponse
import kz.mybrain.superkassa.core.presentation.model.UserUpdateRequest
import kz.mybrain.superkassa.core.presentation.model.VatRateResponse

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
@Suppress("LargeClass", "TooManyFunctions")
class SuperkassaApiImpl(
    private val storage: StoragePort,
    private val queue: OfflineQueuePort,
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
    private val timeValidator: TimeValidatorPort
) : SuperkassaApi {

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

    // Print Use Cases
    private val getReceiptHtmlUseCase = GetReceiptHtmlUseCase(storage, receiptRenderPort, authorization)
    private val getPrintHtmlUseCase = GetPrintHtmlUseCase(
        storage = storage,
        receiptRenderPort = receiptRenderPort,
        authorizeUserUseCase = authorization,
        kkmCommonHelper = kkmCommonHelper,
        getReceiptHtml = getReceiptHtmlUseCase
    )
    private val getPrintPdfUseCase = GetPrintPdfUseCase(getPrintHtmlUseCase, documentConvertPort)

    // OFD Sync Use Cases
    private val sendFiscalCommandUseCase = SendFiscalCommandUseCase(authorization, kkmCommonHelper)
    private val checkOfdConnectionUseCase = CheckOfdConnectionUseCase(authorization, kkmCommonHelper)
    private val getOfdInfoUseCase = GetOfdInfoUseCase(authorization, kkmCommonHelper)
    private val syncOfdCountersUseCase = SyncOfdCountersUseCase(
        storage = storage,
        queue = queue,
        clock = clock,
        idGenerator = idGenerator,
        authorizeUserUseCase = authorization,
        kkmCommonHelper = kkmCommonHelper
    )
    private val syncOfdServiceInfoUseCase = SyncOfdServiceInfoUseCase(
        storage = storage,
        queue = queue,
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
    private val decommissionUseCase = DecommissionKkmUseCase(storage, queue)
    private val updateSettingsUseCase = UpdateKkmSettingsUseCase(storage, queue, clock)
    private val enterProgrammingUseCase = EnterProgrammingUseCase(storage, clock)
    private val exitProgrammingUseCase = ExitProgrammingUseCase(storage, clock)
    private val enforceAutonomousLimitsUseCase = EnforceAutonomousLimitsUseCase(storage, queue, clock)

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
        queue = queue,
        sendFiscalCommandUseCase = sendFiscalCommandUseCase,
        idGenerator = idGenerator,
        clock = clock,
        authorizeUser = authorization
    )

    // Document Processor Use Cases
    private val deliverReceiptUseCase = DeliverReceiptUseCase(receiptDeliveryHelper)
    private val processOfdDocumentResultUseCase = ProcessOfdDocumentResultUseCase(
        storage = storage,
        queue = queue,
        clock = clock,
        updateCountersUseCase = updateCountersUseCase,
        deliverReceipt = deliverReceiptUseCase
    )
    private val processReceiptUseCase = ProcessReceiptUseCase(
        storage = storage,
        queue = queue,
        fiscalOperationExecutor = IdempotentOperationExecutor(
            storage = storage,
            idGenerator = idGenerator,
            clock = clock,
            authorizeUserUseCase = authorization
        ),
        kkmCommonHelper = kkmCommonHelper,
        receiptDeliveryHelper = receiptDeliveryHelper,
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
        queue = queue,
        executor = IdempotentOperationExecutor(
            storage = storage,
            idGenerator = idGenerator,
            clock = clock,
            authorizeUserUseCase = authorization
        ),
        kkmCommonHelper = kkmCommonHelper,
        processOfdDocumentResult = processOfdDocumentResultUseCase
    )
    private val retryReceiptDeliveryUseCase = RetryReceiptDeliveryUseCase(
        storage = storage,
        authorizeUserUseCase = authorization,
        helper = receiptDeliveryHelper
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
            serviceInfo = request.serviceInfo,
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
            defaultVatGroup = request.defaultVatGroup,
            okved = request.okved
        )

    override fun generateFactoryInfo(): FactoryNumberResponse {
        val factoryNumber = idGenerator.generateFactoryNumber()
        return FactoryNumberResponse(
            factoryNumber = factoryNumber,
            manufactureYear = clock.currentYear()
        )
    }

    // KKM Retrieval & Listing
    override fun getKkm(id: String): KkmInfo =
        storage.findKkm(id)
            ?: throw NotFoundException(
                trilingualMessage = ErrorMessages.kkmNotFound(),
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
        return storage.listUsers(kkmId).map { UserResponse(it.id, it.name, it.role, it.pin) }
    }

    override fun createUser(kkmId: String, pin: String, request: UserCreateRequest): UserResponse {
        val user = createUserUseCase.execute(kkmId, pin, request.name, request.role, request.userPin)
        return UserResponse(user.id, user.name, user.role, user.pin)
    }

    override fun updateUser(
        kkmId: String,
        userId: String,
        pin: String,
        request: UserUpdateRequest
    ): UserResponse {
        val user = updateUserUseCase.execute(kkmId, userId, pin, request.name, request.role, request.userPin)
        return UserResponse(user.id, user.name, user.role, user.pin)
    }

    override fun deleteUser(kkmId: String, userId: String, pin: String): Boolean {
        deleteUserUseCase.execute(kkmId, userId, pin)
        return true
    }

    // OFD delegates
    override fun getOfdAuthInfo(kkmId: String, pin: String): OfdAuthInfoResponse {
        val authInfo = getOfdAuthInfoUseCase.execute(kkmId, pin)
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

    override fun syncOfdCounters(kkmId: String, pin: String): OfdCommandResult {
        val result = syncOfdCountersUseCase.execute(kkmId, pin)
        if (result.status == OfdCommandStatus.OK) {
            val kkm = storage.findKkm(kkmId)
            if (kkm != null) {
                enforceAutonomousLimitsUseCase.execute(kkm)
            }
        }
        return result
    }

    // Print & HTML & PDF delegates
    override fun getReceiptHtml(kkmId: String, documentId: String, pin: String, layout: ReceiptLayoutType?): String =
        getReceiptHtmlUseCase.execute(kkmId, documentId, pin, layout)

    override fun getPrintHtml(
        kkmId: String,
        type: PrintDocumentType,
        documentId: String?,
        shiftId: String?,
        pin: String,
        layout: ReceiptLayoutType?
    ): String =
        getPrintHtmlUseCase.execute(kkmId, type, documentId, shiftId, pin, layout)

    override fun getPrintPdf(
        kkmId: String,
        type: PrintDocumentType,
        documentId: String?,
        shiftId: String?,
        pin: String,
        layout: ReceiptLayoutType?
    ): ByteArray =
        getPrintPdfUseCase.execute(kkmId, type, documentId, shiftId, pin, layout)

    // Fiscal Operations / Receipt and Cash processing delegates
    override fun createReceipt(request: ReceiptRequest): ReceiptResult {
        val kkm = authorization.requireKkm(request.kkmId)
        requireOperational(kkm)
        return processReceiptUseCase.execute(request, kkm)
    }

    override fun createSellReceipt(kkmId: String, pin: String, request: ReceiptSellRequest): ReceiptResult {
        val receiptRequest = ReceiptMapper.toReceiptRequest(
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
            change = request.change,
            defaultVatGroup = request.defaultVatGroup,
            customerBin = request.customerBin
        )
        return createReceipt(receiptRequest)
    }

    override fun createSellReturnReceipt(kkmId: String, pin: String, request: ReceiptSellReturnRequest): ReceiptResult {
        val receiptRequest = ReceiptMapper.toReceiptRequest(
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
            change = request.change,
            parentTicket = request.parentTicket,
            defaultVatGroup = request.defaultVatGroup,
            customerBin = request.customerBin
        )
        return createReceipt(receiptRequest)
    }

    override fun createBuyReceipt(kkmId: String, pin: String, request: ReceiptBuyRequest): ReceiptResult {
        val receiptRequest = ReceiptMapper.toReceiptRequest(
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
            change = request.change,
            defaultVatGroup = request.defaultVatGroup,
            customerBin = request.customerBin
        )
        return createReceipt(receiptRequest)
    }

    override fun createBuyReturnReceipt(kkmId: String, pin: String, request: ReceiptBuyReturnRequest): ReceiptResult {
        val receiptRequest = ReceiptMapper.toReceiptRequest(
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
            change = request.change,
            parentTicket = request.parentTicket,
            defaultVatGroup = request.defaultVatGroup,
            customerBin = request.customerBin
        )
        return createReceipt(receiptRequest)
    }

    override fun cashIn(kkmId: String, pin: String, request: CashOperationRequest): CashOperationResult {
        val kkm = authorization.requireKkm(kkmId)
        requireOperational(kkm)
        return createCashOperationUseCase.execute(kkmId, request.copy(pin = pin), CashOperationType.CASH_IN)
    }

    override fun cashOut(kkmId: String, pin: String, request: CashOperationRequest): CashOperationResult {
        val kkm = authorization.requireKkm(kkmId)
        requireOperational(kkm)
        return createCashOperationUseCase.execute(kkmId, request.copy(pin = pin), CashOperationType.CASH_OUT)
    }

    override fun retryReceiptDelivery(kkmId: String, documentId: String, pin: String): List<Pair<String, Boolean>> =
        retryReceiptDeliveryUseCase.execute(kkmId, documentId, pin)

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
            ?: throw ConflictException(ErrorMessages.shiftNotOpen(), "SHIFT_NOT_OPEN")
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
        return storage.inTransaction {
            val kkm = authorization.requireKkm(kkmId)
            requireOperational(kkm)
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
                val result = sendFiscalCommandUseCase.execute(kkmId, OfdCommandType.REPORT, documentId)
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

    private fun requireOperational(kkm: KkmInfo) {
        kkmCommonHelper.ensureSystemTimeValid()
        if (kkm.state == KkmState.BLOCKED.name) {
            throw ValidationException(ErrorMessages.kkmBlocked(), "KKM_BLOCKED")
        }
        if (kkm.state == KkmState.PROGRAMMING.name) {
            throw ValidationException(ErrorMessages.kkmInProgramming(), "KKM_IN_PROGRAMMING")
        }
        enforceAutonomousLimitsUseCase.execute(kkm)
    }
}
