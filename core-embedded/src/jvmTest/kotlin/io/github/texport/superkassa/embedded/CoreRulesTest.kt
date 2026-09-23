package io.github.texport.superkassa.embedded

import io.github.texport.superkassa.core.domain.api.exception.SuperkassaException
import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmState
import io.github.texport.superkassa.core.presentation.api.model.kkm.CashOperationRequest
import io.github.texport.superkassa.core.presentation.api.model.ofd.NomenclatureLookupRequest
import io.github.texport.superkassa.embedded.TestCashRegister.ADMIN_PIN
import io.github.texport.superkassa.embedded.TestCashRegister.CASHIER_PIN
import io.github.texport.superkassa.embedded.TestCashRegister.KKM_ID
import io.github.texport.superkassa.embedded.impl.EmbeddedSuperkassa
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Правила, которые прежде проверял только узел, работают и в кассе приложения:
 * границы сумм, пустое внесение и справочник у заблокированной кассы.
 */
class CoreRulesTest {
    private val dir: File = createTempDirectory("kassa-rules-").toFile()

    @AfterTest
    fun cleanUp() {
        dir.deleteRecursively()
    }

    @Test
    fun `скидка больше ста процентов отвергается до пробития`() = withShift { kassa ->
        val sale = TestCashRegister.sale("sale-1").copy(discountPercent = Decimal.parse("150"))

        val refusal = assertFailsWith<SuperkassaException> { kassa.api.createSellReceipt(KKM_ID, CASHIER_PIN, sale) }

        assertEquals("RECEIPT_VALUE_OUT_OF_RANGE", refusal.code)
    }

    @Test
    fun `позиция по нулевой цене отвергается`() = withShift { kassa ->
        val base = TestCashRegister.sale("sale-1")
        val sale = base.copy(items = base.items.map { it.copy(price = Decimal.ZERO) })

        val refusal = assertFailsWith<SuperkassaException> { kassa.api.createSellReceipt(KKM_ID, CASHIER_PIN, sale) }

        assertEquals("RECEIPT_VALUE_OUT_OF_RANGE", refusal.code)
    }

    @Test
    fun `внесение на ноль тенге отвергается`() = withShift { kassa ->
        val refusal = assertFailsWith<SuperkassaException> {
            kassa.api.cashIn(KKM_ID, CASHIER_PIN, CashOperationRequest(amount = Decimal.ZERO, idempotencyKey = "in-1"))
        }

        assertEquals("CASH_SUM_TOO_SMALL", refusal.code)
    }

    @Test
    fun `заблокированная касса не спрашивает справочник`() = withShift { kassa ->
        val kkm = checkNotNull(kassa.storage.findKkm(KKM_ID))
        kassa.storage.updateKkm(kkm.copy(state = KkmState.BLOCKED.name))

        val refusal = assertFailsWith<SuperkassaException> {
            kassa.api.lookupNomenclature(CASHIER_PIN, NomenclatureLookupRequest(KKM_ID, "4870000000017"))
        }

        assertEquals("KKM_BLOCKED", refusal.code)
    }

    private fun withShift(block: (EmbeddedSuperkassa) -> Unit) {
        TestCashRegister.open(dir).use { kassa ->
            TestCashRegister.registerKkm(kassa)
            kassa.api.openShift(KKM_ID, ADMIN_PIN)
            block(kassa)
        }
    }
}
