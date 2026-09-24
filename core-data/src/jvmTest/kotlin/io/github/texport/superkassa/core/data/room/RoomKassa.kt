package io.github.texport.superkassa.core.data.room

import io.github.texport.superkassa.core.data.api.SuperkassaCoreEngine
import io.github.texport.superkassa.core.data.impl.adapter.DefaultClockAdapter
import io.github.texport.superkassa.core.data.impl.adapter.security.Base64TokenCodecAdapter
import io.github.texport.superkassa.core.data.impl.adapter.security.Sha256PinHasherAdapter
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.domain.api.model.common.TaxRegime
import io.github.texport.superkassa.core.domain.api.model.common.VatGroup
import io.github.texport.superkassa.core.domain.api.model.common.TimeValidationResult
import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmMode
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmState
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdServiceInfo
import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import io.github.texport.superkassa.core.domain.api.model.settings.DeliverySettings
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.integration.CoreSettingsRepositoryPort
import io.github.texport.superkassa.core.domain.api.port.integration.DocumentConvertPort
import io.github.texport.superkassa.core.domain.api.port.integration.TimeValidatorPort
import io.github.texport.superkassa.core.presentation.api.DeliveryApi
import io.github.texport.superkassa.core.presentation.api.SuperkassaApi
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptItemRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptPaymentRequest
import io.github.texport.superkassa.coredatabase.api.RoomStorage
import io.github.texport.superkassa.coredatabase.api.openInMemoryRoomStorage
import io.github.texport.superkassa.receiptrenderer.impl.adapter.DefaultQrCodeGeneratorAdapter
import io.github.texport.superkassa.testing.api.bfd.FakeBfd

/**
 * Касса на ядре с Room и тестовым БФД: сборка та же, что в приложении,
 * база — Room в памяти, сеть — [FakeBfd], доставка покупателю — [DeliveryLog].
 * Касса зарегистрирована, у неё администратор и кассир. Налоговый режим
 * и ставка кассы — те, что переданы; по умолчанию касса не плательщик НДС.
 *
 * База и часы подменяются: файловая база переживает «перезапуск» —
 * вторую кассу на том же файле, — а часы переводятся без ожидания.
 */
internal class RoomKassa(
    private val taxRegime: TaxRegime = TaxRegime.NO_VAT,
    private val kassaVat: VatGroup = VatGroup.NO_VAT,
    private val room: RoomStorage = openInMemoryRoomStorage(),
    clock: ClockPort = DefaultClockAdapter(),
    /** Доставка чека покупателю; по умолчанию не настроена. */
    receiptDelivery: DeliverySettings? = null
) {
    val bfd = FakeBfd(TOKEN)
    val deliveries = DeliveryLog()
    val storage = CountingStorage(room.storagePort)
    private val engine = SuperkassaCoreEngine(
        storage = storage,
        pinAttempts = room.pinAttempts,
        settings = MemorySettings(receiptDelivery),
        delivery = deliveries,
        clock = clock,
        timeValidator = TrustedClock,
        qrCode = DefaultQrCodeGeneratorAdapter(),
        pdfConverter = NoDocuments,
        ofdTransport = bfd
    )
    val api: SuperkassaApi = engine.buildApi()

    /** Доставка чека покупателю: досылка задач и их состояние. */
    val delivery: DeliveryApi = engine.buildDeliveryApi()

    init {
        register()
    }

    /** Документы открытой смены, от первого к последнему. */
    fun shiftDocuments(): List<FiscalDocumentSnapshot> {
        val shift = checkNotNull(storage.findOpenShift(KKM)) { "no open shift" }
        return storage.listFiscalDocumentsByShift(KKM, shift.id, limit = 100, offset = 0).sortedBy { it.createdAt }
    }

    fun document(id: String): FiscalDocumentSnapshot = checkNotNull(storage.findFiscalDocumentById(id)) { "no document $id" }

    /** Настройки кассы, как их правит администратор: через режим программирования. */
    fun settings(autoCloseShift: Boolean = false, autoCashout: Boolean = false) {
        api.enterProgramming(KKM, ADMIN_PIN)
        api.updateKkmSettings(KKM, ADMIN_PIN, autoCloseShift = autoCloseShift, autoCashout = autoCashout)
        api.exitProgramming(KKM, ADMIN_PIN)
    }

    /** Закрывает базу, как при остановке приложения. */
    fun close() = room.close()

    /** Касса, как она записана сейчас. */
    fun kkm(): KkmInfo = checkNotNull(storage.findKkm(KKM))

    /** Токен, с которым касса пойдёт в БФД. */
    fun token(): Long? = Base64TokenCodecAdapter().decodeToken(kkm().tokenEncryptedBase64)

    /** Задачи очереди досылки по документу. */
    fun queueTasks(documentId: String) =
        storage.listQueueTasksByCashbox(KKM, "OFFLINE", limit = 100, offset = 0).filter { it.payloadRef == documentId }

    private fun register() {
        val now = System.currentTimeMillis()
        storage.createKkm(
            KkmInfo(
                id = KKM, createdAt = now, updatedAt = now,
                mode = KkmMode.REGISTRATION.name, state = KkmState.ACTIVE.name,
                ofdProvider = "KAZAKHTELECOM:TEST", registrationNumber = KGD_NUMBER, factoryNumber = "KZT0000001",
                systemId = SYSTEM_ID.toString(), ofdServiceInfo = SERVICE_INFO,
                tokenEncryptedBase64 = Base64TokenCodecAdapter().encodeToken(TOKEN), tokenUpdatedAt = now,
                taxRegime = taxRegime, defaultVatGroup = kassaVat
            )
        )
        val pins = Sha256PinHasherAdapter()
        storage.createUser(KKM, "admin-1", "Айгерим", UserRole.ADMIN, pins.hash(ADMIN_PIN), now)
        storage.createUser(KKM, "cashier-1", "Нурлан", UserRole.CASHIER, pins.hash(CASHIER_PIN), now)
    }

    private class MemorySettings(private val delivery: DeliverySettings?) : CoreSettingsRepositoryPort {
        private var current: CoreSettings? = null
        override fun load(): CoreSettings? = current
        override fun save(settings: CoreSettings): Boolean {
            current = settings
            return true
        }
        override fun loadOrCreate(defaults: CoreSettings): CoreSettings =
            current ?: defaults.copy(delivery = delivery).also { current = it }
    }

    private object TrustedClock : TimeValidatorPort {
        override fun validate(clock: ClockPort) = TimeValidationResult(ok = true)
    }

    private object NoDocuments : DocumentConvertPort {
        override fun htmlToPdf(html: String): ByteArray = error("not used")
        override fun htmlToImage(html: String): ByteArray = error("not used")
        override fun htmlToEscPos(html: String, paperWidthMm: Int): ByteArray = error("not used")
    }

    companion object {
        const val KKM = "kkm-room-1"
        const val KGD_NUMBER = "010101012345"
        const val ADMIN_PIN = "8765"
        const val CASHIER_PIN = "4321"
        const val TOKEN = 123_456_789L

        /** Номер кассы в БФД. */
        const val SYSTEM_ID = 100_500L

        /** Больше интервала восстановления связи (не менее 60 с по протоколу). */

        private val SERVICE_INFO = OfdServiceInfo(
            orgTitle = "ТОО Дала", orgAddress = "Алматы", orgAddressKz = "Алматы", orgIinOrBin = "123456789012",
            orgOked = "47111", geoLatitude = 43_250_000, geoLongitude = 76_900_000, geoSource = "MANUAL"
        )

        /** Одна позиция на всю сумму и оплата наличными. */
        fun item(sum: String) = ReceiptItemRequest(name = "Нан", price = Decimal.parse(sum), quantity = Decimal.parse("1"))

        fun cash(sum: String) = ReceiptPaymentRequest(type = "CASH", sum = Decimal.parse(sum))
    }
}
