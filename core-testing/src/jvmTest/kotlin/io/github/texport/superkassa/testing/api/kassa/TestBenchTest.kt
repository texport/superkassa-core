package io.github.texport.superkassa.testing.api.kassa

import io.github.texport.superkassa.core.domain.api.exception.SuperkassaException
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmListParams
import io.github.texport.superkassa.core.presentation.api.model.kkm.VatGroup
import io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus
import io.github.texport.superkassa.testing.api.kassa.BenchDirectory.Companion.ADMIN_PIN
import io.github.texport.superkassa.testing.api.kassa.BenchDirectory.Companion.CASHIER_PIN
import io.github.texport.superkassa.testing.api.kassa.BenchDirectory.Companion.NOT_PAYER
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Касса заводится на пустом каталоге тем же путём, что у владельца,
 * и остаётся в каталоге после перезапуска стенда.
 */
class TestBenchTest {
    private val directory = BenchDirectory()
    private var bench = directory.open()

    @AfterTest
    fun tearDown() {
        bench.close()
        directory.close()
    }

    @Test
    fun `касса заводится по ответу БФД с его регистрационными сведениями и токеном`() {
        val kassa = bench.registerKassa(NOT_PAYER.copy(name = "Касса у окна"))

        val info = kassa.info()

        assertEquals(TestBench.FIRST_SYSTEM_ID, kassa.systemId)
        assertEquals(bench.bfd.kgdNumber(kassa.systemId), info.kkmKgdId)
        assertEquals("ТОО Дала" to "Касса у окна", info.ofdServiceInfo?.orgTitle to info.name)
        assertEquals(true, info.isTokenValid)
        assertEquals("ADMIN", kassa.api.currentUser(kassa.kkmId, ADMIN_PIN).role.name)
    }

    @Test
    fun `пинов по умолчанию нет`() {
        val kassa = bench.registerKassa(NOT_PAYER)

        assertFailsWith<SuperkassaException> { kassa.api.authenticate(kassa.kkmId, "1234") }
    }

    @Test
    fun `налоговый режим кассы - тот, что задан`() {
        val modes = listOf(
            VatMode.NotPayer,
            VatMode.Payer(VatGroup.VAT_16),
            VatMode.Payer(VatGroup.NO_VAT),
            VatMode.Mixed(VatGroup.VAT_12)
        )

        val kassas = modes.map { bench.registerKassa(NOT_PAYER.copy(vat = it)).info() }

        assertEquals(
            listOf("NO_VAT" to "NO_VAT", "VAT_PAYER" to "VAT_16", "VAT_PAYER" to "NO_VAT", "MIXED" to "VAT_12"),
            kassas.map { it.taxRegime to it.defaultVatGroup }
        )
    }

    @Test
    fun `пятьдесят касс видны в списке входа`() {
        bench.registerKassas(LOGIN_LIST, NOT_PAYER)

        val list = bench.api.listKkms(KkmListParams(limit = LOGIN_LIST))

        assertEquals(LOGIN_LIST, list.total)
        assertEquals(LOGIN_LIST, list.items.mapNotNull { it.name }.toSet().size)
    }

    @Test
    fun `после перезапуска на том же каталоге касса продолжает смену`() {
        val before = bench.registerKassa(NOT_PAYER).also { it.openShift() }
        before.sell()
        bench.close()

        bench = directory.reopen(before.bfd)
        val after = bench.kassa(before.kkmId, ADMIN_PIN, CASHIER_PIN)

        assertEquals(DeliveryStatus.ONLINE_OK, after.sell().deliveryStatus)
        assertEquals(before.systemId, after.systemId)
        assertEquals(2, bench.bfd.countedTickets().size)
    }

    private companion object {
        const val LOGIN_LIST = 50
    }
}
