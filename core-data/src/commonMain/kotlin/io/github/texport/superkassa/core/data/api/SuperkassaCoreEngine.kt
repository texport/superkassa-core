@file:Suppress("WildcardImport", "ParameterListWrapping", "ArgumentListWrapping", "NoConsecutiveBlankLines", "NoTrailingSpaces")

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
import io.github.texport.superkassa.core.domain.impl.usecase.auth.PinGuard
import io.github.texport.superkassa.core.domain.impl.usecase.print.GetPrintHtmlUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.print.GetPrintPdfUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.print.GetReceiptHtmlUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.ofd.SendFiscalCommandUseCase
import io.github.texport.superkassa.core.domain.impl.helper.KkmCommonHelper
import io.github.texport.superkassa.core.domain.impl.helper.ofd.OfdCommandRequestFactory
import io.github.texport.superkassa.core.domain.impl.usecase.ofd.GenerateRequestNumberUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.shift.RecalculateShiftCountersUseCase
import kz.mybrain.network.OfdNetworkClient
import kz.mybrain.network.OfdTcpNetworkClient
import io.github.texport.superkassa.core.domain.api.port.internal.ReceiptRenderPort
import io.github.texport.superkassa.core.presentation.api.DeliveryApi
import io.github.texport.superkassa.core.presentation.impl.DeliveryApiImpl
import io.github.texport.superkassa.core.presentation.api.SettingsApi
import io.github.texport.superkassa.core.presentation.impl.SettingsApiImpl
import io.github.texport.superkassa.core.domain.api.model.settings.withDeploymentOwned
import io.github.texport.superkassa.core.domain.impl.usecase.print.GetDocumentPrintHtmlUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.print.protocol.GetProtocolPrintHtmlUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.settings.GetSettingsUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.settings.UpdateSettingsUseCase

/**
 * Единый фабричный контейнер (сборщик) для инициализации ядра библиотеки Superkassa.
 *
 * Принимает 7 обязательных платформенно-зависимых портов интеграции, скрыто связывает
 * внутренние адаптеры и кодеки и предоставляет готовый к использованию [SuperkassaApi].
 */
class SuperkassaCoreEngine(
    private val storage: StoragePort,
    /**
     * Счёт неверных пинов. Сборка на Room передаёт счёт своей базы
     * ([io.github.texport.superkassa.coredatabase.api.RoomStorage.pinAttempts]),
     * узел — [io.github.texport.superkassa.core.domain.impl.usecase.auth.MemoryPinAttempts].
     * Из хранилища он не выводится: обёртка хранилища молча уводила счёт
     * в память, и блокировка пина не переживала перезапуск.
     */
    pinAttempts: PinAttemptsPort,
    private val settings: CoreSettingsRepositoryPort,
    private val delivery: DeliveryPort,
    private val clock: ClockPort,
    private val timeValidator: TimeValidatorPort,
    private val qrCode: QrCodeGeneratorPort,
    private val pdfConverter: DocumentConvertPort,
    /**
     * Транспорт до ОФД. По умолчанию — TCP-клиент со сроком из настроек;
     * другой подставляют там, где настоящего ОФД нет, — в проверках сборки.
     */
    private val ofdTransport: OfdNetworkClient? = null
) {
    /**
     * Рисовальщик печатных форм ядра.
     *
     * Один на сборку и открыт наружу: доставка и печать пакета протокола
     * рисуют тем же рисовальщиком, что и фасад, без доступа к его полям.
     */
    val receiptRenderer: ReceiptRenderPort = ReceiptRenderAdapter(qrCode)

    /**
     * Счёт неверных пинов — один на сборку: фасад, печать и доставка
     * пускают по пину через него, и перебор не делится между входами.
     */
    private val pinGuard = PinGuard(pinAttempts, clock::now)

    /**
     * Повтор доставки чека покупателю.
     *
     * Собирается из тех же частей, что и фасад: хеш пина — тот же, которым
     * фасад пускает кассира, а не своя копия у вызывающего.
     *
     * @throws IllegalStateException если настройки ядра ещё не заведены: сначала [buildApi].
     */
    fun buildDeliveryApi(): DeliveryApi = DeliveryApiImpl(
        storage = storage,
        pinHasher = Sha256PinHasherAdapter(),
        delivery = delivery,
        coreSettings = checkNotNull(settings.load()) { "Core settings are not created: build the API first" },
        documentConvertPort = pdfConverter,
        receiptRenderPort = receiptRenderer,
        pinGuard = pinGuard
    )

    /**
     * Настройки ядра: чтение и правка по правилам ядра.
     *
     * ОФД и версия протокола здесь те же, что переданы в [buildApi]: ими
     * владеет запуск, и сохранённая запись их не перекрывает.
     */
    fun buildSettingsApi(
        ofdProviderId: String = DEFAULT_OFD_PROVIDER_ID,
        ofdProtocolVersion: String = DEFAULT_OFD_PROTOCOL_VERSION
    ): SettingsApi {
        val current = GetSettingsUseCase(settings, deploymentSettings(ofdProviderId, ofdProtocolVersion))
        return SettingsApiImpl(current, UpdateSettingsUseCase(current, settings))
    }

    /**
     * Настройки первого запуска с полями, которыми владеет запуск.
     *
     * Правка настроек разрешена, как и у узла на рабочем месте: запретить
     * её владелец может сам, сохранив запрет в настройках.
     */
    private fun deploymentSettings(ofdProviderId: String, ofdProtocolVersion: String) = CoreSettings(
        mode = CoreMode.DESKTOP,
        storage = StorageSettings(engine = "SQLITE", jdbcUrl = "jdbc:sqlite:superkassa.db"),
        allowChanges = true,
        ofdProviderId = ofdProviderId,
        ofdProtocolVersion = ofdProtocolVersion
    )

    /**
     * Создает и конфигурирует экземпляр API ядра кассы.
     *
     * @param ownerId Уникальный идентификатор инстанса (используется для распределенных блокировок).
     * @return Объект [SuperkassaApi] для выполнения кассовых и фискальных операций.
     */
    fun buildApi(
        ownerId: String = "core-engine-default",
        ofdProviderId: String = DEFAULT_OFD_PROVIDER_ID,
        ofdProtocolVersion: String = DEFAULT_OFD_PROTOCOL_VERSION
    ): SuperkassaApi {
        val defaultSettings = deploymentSettings(ofdProviderId, ofdProtocolVersion)
        // Какой ОФД и по какой версии обслуживает узел, решает вызывающий,
        // а не запись, сохранённая при первом запуске: иначе сменить их
        // перезапуском нельзя.
        val coreSettings = settings.loadOrCreate(defaultSettings).withDeploymentOwned(defaultSettings)

        // 1. Создаем внутренние кодеки и сетевые клиенты
        val ofdConfig = OfdConfigAdapter()
        val ofdProtocolConfig = ImplOfdConfig(coreSettings.ofdProtocolVersion)
        val codec = OfdProtocolCodec()
        // Срок ожидания ответа у сетевого клиента и у менеджера — один
        // и тот же из настроек: соединение, отправка и чтение укладываются
        // в него целиком, поэтому отдельного срока на соединение нет.
        val networkClient = ofdTransport ?: OfdTcpNetworkClient(
            timeoutMillis = (coreSettings.ofdTimeoutSeconds * SECONDS_TO_MILLIS).toInt()
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
            reconnectIntervalSeconds = coreSettings.ofdReconnectIntervalSeconds,
            now = clock::now
        )

        // 4. Настраиваем кодеки безопасности и хелперы рендеринга
        val tokenCodec = Base64TokenCodecAdapter()
        val idGenerator = UuidGeneratorAdapter
        val pinHasher = Sha256PinHasherAdapter()
        val receiptRenderPort = receiptRenderer

        // 5. Инициализируем оффлайн-очередь
        val leaseLockAdapter = StorageBackedLeaseLockAdapter(storage)
        val queueStorageAdapter = StorageBackedQueueStorageAdapter(storage)

        val authorization = AuthorizeUserUseCase(storage, pinHasher, pinGuard)
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
        val getPrintHtmlUseCase = GetPrintHtmlUseCase(
            storage,
            receiptRenderPort,
            authorization,
            kkmCommonHelper,
            getReceiptHtmlUseCase
        )
        val getPrintPdfUseCase = GetPrintPdfUseCase(getPrintHtmlUseCase, pdfConverter)
        val printApi = PrintApiImpl(
            getReceiptHtmlUseCase,
            getPrintHtmlUseCase,
            getPrintPdfUseCase,
            pdfConverter,
            GetDocumentPrintHtmlUseCase(storage, getPrintHtmlUseCase),
            GetProtocolPrintHtmlUseCase(authorization, receiptRenderPort)
        )

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
            printApi = printApi,
            pinGuard = pinGuard
        )
    }

    companion object {
        /** Провайдер ОФД по умолчанию, если вызывающий не указал другой. */
        const val DEFAULT_OFD_PROVIDER_ID: String = "KAZAKHTELECOM"

        /** Версия протокола по умолчанию, если вызывающий не указал другую. */
        const val DEFAULT_OFD_PROTOCOL_VERSION: String = "203"

        /** Перевод срока ожидания из секунд настройки в миллисекунды клиента. */
        private const val SECONDS_TO_MILLIS = 1000L

        /**
         * Быстрый метод инициализации ядра по умолчанию с in-memory хранилищем.
         */
        fun createDefault(ofdProviderId: String = DEFAULT_OFD_PROVIDER_ID): SuperkassaApi {
            val roomStoragePair = io.github.texport.superkassa.coredatabase.api.RoomStorageFactory.createDefaultStorage()
            return createWithStorage(roomStoragePair, ofdProviderId)
        }

        /**
         * Промышленный метод инициализации ядра с дисковой БД SQLite (Room KMP).
         *
         * @param dbPath Путь к файлу базы данных SQLite.
         */
        fun createProduction(
            dbPath: String = "superkassa.db",
            ofdProviderId: String = DEFAULT_OFD_PROVIDER_ID
        ): SuperkassaApi {
            val roomStoragePair = io.github.texport.superkassa.coredatabase.api.RoomStorageFactory.createRoomStorage(dbPath)
            return createWithStorage(roomStoragePair, ofdProviderId)
        }

        /**
         * Создает экземпляр ядра для Desktop/Server с указанным файлом базы данных.
         */
        fun createDesktop(dbPath: String = "superkassa_desktop.db"): SuperkassaApi = createProduction(dbPath)

        /**
         * Создает экземпляр ядра для Android приложения.
         */
        fun createAndroid(dbName: String = "superkassa_android.db"): SuperkassaApi = createProduction(dbName)

        /**
         * Создает экземпляр ядра для iOS приложения.
         */
        fun createIos(dbName: String = "superkassa_ios.db"): SuperkassaApi = createProduction(dbName)

        private fun noConverter(): Nothing =
            throw UnsupportedOperationException("Document conversion is not configured in this assembly")

        private fun createWithStorage(
            roomStoragePair: io.github.texport.superkassa.coredatabase.api.RoomStoragePair,
            ofdProviderId: String
        ): SuperkassaApi {
            val clock = io.github.texport.superkassa.core.data.impl.adapter.DefaultClockAdapter()
            val timeValidator = io.github.texport.superkassa.core.data.impl.adapter.DefaultTimeValidatorAdapter()
            val delivery = io.github.texport.superkassa.delivery.impl.DefaultKtorDeliveryAdapter()
            val qrGen = io.github.texport.superkassa.receiptrenderer.impl.adapter.DefaultQrCodeGeneratorAdapter()

            // Рисовать документы этой сборке нечем. Прежде здесь отдавались
            // байты HTML под видом PDF и PNG: вызывающий сохранял «документ»,
            // который ничем не открывается. Отказ честнее; настоящий
            // конвертер даёт сборка core-embedded.
            val pdfConverter = object : DocumentConvertPort {
                override fun htmlToPdf(html: String): ByteArray = noConverter()
                override fun htmlToImage(html: String): ByteArray = noConverter()
                override fun htmlToEscPos(html: String, paperWidthMm: Int): ByteArray = noConverter()
            }

            val coreSettings = object : CoreSettingsRepositoryPort {
                private var current: CoreSettings? = null
                override fun load(): CoreSettings? = current
                override fun save(settings: CoreSettings): Boolean {
                    current = settings
                    return true
                }
                override fun loadOrCreate(defaults: CoreSettings): CoreSettings {
                    return current ?: defaults.also { current = it }
                }
            }

            val engine = SuperkassaCoreEngine(
                storage = roomStoragePair.storagePort,
                pinAttempts = roomStoragePair.pinAttempts,
                settings = coreSettings,
                delivery = delivery,
                clock = clock,
                timeValidator = timeValidator,
                qrCode = qrGen,
                pdfConverter = pdfConverter
            )

            return engine.buildApi(ofdProviderId = ofdProviderId)
        }
    }
}

