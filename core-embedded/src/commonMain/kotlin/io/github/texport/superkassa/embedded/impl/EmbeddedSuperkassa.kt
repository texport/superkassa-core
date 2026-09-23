package io.github.texport.superkassa.embedded.impl

import io.github.texport.superkassa.core.data.api.SuperkassaCoreEngine
import io.github.texport.superkassa.core.data.api.systemClock
import io.github.texport.superkassa.core.data.api.systemTimeGuard
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.api.port.integration.TimeValidatorPort
import io.github.texport.superkassa.core.presentation.api.DeliveryApi
import io.github.texport.superkassa.core.presentation.api.OfflineQueueApi
import io.github.texport.superkassa.core.presentation.api.PrintApi
import io.github.texport.superkassa.core.presentation.api.SettingsApi
import io.github.texport.superkassa.core.presentation.api.SuperkassaApi
import io.github.texport.superkassa.coredatabase.api.RoomStorage
import io.github.texport.superkassa.coredatabase.api.openRoomStorage
import io.github.texport.superkassa.embedded.api.DocumentPrinter
import io.github.texport.superkassa.embedded.api.Superkassa
import io.github.texport.superkassa.embedded.api.SuperkassaConfig
import io.github.texport.superkassa.embedded.api.SuperkassaPlatform
import io.github.texport.superkassa.embedded.impl.delivery.EmbeddedDelivery
import io.github.texport.superkassa.embedded.impl.queue.QueueSender
import io.github.texport.superkassa.embedded.impl.queue.queueDispatcher
import io.github.texport.superkassa.embedded.impl.settings.FileCoreSettings
import io.github.texport.superkassa.embedded.impl.shift.ShiftAutoCloser
import io.github.texport.superkassa.embedded.impl.storage.DataFiles
import io.github.texport.superkassa.embedded.impl.storage.LocalFiles
import io.github.texport.superkassa.receiptrenderer.impl.adapter.DefaultQrCodeGeneratorAdapter
import kz.mybrain.network.OfdNetworkClient

/**
 * Сборка кассы в процессе приложения — единственная точка входа модуля.
 *
 * Порядок открытия: замок каталога, проверка «настройки есть — база есть»,
 * база, проверка часов, ядро, досылка очереди и автозакрытие смен. Любой отказ по дороге
 * закрывает уже открытое и уходит наружу с причиной.
 */
internal class EmbeddedSuperkassa private constructor(
    private val lock: AutoCloseable,
    private val database: RoomStorage,
    private val sender: QueueSender,
    private val shiftCloser: ShiftAutoCloser,
    override val api: SuperkassaApi,
    override val delivery: DeliveryApi,
    override val settings: SettingsApi,
    override val printer: DocumentPrinter
) : Superkassa {
    override val print: PrintApi get() = api
    override val queue: OfflineQueueApi get() = api.queue

    /** Хранилище ядра: для проверок сборки, приложению оно не нужно. */
    internal val storage: StoragePort get() = database.storagePort

    /** Досылка очереди одним заходом, не дожидаясь паузы: для проверок сборки. */
    internal fun sendQueueNow(): Int = sender.sendOnce()

    /** Проверка автозакрытия одним заходом, не дожидаясь паузы: для проверок сборки. */
    internal fun closeDueShiftsNow(): Int = shiftCloser.closeDueOnce()

    private var closed = false

    override fun close() {
        if (closed) return
        closed = true
        try {
            shiftCloser.stop()
            sender.stop()
            database.close()
        } finally {
            lock.close()
        }
    }

    companion object {
        /**
         * Открывает кассу.
         *
         * @param ofdTransport транспорт до ОФД; `null` — настоящий TCP-клиент.
         * @param timeGuard проверка часов; по умолчанию та же, что у узла.
         * @param clock часы кассы; по умолчанию системные.
         */
        internal fun open(
            platform: SuperkassaPlatform,
            config: SuperkassaConfig,
            ofdTransport: OfdNetworkClient?,
            timeGuard: TimeValidatorPort = systemTimeGuard(),
            clock: ClockPort = systemClock()
        ): EmbeddedSuperkassa {
            val dir = platform.dataDir
            LocalFiles.ensureDirectory(dir)
            val lock = LocalFiles.lock("$dir/${DataFiles.LOCK}")
            var database: RoomStorage? = null
            var opened = false
            try {
                requireDatabaseWhereSettingsAre(dir)
                database = openRoomStorage(platform.databaseBuilder("$dir/${DataFiles.DATABASE}"))
                return assemble(platform, config, Parts(lock, database, ofdTransport, timeGuard, clock)).also { opened = true }
            } finally {
                // Сорвалось по дороге — закрывается уже открытое: база и замок каталога.
                if (!opened) {
                    database?.close()
                    lock.close()
                }
            }
        }

        private fun assemble(platform: SuperkassaPlatform, config: SuperkassaConfig, parts: Parts): EmbeddedSuperkassa {
            val time = parts.timeGuard.validate(parts.clock)
            check(time.ok) { "System time is not valid: ${time.reason}" }
            val engine = engine(platform, config, parts, parts.clock)
            val api = engine.buildApi(config.ownerId, config.ofdProviderId, config.ofdProtocolVersion)
            val storage = parts.database.storagePort
            val sender = QueueSender(storage, api.queue, config.queueInterval, config.queueBatchSize, queueDispatcher)
            val shiftCloser = ShiftAutoCloser(storage, api, config.shiftCheckInterval, queueDispatcher)
            val kassa =
                EmbeddedSuperkassa(
                    parts.lock,
                    parts.database,
                    sender,
                    shiftCloser,
                    api,
                    engine.buildDeliveryApi(),
                    engine.buildSettingsApi(config.ofdProviderId, config.ofdProtocolVersion),
                    platform.printer()
                )
            // Досылка и автозакрытие запускаются последними: до этого сборка ещё может сорваться.
            sender.start()
            shiftCloser.start()
            return kassa
        }

        private fun engine(platform: SuperkassaPlatform, config: SuperkassaConfig, parts: Parts, clock: ClockPort) =
            FileCoreSettings("${platform.dataDir}/${DataFiles.SETTINGS}").let { settings ->
                SuperkassaCoreEngine(
                    storage = parts.database.storagePort,
                    settings = settings,
                    delivery = EmbeddedDelivery(config.channels, settings::load),
                    clock = clock,
                    timeValidator = parts.timeGuard,
                    qrCode = DefaultQrCodeGeneratorAdapter(),
                    pdfConverter = platform.documents(),
                    ofdTransport = parts.ofdTransport
                )
            }

        /**
         * Настройки на месте, а базы нет — значит каталог перенесён или указан
         * не тот. Пустая база здесь спрятала бы потерю смен и чеков.
         */
        private fun requireDatabaseWhereSettingsAre(dir: String) {
            val settingsFound = LocalFiles.exists("$dir/${DataFiles.SETTINGS}")
            check(!settingsFound || LocalFiles.exists("$dir/${DataFiles.DATABASE}")) {
                "Database ${DataFiles.DATABASE} is missing in $dir, although ${DataFiles.SETTINGS} is there. " +
                    "Restore the database or remove ${DataFiles.SETTINGS} to start an empty cash register deliberately."
            }
        }
    }

    private class Parts(
        val lock: AutoCloseable,
        val database: RoomStorage,
        val ofdTransport: OfdNetworkClient?,
        val timeGuard: TimeValidatorPort,
        val clock: ClockPort
    )
}
