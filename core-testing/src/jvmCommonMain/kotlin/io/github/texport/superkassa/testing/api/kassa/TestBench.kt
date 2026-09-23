package io.github.texport.superkassa.testing.api.kassa

import io.github.texport.superkassa.core.domain.api.exception.SuperkassaException
import io.github.texport.superkassa.core.presentation.api.SuperkassaApi
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmListParams
import io.github.texport.superkassa.embedded.api.Superkassa
import io.github.texport.superkassa.embedded.api.SuperkassaConfig
import io.github.texport.superkassa.embedded.api.SuperkassaPlatform
import io.github.texport.superkassa.testing.api.bfd.FakeBfd
import io.github.texport.superkassa.testing.api.clock.MovableClock
import io.github.texport.superkassa.testing.impl.kassa.KassaRegistration

/**
 * Стенд проверки: настоящее ядро встраиваемой сборкой на каталоге данных,
 * БФД — [bfd], часы — [clock]. Внешних систем стенд не требует.
 *
 * Кассы заводятся через фасад, как у владельца, и остаются в каталоге:
 * стенд, закрытый и открытый заново на том же каталоге с тем же [bfd],
 * видит те же кассы, смены и документы.
 *
 * @property superkassa касса в процессе — её фасады отдаются проверяемому экрану.
 */
class TestBench private constructor(
    val superkassa: Superkassa,
    val bfd: FakeBfd,
    val clock: MovableClock
) : AutoCloseable {
    private val registration = KassaRegistration(superkassa.api, bfd.firstToken)

    /** Фасад кассовых операций. */
    val api: SuperkassaApi get() = superkassa.api

    /**
     * Заводит кассу: регистрация по номеру и токену БФД, налоговый режим,
     * название и кассир.
     *
     * @throws SuperkassaException если ядро отказало.
     */
    fun registerKassa(setup: KassaSetup): ReadyKassa {
        val kkm = registration.register(setup, setup.systemId ?: nextSystemId())
        return ReadyKassa(this, kkm.kkmId, checkNotNull(kkm.ofdSystemId).toLong(), setup.adminPin, setup.cashierPin)
    }

    /**
     * Заводит [count] касс по образцу [setup] — например, для списка входа.
     * Кассы называются «Касса 1», «Касса 2» … и получают свои номера в БФД.
     */
    fun registerKassas(count: Int, setup: KassaSetup): List<ReadyKassa> =
        (1..count).map { registerKassa(setup.copy(name = "Касса $it", systemId = null)) }

    /** Касса, заведённая раньше, например до перезапуска стенда на том же каталоге. */
    fun kassa(kkmId: String, adminPin: String, cashierPin: String): ReadyKassa =
        ReadyKassa(this, kkmId, checkNotNull(api.getKkm(kkmId).ofdSystemId).toLong(), adminPin, cashierPin)

    /** Останавливает фон кассы, закрывает базу и освобождает каталог. */
    override fun close() = superkassa.close()

    private fun nextSystemId(): Long = FIRST_SYSTEM_ID + api.listKkms(KkmListParams(limit = 1)).total

    companion object {
        /** Номер в БФД первой кассы, заведённой стендом. */
        const val FIRST_SYSTEM_ID = 100_001L

        /**
         * Открывает стенд на каталоге [platform].
         *
         * @param bfd БФД стенда; для перезапуска на том же каталоге передаётся прежний.
         * @param clock часы кассы.
         * @param config настройки кассы; по умолчанию [testSuperkassaConfig].
         * @throws IllegalStateException если каталог занят другим экземпляром.
         */
        fun open(
            platform: SuperkassaPlatform,
            bfd: FakeBfd = FakeBfd(),
            clock: MovableClock = MovableClock(),
            config: SuperkassaConfig = testSuperkassaConfig()
        ): TestBench = TestBench(createTestSuperkassa(platform, bfd, clock, config), bfd, clock)
    }
}
