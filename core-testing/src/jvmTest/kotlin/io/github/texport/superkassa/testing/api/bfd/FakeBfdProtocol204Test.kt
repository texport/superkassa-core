package io.github.texport.superkassa.testing.api.bfd

import io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus
import io.github.texport.superkassa.testing.api.clock.MovableClock
import io.github.texport.superkassa.testing.api.kassa.BenchDirectory
import io.github.texport.superkassa.testing.api.kassa.BenchDirectory.Companion.NOT_PAYER
import io.github.texport.superkassa.testing.api.kassa.KassaSetup
import io.github.texport.superkassa.testing.api.kassa.appSuperkassaConfig
import kotlinx.coroutines.runBlocking
import kz.mybrain.network.OfdEndpoint
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Стенд на конфигурации приложения: касса говорит по CPCR 2.0.4 и заведена
 * у провайдера БФД. Тестовый БФД разбирает её запросы по 2.0.4 и отвечает
 * по 2.0.4 — проверки приложения идут на его настоящей конфигурации.
 */
class FakeBfdProtocol204Test {
    private val directory = BenchDirectory()
    private val bfd = FakeBfd()
    private val bench = directory.reopen(bfd, MovableClock(), appSuperkassaConfig())

    @AfterTest
    fun tearDown() {
        bench.close()
        directory.close()
    }

    @Test
    fun `касса на 2_0_4 заводится, продаёт, снимает X и закрывает смену`() {
        val kassa = bench.registerKassa(NOT_PAYER.copy(ofdProvider = KassaSetup.BFD)).also { it.openShift() }

        val statuses = listOf(kassa.sell().deliveryStatus, kassa.xReport().deliveryStatus, kassa.closeShift().deliveryStatus)

        assertEquals(List(3) { DeliveryStatus.ONLINE_OK }, statuses)
        assertEquals(setOf(204), bfd.exchanges.map { it.version }.toSet())
        assertEquals(1, bfd.countedTickets().size)
    }

    @Test
    fun `запрос версии, которую БФД не обслуживает, не разбирается`() {
        val header = ByteArray(HEADER_SIZE).also { it[VERSION_OFFSET] = UNSERVED_VERSION.toByte() }

        assertFailsWith<IllegalArgumentException> {
            runBlocking { bfd.sendAndReceive(OfdEndpoint("bfd", 1), header) }
        }
    }

    private companion object {
        const val HEADER_SIZE = 18
        const val VERSION_OFFSET = 2
        const val UNSERVED_VERSION = 202
    }
}
