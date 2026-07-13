package io.github.texport.superkassa.core.data.api

import io.github.texport.superkassa.core.domain.api.port.integration.*
import io.github.texport.superkassa.core.data.impl.adapter.generator.UuidGeneratorAdapter
import io.github.texport.superkassa.core.data.impl.adapter.ofd.OfdConfigAdapter
import io.github.texport.superkassa.core.data.impl.adapter.ofd.OfdManagerAdapter
import io.github.texport.superkassa.core.data.impl.adapter.queue.OfdQueueCommandHandlerPortAdapter
import io.github.texport.superkassa.core.data.impl.adapter.queue.OfflineQueueAdapter
import io.github.texport.superkassa.core.data.impl.adapter.queue.StorageBackedLeaseLockAdapter
import io.github.texport.superkassa.core.data.impl.adapter.queue.StorageBackedQueueStorageAdapter
import io.github.texport.superkassa.core.data.impl.adapter.receipt.ReceiptRenderAdapter
import io.github.texport.superkassa.core.data.impl.adapter.security.Base64TokenCodecAdapter
import io.github.texport.superkassa.core.data.impl.adapter.security.Sha256PinHasherAdapter
import io.github.texport.superkassa.core.data.impl.ofd.OfdConfig as ImplOfdConfig
import io.github.texport.superkassa.core.data.impl.ofd.OfdProtocolCodec
import io.github.texport.superkassa.core.data.impl.ofd.strategy.*
import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import io.github.texport.superkassa.core.domain.api.model.settings.CoreMode
import io.github.texport.superkassa.core.domain.api.model.settings.StorageSettings
import io.github.texport.superkassa.core.presentation.api.SuperkassaApi
import io.github.texport.superkassa.core.presentation.impl.SuperkassaApiImpl
import io.github.texport.superkassa.core.presentation.impl.PrintApiImpl
import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.print.GetPrintHtmlUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.print.GetPrintPdfUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.print.GetReceiptHtmlUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.ofd.SendFiscalCommandUseCase
import io.github.texport.superkassa.core.domain.impl.helper.KkmCommonHelper
import io.github.texport.superkassa.core.domain.impl.helper.ofd.OfdCommandRequestFactory
import io.github.texport.superkassa.core.domain.impl.usecase.ofd.GenerateRequestNumberUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.shift.RecalculateShiftCountersUseCase
import kz.mybrain.network.OfdTcpNetworkClient

/**
 * Единый фабричный контейнер (сборщик) для инициализации ядра библиотеки Superkassa.
 *
 * Принимает 7 обязательных платформенно-зависимых портов интеграции, скрыто связывает
 * внутренние адаптеры и кодеки и предоставляет готовый к использованию [SuperkassaApi].
 */
class SuperkassaCoreEngine(
    private val storage: StoragePort,
    private val settings: CoreSettingsRepositoryPort,
    private val delivery: DeliveryPort,
    private val clock: ClockPort,
    private val timeValidator: TimeValidatorPort,
    private val qrCode: QrCodeGeneratorPort,
    private val pdfConverter: DocumentConvertPort
) {
    /**
     * Создает и конфигурирует экземпляр API ядра кассы.
     *
     * @param ownerId Уникальный идентификатор инстанса (используется для распределенных блокировок).
     * @return Объект [SuperkassaApi] для выполнения кассовых и фискальных операций.
     */
    fun buildApi(ownerId: String = "core-engine-default"): SuperkassaApi {
        val defaultSettings = CoreSettings(
            mode = CoreMode.DESKTOP,
            storage = StorageSettings(engine = "SQLITE", jdbcUrl = "jdbc:sqlite:superkassa.db")
        )
        val coreSettings = settings.loadOrCreate(defaultSettings)

        // 1. Создаем внутренние кодеки и сетевые клиенты
        val ofdConfig = OfdConfigAdapter()
        val ofdProtocolConfig = ImplOfdConfig(coreSettings.ofdProtocolVersion)
        val codec = OfdProtocolCodec()
        val networkClient = OfdTcpNetworkClient(
            timeoutMillis = (coreSettings.ofdTimeoutSeconds * 1000).toInt()
        )

        // 2. Инициализируем стратегии ОФД
        val recalculateShiftCounters = RecalculateShiftCountersUseCase(storage)
        val requestBuilders = listOf(
            ServiceRequestBuilderStrategy(),
            TicketRequestBuilderStrategy(storage),
            NomenclatureRequestBuilderStrategy(),
            MoneyPlacementRequestBuilderStrategy(storage),
            ReportRequestBuilderStrategy(storage, recalculateShiftCounters),
            CloseShiftRequestBuilderStrategy(storage, recalculateShiftCounters)
        )

        // 3. Создаем ОФД Менеджер
        val ofdManager = OfdManagerAdapter(
            config = ofdProtocolConfig,
            codec = codec,
            networkClient = networkClient,
            requestBuilders = requestBuilders,
            timeoutSeconds = coreSettings.ofdTimeoutSeconds,
            reconnectIntervalSeconds = coreSettings.ofdReconnectIntervalSeconds
        )

        // 4. Настраиваем кодеки безопасности и хелперы рендеринга
        val tokenCodec = Base64TokenCodecAdapter()
        val idGenerator = UuidGeneratorAdapter
        val pinHasher = Sha256PinHasherAdapter()
        val receiptRenderPort = ReceiptRenderAdapter(qrCode)

        // 5. Инициализируем оффлайн-очередь
        val leaseLockAdapter = StorageBackedLeaseLockAdapter(storage)
        val queueStorageAdapter = StorageBackedQueueStorageAdapter(storage)

        val authorization = AuthorizeUserUseCase(storage, pinHasher)
        val generateRequestNumber = GenerateRequestNumberUseCase(storage)
        val ofdRequestFactory = OfdCommandRequestFactory(ofdConfig)
        val kkmCommonHelper = KkmCommonHelper(
            storage = storage,
            clock = clock,
            timeValidator = timeValidator,
            tokenCodec = tokenCodec,
            generateRequestNumberUseCase = generateRequestNumber,
            ofdCommandRequestFactory = ofdRequestFactory,
            ofd = ofdManager
        )
        val sendFiscalCommand = SendFiscalCommandUseCase(authorization, kkmCommonHelper)

        val queueCommandHandlerAdapter = OfdQueueCommandHandlerPortAdapter(
            sendFiscalCommand = sendFiscalCommand,
            storage = storage,
            clock = clock
        )

        val queuePort = OfflineQueueAdapter(
            storage = queueStorageAdapter,
            lockPort = leaseLockAdapter,
            handler = queueCommandHandlerAdapter,
            ownerId = ownerId
        )

        // 6. Инициализируем Use Cases для PrintApi
        val getReceiptHtmlUseCase = GetReceiptHtmlUseCase(storage, receiptRenderPort, authorization)
        val getPrintHtmlUseCase = GetPrintHtmlUseCase(storage, receiptRenderPort, authorization, kkmCommonHelper, getReceiptHtmlUseCase)
        val getPrintPdfUseCase = GetPrintPdfUseCase(getPrintHtmlUseCase, pdfConverter)
        val printApi = PrintApiImpl(getReceiptHtmlUseCase, getPrintHtmlUseCase, getPrintPdfUseCase, pdfConverter)

        // 7. Сборка фасада SuperkassaApi
        return SuperkassaApiImpl(
            storage = storage,
            queuePort = queuePort,
            ofd = ofdManager,
            ofdConfig = ofdConfig,
            delivery = delivery,
            tokenCodec = tokenCodec,
            idGenerator = idGenerator,
            clock = clock,
            pinHasher = pinHasher,
            coreSettings = coreSettings,
            receiptRenderPort = receiptRenderPort,
            documentConvertPort = pdfConverter,
            timeValidator = timeValidator,
            printApi = printApi
        )
    }
}
