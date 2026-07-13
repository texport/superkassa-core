package io.github.texport.superkassa.core.domain.impl.usecase.counter

import kotlin.test.Test
import kotlin.test.assertEquals
import io.github.texport.superkassa.core.domain.api.model.auth.KkmUser
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.model.common.CounterScopes
import io.github.texport.superkassa.core.domain.api.model.common.CounterSnapshot
import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.common.TaxRegime
import io.github.texport.superkassa.core.domain.api.model.common.VatGroup
import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.queue.QueueTask
import io.github.texport.superkassa.core.domain.api.model.receipt.PaymentType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptItem
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptPayment
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftStatus
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort


class UpdateCountersUseCaseTest {
    @Test
    fun shouldUpdateShiftAndGlobalCounters() {
        val storage = InMemoryStoragePort()
        val updater = UpdateCountersUseCase(storage)
        val request = ReceiptRequest(
            kkmId = "kkm-1",
            pin = "1111",
            operation = ReceiptOperationType.SELL,
            items = listOf(ReceiptItem("Item", "1", 1, Money(1000, 0), Money(1000, 0))),
            payments = listOf(ReceiptPayment(PaymentType.CASH, Money(1000, 0))),
            total = Money(1000, 0),
            idempotencyKey = "key-1"
        )

        updater.execute("kkm-1", "shift-1", request, isOffline = false)

        val shift = storage.loadCounters("kkm-1", CounterScopes.SHIFT, "shift-1")
        val global = storage.loadCounters("kkm-1", CounterScopes.GLOBAL, null)

        assertEquals(1L, shift["operation.OPERATION_SELL.count"])
        assertEquals(1000L, shift["operation.OPERATION_SELL.sum"])
        assertEquals(1L, global["operation.OPERATION_SELL.count"])
        assertEquals(1000L, global["operation.OPERATION_SELL.sum"])
    }

    @Test
    fun testUpdateCountersSellReturnCard() {
        val storage = InMemoryStoragePort()
        val updater = UpdateCountersUseCase(storage)
        val request = ReceiptRequest(
            kkmId = "kkm-1",
            pin = "1111",
            operation = ReceiptOperationType.SELL_RETURN,
            items = listOf(ReceiptItem("Item 1", "1", 1, Money(500, 0), Money(500, 0))),
            payments = listOf(ReceiptPayment(PaymentType.CARD, Money(500, 0))),
            total = Money(500, 0),
            idempotencyKey = "key-2"
        )

        updater.execute("kkm-1", "shift-1", request, isOffline = true)

        val shift = storage.loadCounters("kkm-1", CounterScopes.SHIFT, "shift-1")
        val global = storage.loadCounters("kkm-1", CounterScopes.GLOBAL, null)

        assertEquals(1L, shift["operation.OPERATION_SELL_RETURN.count"])
        assertEquals(500L, shift["operation.OPERATION_SELL_RETURN.sum"])
        assertEquals(1L, shift["ticket.OPERATION_SELL_RETURN.payment.PAYMENT_CARD.count"])
        assertEquals(500L, shift["ticket.OPERATION_SELL_RETURN.payment.PAYMENT_CARD.sum"])
        assertEquals(1L, shift["ticket.OPERATION_SELL_RETURN.offline_count"])
        
        // Revenue delta is negative for return operations
        assertEquals(-500L, shift["revenue.sum"])
        assertEquals(1L, shift["revenue.is_negative"])
        assertEquals(-500L, global["revenue.sum"])
        assertEquals(1L, global["revenue.is_negative"])
    }

    @Test
    fun testUpdateCountersBuyMobile() {
        val storage = InMemoryStoragePort()
        val updater = UpdateCountersUseCase(storage)
        val request = ReceiptRequest(
            kkmId = "kkm-1",
            pin = "1111",
            operation = ReceiptOperationType.BUY,
            items = listOf(ReceiptItem("Item 1", "1", 1, Money(1200, 0), Money(1200, 0))),
            payments = listOf(ReceiptPayment(PaymentType.MOBILE, Money(1200, 0))),
            total = Money(1200, 0),
            idempotencyKey = "key-3"
        )

        updater.execute("kkm-1", "shift-1", request, isOffline = false)

        val shift = storage.loadCounters("kkm-1", CounterScopes.SHIFT, "shift-1")
        assertEquals(1L, shift["operation.OPERATION_BUY.count"])
        assertEquals(1200L, shift["operation.OPERATION_BUY.sum"])
        assertEquals(1L, shift["ticket.OPERATION_BUY.payment.PAYMENT_MOBILE.count"])
        assertEquals(1200L, shift["ticket.OPERATION_BUY.payment.PAYMENT_MOBILE.sum"])
        assertEquals(1200L, shift["revenue.sum"])
        assertEquals(0L, shift["revenue.is_negative"])
    }

    @Test
    fun testUpdateCountersBuyReturnElectronic() {
        val storage = InMemoryStoragePort()
        val updater = UpdateCountersUseCase(storage)
        val request = ReceiptRequest(
            kkmId = "kkm-1",
            pin = "1111",
            operation = ReceiptOperationType.BUY_RETURN,
            items = listOf(ReceiptItem("Item 1", "1", 1, Money(300, 0), Money(300, 0))),
            payments = listOf(ReceiptPayment(PaymentType.ELECTRONIC, Money(300, 0))),
            total = Money(300, 0),
            idempotencyKey = "key-4"
        )

        updater.execute("kkm-1", "shift-1", request, isOffline = false)

        val shift = storage.loadCounters("kkm-1", CounterScopes.SHIFT, "shift-1")
        assertEquals(1L, shift["operation.OPERATION_BUY_RETURN.count"])
        assertEquals(300L, shift["operation.OPERATION_BUY_RETURN.sum"])
        assertEquals(1L, shift["ticket.OPERATION_BUY_RETURN.payment.PAYMENT_ELECTRONIC.count"])
        assertEquals(300L, shift["ticket.OPERATION_BUY_RETURN.payment.PAYMENT_ELECTRONIC.sum"])
        assertEquals(-300L, shift["revenue.sum"])
        assertEquals(1L, shift["revenue.is_negative"])
    }

    @Test
    fun testUpdateCountersMultipleItemsVariousTaxesAndPayments() {
        val storage = InMemoryStoragePort()
        val updater = UpdateCountersUseCase(storage)
        
        // Items with different VAT groups
        val items = listOf(
            ReceiptItem("Item 1", "001", 1, Money(1160, 0), Money(1160, 0), vatGroup = VatGroup.VAT_16), // 16% -> base = 1000, tax = 160
            ReceiptItem("Item 2", "002", 1, Money(2000, 0), Money(2000, 0), vatGroup = VatGroup.VAT_0),  // 0% -> base = 2000, tax = 0
            ReceiptItem("Item 3", "003", 1, Money(1500, 0), Money(1500, 0), vatGroup = VatGroup.NO_VAT) // NO_VAT
        )
        
        val payments = listOf(
            ReceiptPayment(PaymentType.CASH, Money(3160, 0)),
            ReceiptPayment(PaymentType.CARD, Money(1500, 0))
        )
        
        val request = ReceiptRequest(
            kkmId = "kkm-1",
            pin = "1111",
            operation = ReceiptOperationType.SELL,
            items = items,
            payments = payments,
            total = Money(4660, 0),
            idempotencyKey = "key-5",
            change = Money(500, 0), // 500 change
            discount = Money(100, 0),
            markup = Money(50, 0),
            taxRegime = io.github.texport.superkassa.core.domain.api.model.common.TaxRegime.MIXED
        )

        updater.execute("kkm-1", "shift-1", request, isOffline = true)

        val shift = storage.loadCounters("kkm-1", CounterScopes.SHIFT, "shift-1")
        val global = storage.loadCounters("kkm-1", CounterScopes.GLOBAL, null)

        // General operations
        assertEquals(1L, shift["operation.OPERATION_SELL.count"])
        assertEquals(4660L, shift["operation.OPERATION_SELL.sum"])
        assertEquals(100L, shift["operation.OPERATION_SELL.discount_sum"])
        assertEquals(50L, shift["operation.OPERATION_SELL.markup_sum"])

        // Ticket operations
        assertEquals(1L, shift["ticket.OPERATION_SELL.total_count"])
        assertEquals(1L, shift["ticket.OPERATION_SELL.count"])
        assertEquals(4660L, shift["ticket.OPERATION_SELL.sum"])
        assertEquals(100L, shift["ticket.OPERATION_SELL.discount_sum"])
        assertEquals(50L, shift["ticket.OPERATION_SELL.markup_sum"])
        assertEquals(500L, shift["ticket.OPERATION_SELL.change_sum"])

        // Section operations
        assertEquals(1L, shift["section.001.operation.OPERATION_SELL.count"])
        assertEquals(1160L, shift["section.001.operation.OPERATION_SELL.sum"])
        assertEquals(1L, shift["section.002.operation.OPERATION_SELL.count"])
        assertEquals(2000L, shift["section.002.operation.OPERATION_SELL.sum"])
        assertEquals(1L, shift["section.003.operation.OPERATION_SELL.count"])
        assertEquals(1500L, shift["section.003.operation.OPERATION_SELL.sum"])

        // Cash flow sum
        assertEquals(3160L, shift["cash.sum"])
        assertEquals(3160L, global["cash.sum"])

        // Tax counters
        // VAT_16 turnovers: base = 1000, tax = 160, base without tax = 1000
        assertEquals(1000L, shift["tax.VAT_16.OPERATION_SELL.turnover"])
        assertEquals(160L, shift["tax.VAT_16.OPERATION_SELL.sum"])
        assertEquals(1000L, shift["tax.VAT_16.OPERATION_SELL.turnover_without_tax"])
    }

    private fun runAndVerifyScenario(
        id: Int,
        op: ReceiptOperationType,
        totalAmount: Long,
        paymentType: PaymentType,
        isOffline: Boolean,
        sectionCode: String
    ) {
        val storage = InMemoryStoragePort()
        val updater = UpdateCountersUseCase(storage)
        val vatGroup = when (id % 3) {
            0 -> VatGroup.VAT_16
            1 -> VatGroup.VAT_5
            else -> null
        }
        val taxRegime = if (vatGroup != null) TaxRegime.MIXED else TaxRegime.NO_VAT
        val request = ReceiptRequest(
            kkmId = "kkm-1", pin = "1111", operation = op,
            items = listOf(ReceiptItem("Item-$id", sectionCode, 1, Money(totalAmount, 0), Money(totalAmount, 0), vatGroup = vatGroup)),
            payments = listOf(ReceiptPayment(paymentType, Money(totalAmount, 0))),
            total = Money(totalAmount, 0), idempotencyKey = "k-$id",
            taxRegime = taxRegime, defaultVatGroup = vatGroup
        )
        updater.execute("kkm-1", "shift-1", request, isOffline)

        val shift = storage.loadCounters("kkm-1", CounterScopes.SHIFT, "shift-1")
        val global = storage.loadCounters("kkm-1", CounterScopes.GLOBAL, null)

        val opKey = when(op) {
            ReceiptOperationType.SELL -> "OPERATION_SELL"
            ReceiptOperationType.SELL_RETURN -> "OPERATION_SELL_RETURN"
            ReceiptOperationType.BUY -> "OPERATION_BUY"
            ReceiptOperationType.BUY_RETURN -> "OPERATION_BUY_RETURN"
        }
        val payKey = when (paymentType) {
            PaymentType.CASH -> "PAYMENT_CASH"
            PaymentType.CARD -> "PAYMENT_CARD"
            PaymentType.ELECTRONIC -> "PAYMENT_ELECTRONIC"
            PaymentType.MOBILE -> "PAYMENT_MOBILE"
        }

        val expectedShift = mutableMapOf<String, Long>()
        expectedShift["operation.$opKey.count"] = 1L
        expectedShift["operation.$opKey.sum"] = totalAmount
        expectedShift["operation.$opKey.discount_sum"] = 0L
        expectedShift["operation.$opKey.markup_sum"] = 0L
        expectedShift["section.$sectionCode.operation.$opKey.count"] = 1L
        expectedShift["section.$sectionCode.operation.$opKey.sum"] = totalAmount
        expectedShift["ticket.$opKey.total_count"] = 1L
        expectedShift["ticket.$opKey.count"] = 1L
        expectedShift["ticket.$opKey.sum"] = totalAmount
        expectedShift["ticket.$opKey.discount_sum"] = 0L
        expectedShift["ticket.$opKey.markup_sum"] = 0L
        expectedShift["ticket.$opKey.change_sum"] = 0L
        if (isOffline) {
            expectedShift["ticket.$opKey.offline_count"] = 1L
        }
        expectedShift["non_nullable.$opKey.sum"] = totalAmount
        expectedShift["ticket.$opKey.payment.$payKey.sum"] = totalAmount
        expectedShift["ticket.$opKey.payment.$payKey.count"] = 1L
        if (paymentType == PaymentType.CASH) {
            expectedShift["cash.sum"] = totalAmount
        }
        val revSum = when(op) {
            ReceiptOperationType.SELL, ReceiptOperationType.BUY -> totalAmount
            ReceiptOperationType.SELL_RETURN, ReceiptOperationType.BUY_RETURN -> -totalAmount
        }
        expectedShift["revenue.sum"] = revSum
        expectedShift["revenue.is_negative"] = if (revSum < 0) 1L else 0L

        if (vatGroup != null) {
            val percent = vatGroup.percent
            val vatAmountDouble = totalAmount - totalAmount / (1.0 + percent / 100.0)
            val baseAmountDouble = totalAmount - vatAmountDouble
            val vatSum = Money.fromTenge(vatAmountDouble).bills
            val taxBase = Money.fromTenge(baseAmountDouble).bills
            val taxKey = vatGroup.name
            expectedShift["tax.$taxKey.$opKey.turnover"] = taxBase
            expectedShift["tax.$taxKey.$opKey.sum"] = vatSum
            expectedShift["tax.$taxKey.$opKey.turnover_without_tax"] = taxBase
        }

        val expectedGlobal = expectedShift.toMutableMap()
        if (vatGroup != null) {
            val taxKey = vatGroup.name
            expectedGlobal.remove("tax.$taxKey.$opKey.turnover")
            expectedGlobal.remove("tax.$taxKey.$opKey.sum")
            expectedGlobal.remove("tax.$taxKey.$opKey.turnover_without_tax")
        }

        assertEquals(expectedShift, shift)
        assertEquals(expectedGlobal, global)
    }

    @Test fun testScenario1() = runAndVerifyScenario(1, ReceiptOperationType.SELL, 100L, PaymentType.CASH, false, "001")
    @Test fun testScenario2() = runAndVerifyScenario(2, ReceiptOperationType.SELL, 200L, PaymentType.CARD, true, "002")
    @Test fun testScenario3() = runAndVerifyScenario(3, ReceiptOperationType.SELL, 300L, PaymentType.ELECTRONIC, false, "003")
    @Test fun testScenario4() = runAndVerifyScenario(4, ReceiptOperationType.SELL, 400L, PaymentType.MOBILE, true, "004")
    @Test fun testScenario5() = runAndVerifyScenario(5, ReceiptOperationType.SELL, 500L, PaymentType.CASH, true, "005")
    @Test fun testScenario6() = runAndVerifyScenario(6, ReceiptOperationType.SELL, 600L, PaymentType.CARD, false, "006")
    @Test fun testScenario7() = runAndVerifyScenario(7, ReceiptOperationType.SELL, 700L, PaymentType.ELECTRONIC, true, "007")
    @Test fun testScenario8() = runAndVerifyScenario(8, ReceiptOperationType.SELL, 800L, PaymentType.MOBILE, false, "008")
    @Test fun testScenario9() = runAndVerifyScenario(9, ReceiptOperationType.SELL, 900L, PaymentType.CASH, false, "009")
    @Test fun testScenario10() = runAndVerifyScenario(10, ReceiptOperationType.SELL, 1000L, PaymentType.CARD, true, "010")
    @Test fun testScenario11() = runAndVerifyScenario(11, ReceiptOperationType.SELL, 1100L, PaymentType.ELECTRONIC, false, "011")
    @Test fun testScenario12() = runAndVerifyScenario(12, ReceiptOperationType.SELL, 1200L, PaymentType.MOBILE, true, "012")

    @Test fun testScenario13() = runAndVerifyScenario(13, ReceiptOperationType.SELL_RETURN, 1300L, PaymentType.CASH, false, "013")
    @Test fun testScenario14() = runAndVerifyScenario(14, ReceiptOperationType.SELL_RETURN, 1400L, PaymentType.CARD, true, "014")
    @Test fun testScenario15() = runAndVerifyScenario(15, ReceiptOperationType.SELL_RETURN, 1500L, PaymentType.ELECTRONIC, false, "015")
    @Test fun testScenario16() = runAndVerifyScenario(16, ReceiptOperationType.SELL_RETURN, 1600L, PaymentType.MOBILE, true, "016")
    @Test fun testScenario17() = runAndVerifyScenario(17, ReceiptOperationType.SELL_RETURN, 1700L, PaymentType.CASH, true, "017")
    @Test fun testScenario18() = runAndVerifyScenario(18, ReceiptOperationType.SELL_RETURN, 1800L, PaymentType.CARD, false, "018")
    @Test fun testScenario19() = runAndVerifyScenario(19, ReceiptOperationType.SELL_RETURN, 1900L, PaymentType.ELECTRONIC, true, "019")
    @Test fun testScenario20() = runAndVerifyScenario(20, ReceiptOperationType.SELL_RETURN, 2000L, PaymentType.MOBILE, false, "020")
    @Test fun testScenario21() = runAndVerifyScenario(21, ReceiptOperationType.SELL_RETURN, 2100L, PaymentType.CASH, false, "021")
    @Test fun testScenario22() = runAndVerifyScenario(22, ReceiptOperationType.SELL_RETURN, 2200L, PaymentType.CARD, true, "022")
    @Test fun testScenario23() = runAndVerifyScenario(23, ReceiptOperationType.SELL_RETURN, 2300L, PaymentType.ELECTRONIC, false, "023")
    @Test fun testScenario24() = runAndVerifyScenario(24, ReceiptOperationType.SELL_RETURN, 2400L, PaymentType.MOBILE, true, "024")

    @Test fun testScenario25() = runAndVerifyScenario(25, ReceiptOperationType.BUY, 2500L, PaymentType.CASH, false, "025")
    @Test fun testScenario26() = runAndVerifyScenario(26, ReceiptOperationType.BUY, 2600L, PaymentType.CARD, true, "026")
    @Test fun testScenario27() = runAndVerifyScenario(27, ReceiptOperationType.BUY, 2700L, PaymentType.ELECTRONIC, false, "027")
    @Test fun testScenario28() = runAndVerifyScenario(28, ReceiptOperationType.BUY, 2800L, PaymentType.MOBILE, true, "028")
    @Test fun testScenario29() = runAndVerifyScenario(29, ReceiptOperationType.BUY, 2900L, PaymentType.CASH, true, "029")
    @Test fun testScenario30() = runAndVerifyScenario(30, ReceiptOperationType.BUY, 3000L, PaymentType.CARD, false, "030")
    @Test fun testScenario31() = runAndVerifyScenario(31, ReceiptOperationType.BUY, 3100L, PaymentType.ELECTRONIC, true, "031")
    @Test fun testScenario32() = runAndVerifyScenario(32, ReceiptOperationType.BUY, 3200L, PaymentType.MOBILE, false, "032")
    @Test fun testScenario33() = runAndVerifyScenario(33, ReceiptOperationType.BUY, 3300L, PaymentType.CASH, false, "033")
    @Test fun testScenario34() = runAndVerifyScenario(34, ReceiptOperationType.BUY, 3400L, PaymentType.CARD, true, "034")
    @Test fun testScenario35() = runAndVerifyScenario(35, ReceiptOperationType.BUY, 3500L, PaymentType.ELECTRONIC, false, "035")
    @Test fun testScenario36() = runAndVerifyScenario(36, ReceiptOperationType.BUY, 3600L, PaymentType.MOBILE, true, "036")

    @Test fun testScenario37() = runAndVerifyScenario(37, ReceiptOperationType.BUY_RETURN, 3700L, PaymentType.CASH, false, "037")
    @Test fun testScenario38() = runAndVerifyScenario(38, ReceiptOperationType.BUY_RETURN, 3800L, PaymentType.CARD, true, "038")
    @Test fun testScenario39() = runAndVerifyScenario(39, ReceiptOperationType.BUY_RETURN, 3900L, PaymentType.ELECTRONIC, false, "039")
    @Test fun testScenario40() = runAndVerifyScenario(40, ReceiptOperationType.BUY_RETURN, 4000L, PaymentType.MOBILE, true, "040")
    @Test fun testScenario41() = runAndVerifyScenario(41, ReceiptOperationType.BUY_RETURN, 4100L, PaymentType.CASH, true, "041")
    @Test fun testScenario42() = runAndVerifyScenario(42, ReceiptOperationType.BUY_RETURN, 4200L, PaymentType.CARD, false, "042")
    @Test fun testScenario43() = runAndVerifyScenario(43, ReceiptOperationType.BUY_RETURN, 4300L, PaymentType.ELECTRONIC, true, "043")
    @Test fun testScenario44() = runAndVerifyScenario(44, ReceiptOperationType.BUY_RETURN, 4400L, PaymentType.MOBILE, false, "044")
    @Test fun testScenario45() = runAndVerifyScenario(45, ReceiptOperationType.BUY_RETURN, 4500L, PaymentType.CASH, false, "045")
    @Test fun testScenario46() = runAndVerifyScenario(46, ReceiptOperationType.BUY_RETURN, 4600L, PaymentType.CARD, true, "046")
    @Test fun testScenario47() = runAndVerifyScenario(47, ReceiptOperationType.BUY_RETURN, 4700L, PaymentType.ELECTRONIC, false, "047")
    @Test fun testScenario48() = runAndVerifyScenario(48, ReceiptOperationType.BUY_RETURN, 4800L, PaymentType.MOBILE, true, "048")

    @Test fun testScenario49() = runAndVerifyScenario(49, ReceiptOperationType.SELL, 9999999L, PaymentType.CASH, false, "049")
    @Test fun testScenario50() = runAndVerifyScenario(50, ReceiptOperationType.BUY_RETURN, 1L, PaymentType.CARD, true, "050")

    @Test fun testScenario51() = runAndVerifyScenario(51, ReceiptOperationType.SELL, 5100L, PaymentType.CASH, false, "051")
    @Test fun testScenario52() = runAndVerifyScenario(52, ReceiptOperationType.SELL, 5200L, PaymentType.CARD, true, "052")
    @Test fun testScenario53() = runAndVerifyScenario(53, ReceiptOperationType.SELL, 5300L, PaymentType.ELECTRONIC, false, "053")
    @Test fun testScenario54() = runAndVerifyScenario(54, ReceiptOperationType.SELL, 5400L, PaymentType.MOBILE, true, "054")
    @Test fun testScenario55() = runAndVerifyScenario(55, ReceiptOperationType.SELL, 5500L, PaymentType.CASH, true, "055")
    @Test fun testScenario56() = runAndVerifyScenario(56, ReceiptOperationType.SELL, 5600L, PaymentType.CARD, false, "056")
    @Test fun testScenario57() = runAndVerifyScenario(57, ReceiptOperationType.SELL, 5700L, PaymentType.ELECTRONIC, true, "057")
    @Test fun testScenario58() = runAndVerifyScenario(58, ReceiptOperationType.SELL, 5800L, PaymentType.MOBILE, false, "058")
    @Test fun testScenario59() = runAndVerifyScenario(59, ReceiptOperationType.SELL, 5900L, PaymentType.CASH, false, "059")
    @Test fun testScenario60() = runAndVerifyScenario(60, ReceiptOperationType.SELL, 6000L, PaymentType.CARD, true, "060")
    @Test fun testScenario61() = runAndVerifyScenario(61, ReceiptOperationType.SELL, 6100L, PaymentType.ELECTRONIC, false, "061")
    @Test fun testScenario62() = runAndVerifyScenario(62, ReceiptOperationType.SELL, 6200L, PaymentType.MOBILE, true, "062")
    @Test fun testScenario63() = runAndVerifyScenario(63, ReceiptOperationType.SELL, 6300L, PaymentType.CASH, false, "063")
    @Test fun testScenario64() = runAndVerifyScenario(64, ReceiptOperationType.SELL, 6400L, PaymentType.CARD, true, "064")
    @Test fun testScenario65() = runAndVerifyScenario(65, ReceiptOperationType.SELL, 6500L, PaymentType.ELECTRONIC, false, "065")
    @Test fun testScenario66() = runAndVerifyScenario(66, ReceiptOperationType.SELL, 6600L, PaymentType.MOBILE, true, "066")
    @Test fun testScenario67() = runAndVerifyScenario(67, ReceiptOperationType.SELL, 6700L, PaymentType.CASH, true, "067")
    @Test fun testScenario68() = runAndVerifyScenario(68, ReceiptOperationType.SELL, 6800L, PaymentType.CARD, false, "068")
    @Test fun testScenario69() = runAndVerifyScenario(69, ReceiptOperationType.SELL, 6900L, PaymentType.ELECTRONIC, true, "069")
    @Test fun testScenario70() = runAndVerifyScenario(70, ReceiptOperationType.SELL, 7000L, PaymentType.MOBILE, false, "070")
    @Test fun testScenario71() = runAndVerifyScenario(71, ReceiptOperationType.SELL, 7100L, PaymentType.CASH, false, "071")
    @Test fun testScenario72() = runAndVerifyScenario(72, ReceiptOperationType.SELL, 7200L, PaymentType.CARD, true, "072")
    @Test fun testScenario73() = runAndVerifyScenario(73, ReceiptOperationType.SELL, 7300L, PaymentType.ELECTRONIC, false, "073")
    @Test fun testScenario74() = runAndVerifyScenario(74, ReceiptOperationType.SELL, 7400L, PaymentType.MOBILE, true, "074")
    @Test fun testScenario75() = runAndVerifyScenario(75, ReceiptOperationType.SELL, 7500L, PaymentType.CASH, false, "075")

    @Test fun testScenario76() = runAndVerifyScenario(76, ReceiptOperationType.SELL_RETURN, 7600L, PaymentType.CASH, false, "076")
    @Test fun testScenario77() = runAndVerifyScenario(77, ReceiptOperationType.SELL_RETURN, 7700L, PaymentType.CARD, true, "077")
    @Test fun testScenario78() = runAndVerifyScenario(78, ReceiptOperationType.SELL_RETURN, 7800L, PaymentType.ELECTRONIC, false, "078")
    @Test fun testScenario79() = runAndVerifyScenario(79, ReceiptOperationType.SELL_RETURN, 7900L, PaymentType.MOBILE, true, "079")
    @Test fun testScenario80() = runAndVerifyScenario(80, ReceiptOperationType.SELL_RETURN, 8000L, PaymentType.CASH, true, "080")
    @Test fun testScenario81() = runAndVerifyScenario(81, ReceiptOperationType.SELL_RETURN, 8100L, PaymentType.CARD, false, "081")
    @Test fun testScenario82() = runAndVerifyScenario(82, ReceiptOperationType.SELL_RETURN, 8200L, PaymentType.ELECTRONIC, true, "082")
    @Test fun testScenario83() = runAndVerifyScenario(83, ReceiptOperationType.SELL_RETURN, 8300L, PaymentType.MOBILE, false, "083")
    @Test fun testScenario84() = runAndVerifyScenario(84, ReceiptOperationType.SELL_RETURN, 8400L, PaymentType.CASH, false, "084")
    @Test fun testScenario85() = runAndVerifyScenario(85, ReceiptOperationType.SELL_RETURN, 8500L, PaymentType.CARD, true, "085")
    @Test fun testScenario86() = runAndVerifyScenario(86, ReceiptOperationType.SELL_RETURN, 8600L, PaymentType.ELECTRONIC, false, "086")
    @Test fun testScenario87() = runAndVerifyScenario(87, ReceiptOperationType.SELL_RETURN, 8700L, PaymentType.MOBILE, true, "087")
    @Test fun testScenario88() = runAndVerifyScenario(88, ReceiptOperationType.SELL_RETURN, 8800L, PaymentType.CASH, false, "088")
    @Test fun testScenario89() = runAndVerifyScenario(89, ReceiptOperationType.SELL_RETURN, 8900L, PaymentType.CARD, true, "089")
    @Test fun testScenario90() = runAndVerifyScenario(90, ReceiptOperationType.SELL_RETURN, 9000L, PaymentType.ELECTRONIC, false, "090")
    @Test fun testScenario91() = runAndVerifyScenario(91, ReceiptOperationType.SELL_RETURN, 9100L, PaymentType.MOBILE, true, "091")
    @Test fun testScenario92() = runAndVerifyScenario(92, ReceiptOperationType.SELL_RETURN, 9200L, PaymentType.CASH, true, "092")
    @Test fun testScenario93() = runAndVerifyScenario(93, ReceiptOperationType.SELL_RETURN, 9300L, PaymentType.CARD, false, "093")
    @Test fun testScenario94() = runAndVerifyScenario(94, ReceiptOperationType.SELL_RETURN, 9400L, PaymentType.ELECTRONIC, true, "094")
    @Test fun testScenario95() = runAndVerifyScenario(95, ReceiptOperationType.SELL_RETURN, 9500L, PaymentType.MOBILE, false, "095")
    @Test fun testScenario96() = runAndVerifyScenario(96, ReceiptOperationType.SELL_RETURN, 9600L, PaymentType.CASH, false, "096")
    @Test fun testScenario97() = runAndVerifyScenario(97, ReceiptOperationType.SELL_RETURN, 9700L, PaymentType.CARD, true, "097")
    @Test fun testScenario98() = runAndVerifyScenario(98, ReceiptOperationType.SELL_RETURN, 9800L, PaymentType.ELECTRONIC, false, "098")
    @Test fun testScenario99() = runAndVerifyScenario(99, ReceiptOperationType.SELL_RETURN, 9900L, PaymentType.MOBILE, true, "099")
    @Test fun testScenario100() = runAndVerifyScenario(100, ReceiptOperationType.SELL_RETURN, 10000L, PaymentType.CASH, false, "100")

    @Test fun testScenario101() = runAndVerifyScenario(101, ReceiptOperationType.BUY, 10100L, PaymentType.CASH, false, "101")
    @Test fun testScenario102() = runAndVerifyScenario(102, ReceiptOperationType.BUY, 10200L, PaymentType.CARD, true, "102")
    @Test fun testScenario103() = runAndVerifyScenario(103, ReceiptOperationType.BUY, 10300L, PaymentType.ELECTRONIC, false, "103")
    @Test fun testScenario104() = runAndVerifyScenario(104, ReceiptOperationType.BUY, 10400L, PaymentType.MOBILE, true, "104")
    @Test fun testScenario105() = runAndVerifyScenario(105, ReceiptOperationType.BUY, 10500L, PaymentType.CASH, true, "105")
    @Test fun testScenario106() = runAndVerifyScenario(106, ReceiptOperationType.BUY, 10600L, PaymentType.CARD, false, "106")
    @Test fun testScenario107() = runAndVerifyScenario(107, ReceiptOperationType.BUY, 10700L, PaymentType.ELECTRONIC, true, "107")
    @Test fun testScenario108() = runAndVerifyScenario(108, ReceiptOperationType.BUY, 10800L, PaymentType.MOBILE, false, "108")
    @Test fun testScenario109() = runAndVerifyScenario(109, ReceiptOperationType.BUY, 10900L, PaymentType.CASH, false, "109")
    @Test fun testScenario110() = runAndVerifyScenario(110, ReceiptOperationType.BUY, 11000L, PaymentType.CARD, true, "110")
    @Test fun testScenario111() = runAndVerifyScenario(111, ReceiptOperationType.BUY, 11100L, PaymentType.ELECTRONIC, false, "111")
    @Test fun testScenario112() = runAndVerifyScenario(112, ReceiptOperationType.BUY, 11200L, PaymentType.MOBILE, true, "112")
    @Test fun testScenario113() = runAndVerifyScenario(113, ReceiptOperationType.BUY, 11300L, PaymentType.CASH, false, "113")
    @Test fun testScenario114() = runAndVerifyScenario(114, ReceiptOperationType.BUY, 11400L, PaymentType.CARD, true, "114")
    @Test fun testScenario115() = runAndVerifyScenario(115, ReceiptOperationType.BUY, 11500L, PaymentType.ELECTRONIC, false, "115")
    @Test fun testScenario116() = runAndVerifyScenario(116, ReceiptOperationType.BUY, 11600L, PaymentType.MOBILE, true, "116")
    @Test fun testScenario117() = runAndVerifyScenario(117, ReceiptOperationType.BUY, 11700L, PaymentType.CASH, true, "117")
    @Test fun testScenario118() = runAndVerifyScenario(118, ReceiptOperationType.BUY, 11800L, PaymentType.CARD, false, "118")
    @Test fun testScenario119() = runAndVerifyScenario(119, ReceiptOperationType.BUY, 11900L, PaymentType.ELECTRONIC, true, "119")
    @Test fun testScenario120() = runAndVerifyScenario(120, ReceiptOperationType.BUY, 12000L, PaymentType.MOBILE, false, "120")
    @Test fun testScenario121() = runAndVerifyScenario(121, ReceiptOperationType.BUY, 12100L, PaymentType.CASH, false, "121")
    @Test fun testScenario122() = runAndVerifyScenario(122, ReceiptOperationType.BUY, 12200L, PaymentType.CARD, true, "122")
    @Test fun testScenario123() = runAndVerifyScenario(123, ReceiptOperationType.BUY, 12300L, PaymentType.ELECTRONIC, false, "123")
    @Test fun testScenario124() = runAndVerifyScenario(124, ReceiptOperationType.BUY, 12400L, PaymentType.MOBILE, true, "124")
    @Test fun testScenario125() = runAndVerifyScenario(125, ReceiptOperationType.BUY, 12500L, PaymentType.CASH, false, "125")

    @Test fun testScenario126() = runAndVerifyScenario(126, ReceiptOperationType.BUY_RETURN, 12600L, PaymentType.CASH, false, "126")
    @Test fun testScenario127() = runAndVerifyScenario(127, ReceiptOperationType.BUY_RETURN, 12700L, PaymentType.CARD, true, "127")
    @Test fun testScenario128() = runAndVerifyScenario(128, ReceiptOperationType.BUY_RETURN, 12800L, PaymentType.ELECTRONIC, false, "128")
    @Test fun testScenario129() = runAndVerifyScenario(129, ReceiptOperationType.BUY_RETURN, 12900L, PaymentType.MOBILE, true, "129")
    @Test fun testScenario130() = runAndVerifyScenario(130, ReceiptOperationType.BUY_RETURN, 13000L, PaymentType.CASH, true, "130")
    @Test fun testScenario131() = runAndVerifyScenario(131, ReceiptOperationType.BUY_RETURN, 13100L, PaymentType.CARD, false, "131")
    @Test fun testScenario132() = runAndVerifyScenario(132, ReceiptOperationType.BUY_RETURN, 13200L, PaymentType.ELECTRONIC, true, "132")
    @Test fun testScenario133() = runAndVerifyScenario(133, ReceiptOperationType.BUY_RETURN, 13300L, PaymentType.MOBILE, false, "133")
    @Test fun testScenario134() = runAndVerifyScenario(134, ReceiptOperationType.BUY_RETURN, 13400L, PaymentType.CASH, false, "134")
    @Test fun testScenario135() = runAndVerifyScenario(135, ReceiptOperationType.BUY_RETURN, 13500L, PaymentType.CARD, true, "135")
    @Test fun testScenario136() = runAndVerifyScenario(136, ReceiptOperationType.BUY_RETURN, 13600L, PaymentType.ELECTRONIC, false, "136")
    @Test fun testScenario137() = runAndVerifyScenario(137, ReceiptOperationType.BUY_RETURN, 13700L, PaymentType.MOBILE, true, "137")
    @Test fun testScenario138() = runAndVerifyScenario(138, ReceiptOperationType.BUY_RETURN, 13800L, PaymentType.CASH, false, "138")
    @Test fun testScenario139() = runAndVerifyScenario(139, ReceiptOperationType.BUY_RETURN, 13900L, PaymentType.CARD, true, "139")
    @Test fun testScenario140() = runAndVerifyScenario(140, ReceiptOperationType.BUY_RETURN, 14000L, PaymentType.ELECTRONIC, false, "140")
    @Test fun testScenario141() = runAndVerifyScenario(141, ReceiptOperationType.BUY_RETURN, 14100L, PaymentType.MOBILE, true, "141")
    @Test fun testScenario142() = runAndVerifyScenario(142, ReceiptOperationType.BUY_RETURN, 14200L, PaymentType.CASH, true, "142")
    @Test fun testScenario143() = runAndVerifyScenario(143, ReceiptOperationType.BUY_RETURN, 14300L, PaymentType.CARD, false, "143")
    @Test fun testScenario144() = runAndVerifyScenario(144, ReceiptOperationType.BUY_RETURN, 14400L, PaymentType.ELECTRONIC, true, "144")
    @Test fun testScenario145() = runAndVerifyScenario(145, ReceiptOperationType.BUY_RETURN, 14500L, PaymentType.MOBILE, false, "145")
    @Test fun testScenario146() = runAndVerifyScenario(146, ReceiptOperationType.BUY_RETURN, 14600L, PaymentType.CASH, false, "146")
    @Test fun testScenario147() = runAndVerifyScenario(147, ReceiptOperationType.BUY_RETURN, 14700L, PaymentType.CARD, true, "147")
    @Test fun testScenario148() = runAndVerifyScenario(148, ReceiptOperationType.BUY_RETURN, 14800L, PaymentType.ELECTRONIC, false, "148")
    @Test fun testScenario149() = runAndVerifyScenario(149, ReceiptOperationType.BUY_RETURN, 14900L, PaymentType.MOBILE, true, "149")
    @Test fun testScenario150() = runAndVerifyScenario(150, ReceiptOperationType.BUY_RETURN, 15000L, PaymentType.CASH, false, "150")
}

private class InMemoryStoragePort : StoragePort {
    private val counters = mutableMapOf<String, MutableMap<String, Long>>()
    private val kkms = mutableMapOf<String, KkmInfo>()
    private val users = mutableMapOf<String, MutableList<KkmUser>>()

    override fun <T> inTransaction(block: () -> T): T = block()

    override fun createKkm(info: KkmInfo): Boolean {
        kkms[info.id] = info
        return true
    }

    override fun updateKkm(info: KkmInfo): Boolean {
        kkms[info.id] = info
        return true
    }

    override fun findKkm(id: String): KkmInfo? = kkms[id]
    override fun findKkmForUpdate(id: String): KkmInfo? = kkms[id]

    override fun findKkmByRegistrationNumber(registrationNumber: String): KkmInfo? = null

    override fun findKkmBySystemId(systemId: String): KkmInfo? = null

    override fun listKkms(
        limit: Int,
        offset: Int,
        state: String?,
        search: String?,
        sortBy: String,
        sortOrder: String
    ): List<KkmInfo> {
        var filtered = kkms.values.asSequence()
        if (state != null) filtered = filtered.filter { it.state == state }
        if (search != null) {
            filtered = filtered.filter {
                it.registrationNumber?.contains(search, ignoreCase = true) == true ||
                it.id.contains(search, ignoreCase = true)
            }
        }
        return filtered.drop(offset).take(limit).toList()
    }

    override fun countKkms(state: String?, search: String?): Int {
        var filtered = kkms.values.asSequence()
        if (state != null) filtered = filtered.filter { it.state == state }
        if (search != null) {
            filtered = filtered.filter {
                it.registrationNumber?.contains(search, ignoreCase = true) == true ||
                it.id.contains(search, ignoreCase = true)
            }
        }
        return filtered.count()
    }

    override fun deleteKkm(id: String): Boolean = kkms.remove(id) != null

    override fun hasOfflineQueue(kkmId: String): Boolean = false

    override fun enqueueQueueTask(dto: QueueTask): Boolean = true
    override fun listQueueTasksByCashbox(cashboxId: String, lane: String, limit: Int, offset: Int): List<QueueTask> = emptyList()
    override fun nextPendingQueueTask(cashboxId: String, lane: String, now: Long): QueueTask? = null
    override fun updateQueueTaskStatus(id: String, status: String, attempt: Int, lastError: String?, nextAttemptAt: Long?): Boolean = true
    override fun markQueueTaskInProgress(id: String, now: Long): Boolean = true
    override fun deleteQueueTasksByCashbox(cashboxId: String): Boolean = true
    override fun tryAcquireQueueLock(cashboxId: String, ownerId: String, leaseUntil: Long, acquiredAt: Long): Boolean = true
    override fun renewQueueLock(cashboxId: String, ownerId: String, leaseUntil: Long, now: Long): Boolean = true
    override fun releaseQueueLock(cashboxId: String, ownerId: String): Boolean = true

    override fun deleteKkmCompletely(kkmId: String): Boolean {
        kkms.remove(kkmId)
        users.remove(kkmId)
        return true
    }

    override fun updateKkmToken(id: String, tokenEncryptedBase64: String, updatedAt: Long): Boolean {
        val current = kkms[id] ?: return false
        kkms[id] = current.copy(
            tokenEncryptedBase64 = tokenEncryptedBase64,
            tokenUpdatedAt = updatedAt,
            updatedAt = updatedAt
        )
        return true
    }

    override fun listUsers(kkmId: String): List<KkmUser> {
        return users[kkmId]?.toList() ?: emptyList()
    }

    override fun createUser(
        kkmId: String,
        userId: String,
        name: String,
        role: UserRole,
        pin: String,
        pinHash: String,
        createdAt: Long
    ): Boolean {
        val list = users.getOrPut(kkmId) { mutableListOf() }
        list.add(KkmUser(userId, name, role, pin, createdAt))
        return true
    }

    override fun updateUser(
        kkmId: String,
        userId: String,
        name: String?,
        role: UserRole?,
        pin: String?,
        pinHash: String?
    ): Boolean {
        val list = users[kkmId] ?: return false
        val index = list.indexOfFirst { it.id == userId }
        if (index == -1) return false
        val current = list[index]
        list[index] = current.copy(
            name = name ?: current.name,
            role = role ?: current.role,
            pin = pin ?: current.pin
        )
        return true
    }

    override fun deleteUser(kkmId: String, userId: String): Boolean {
        val list = users[kkmId] ?: return false
        return list.removeAll { it.id == userId }
    }

    override fun findUserById(kkmId: String, userId: String): KkmUser? {
        return users[kkmId]?.firstOrNull { it.id == userId }
    }

    override fun findUserByPin(kkmId: String, pinHash: String): KkmUser? = null

    override fun findOpenShift(kkmId: String): ShiftInfo? = null

    override fun findShiftById(shiftId: String): ShiftInfo? = null

    override fun listShifts(kkmId: String, limit: Int, offset: Int): List<ShiftInfo> = emptyList()

    override fun createShift(shift: ShiftInfo): Boolean = true

    override fun closeShift(shiftId: String, status: ShiftStatus, closedAt: Long, closeDocumentId: String?): Boolean = true

    override fun saveReceipt(request: ReceiptRequest, documentId: String, shiftId: String, createdAt: Long): Boolean = true

    override fun saveCashOperation(
        kkmId: String,
        type: String,
        amount: Money,
        documentId: String,
        shiftId: String,
        createdAt: Long
    ): Boolean = true

    override fun updateReceiptStatus(
        documentId: String,
        fiscalSign: String?,
        autonomousSign: String?,
        ofdStatus: String,
        deliveredAt: Long?,
        isAutonomous: Boolean?
    ): Boolean = true

    override fun loadCounters(kkmId: String, scope: String, shiftId: String?): Map<String, Long> {
        val key = "$kkmId:$scope:${shiftId ?: "-"}"
        return counters[key] ?: emptyMap()
    }

    override fun listCounters(kkmId: String): List<CounterSnapshot> {
        return counters.flatMap { (mapKey, values) ->
            val parts = mapKey.split(":", limit = 3)
            if (parts.size != 3 || parts[0] != kkmId) {
                emptyList()
            } else {
                val scope = parts[1]
                val shiftId = parts[2].takeIf { it != "-" }
                values.map { (key, value) ->
                    CounterSnapshot(
                        scope = scope,
                        shiftId = shiftId,
                        key = key,
                        value = value,
                        updatedAt = 0L
                    )
                }
            }
        }
    }

    override fun upsertCounter(kkmId: String, scope: String, shiftId: String?, key: String, value: Long): Boolean {
        val mapKey = "$kkmId:$scope:${shiftId ?: "-"}"
        val map = counters.getOrPut(mapKey) { mutableMapOf() }
        map[key] = value
        return true
    }

    override fun insertIdempotency(kkmId: String, idempotencyKey: String, operation: String): Boolean = true

    override fun findIdempotencyResponse(kkmId: String, idempotencyKey: String): String? = null

    override fun updateIdempotencyResponse(kkmId: String, idempotencyKey: String, responseRef: String?): Boolean = true

    override fun findFiscalDocumentById(id: String): FiscalDocumentSnapshot? = null

    override fun findFiscalDocumentWithReceiptPayload(documentId: String): Pair<FiscalDocumentSnapshot, ReceiptRequest>? = null

    override fun listFiscalDocumentsByShift(
        kkmId: String,
        shiftId: String,
        limit: Int,
        offset: Int
    ): List<FiscalDocumentSnapshot> = emptyList()

    override fun listFiscalDocumentsByPeriod(
        kkmId: String,
        fromInclusive: Long,
        toExclusive: Long,
        limit: Int,
        offset: Int
    ): List<FiscalDocumentSnapshot> = emptyList()

    override fun countFiscalDocuments(docType: String?): Long = 0L

    override fun countClosedShifts(): Long = 0L

    override fun countOfflineQueue(): Long = 0L
}
