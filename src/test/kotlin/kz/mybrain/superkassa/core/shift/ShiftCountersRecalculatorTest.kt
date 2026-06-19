package kz.mybrain.superkassa.core.shift

import kz.mybrain.superkassa.core.application.policy.CounterKeyFormats
import kz.mybrain.superkassa.core.application.policy.CounterScopes
import kz.mybrain.superkassa.core.application.policy.DefaultCounterUpdater
import kz.mybrain.superkassa.core.application.service.ShiftCountersRecalculator
import kz.mybrain.superkassa.core.domain.model.Money
import kz.mybrain.superkassa.core.domain.model.PaymentType
import kz.mybrain.superkassa.core.domain.model.ReceiptItem
import kz.mybrain.superkassa.core.domain.model.ReceiptOperationType
import kz.mybrain.superkassa.core.domain.model.ReceiptPayment
import kz.mybrain.superkassa.core.domain.model.ReceiptRequest
import kz.mybrain.superkassa.core.domain.model.ShiftInfo
import kz.mybrain.superkassa.core.domain.model.ShiftStatus
import kz.mybrain.superkassa.core.domain.model.TaxRegime
import kz.mybrain.superkassa.core.domain.model.VatGroup
import kz.mybrain.superkassa.core.support.TestStoragePort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShiftCountersRecalculatorTest {

    @Test
    fun `rebuildShiftCounters matches live counters for all receipt operation types`() {
        val storage = TestStoragePort()
        val updater = DefaultCounterUpdater(storage)
        val recalculator = ShiftCountersRecalculator(storage)
        val kkmId = "kkm-1"
        val shift =
            ShiftInfo(
                id = "shift-1",
                kkmId = kkmId,
                shiftNo = 11L,
                status = ShiftStatus.OPEN,
                openedAt = 1_700_000_000_000L
            )
        storage.createShift(shift)

        listOf("OPERATION_SELL", "OPERATION_SELL_RETURN", "OPERATION_BUY", "OPERATION_BUY_RETURN").forEach { op ->
            storage.upsertCounter(
                kkmId,
                CounterScopes.SHIFT,
                shift.id,
                CounterKeyFormats.START_SHIFT_NON_NULLABLE_SUM.format(op),
                10_000L
            )
            storage.upsertCounter(
                kkmId,
                CounterScopes.SHIFT,
                shift.id,
                CounterKeyFormats.NON_NULLABLE_SUM.format(op),
                10_000L
            )
        }

        val requests =
            listOf(
                ReceiptRequest(
                    kkmId = kkmId,
                    pin = "1111",
                    operation = ReceiptOperationType.SELL,
                    items =
                        listOf(
                            ReceiptItem("Item VAT16", "001", 1, Money(1160, 0), Money(1160, 0), vatGroup = VatGroup.VAT_16),
                            ReceiptItem("Item VAT5", "001", 1, Money(1050, 0), Money(1050, 0), vatGroup = VatGroup.VAT_5)
                        ),
                    payments =
                        listOf(
                            ReceiptPayment(PaymentType.CASH, Money(1100, 0)),
                            ReceiptPayment(PaymentType.CARD, Money(1060, 0))
                        ),
                    total = Money(2160, 0),
                    idempotencyKey = "idem-1",
                    taxRegime = TaxRegime.MIXED,
                    defaultVatGroup = VatGroup.VAT_16,
                    discount = Money(100, 0),
                    markup = Money(50, 0)
                ),
                ReceiptRequest(
                    kkmId = kkmId,
                    pin = "1111",
                    operation = ReceiptOperationType.SELL_RETURN,
                    items = listOf(ReceiptItem("Return", "001", 1, Money(600, 0), Money(600, 0), vatGroup = VatGroup.VAT_16)),
                    payments = listOf(ReceiptPayment(PaymentType.CASH, Money(600, 0))),
                    total = Money(600, 0),
                    idempotencyKey = "idem-2",
                    taxRegime = TaxRegime.VAT_PAYER,
                    defaultVatGroup = VatGroup.VAT_16
                ),
                ReceiptRequest(
                    kkmId = kkmId,
                    pin = "1111",
                    operation = ReceiptOperationType.BUY,
                    items =
                        listOf(
                            ReceiptItem(
                                "Buy item",
                                "001",
                                1,
                                Money(1000, 0),
                                Money(960, 0),
                                vatGroup = VatGroup.VAT_0,
                                discount = Money(40, 0)
                            )
                        ),
                    payments = listOf(ReceiptPayment(PaymentType.ELECTRONIC, Money(960, 0))),
                    total = Money(960, 0),
                    idempotencyKey = "idem-3",
                    taxRegime = TaxRegime.MIXED,
                    defaultVatGroup = VatGroup.VAT_0
                ),
                ReceiptRequest(
                    kkmId = kkmId,
                    pin = "1111",
                    operation = ReceiptOperationType.BUY_RETURN,
                    items =
                        listOf(
                            ReceiptItem(
                                "Buy return item",
                                "001",
                                1,
                                Money(500, 0),
                                Money(520, 0),
                                vatGroup = VatGroup.VAT_10,
                                markup = Money(20, 0)
                            )
                        ),
                    payments = listOf(ReceiptPayment(PaymentType.CASH, Money(520, 0))),
                    total = Money(520, 0),
                    idempotencyKey = "idem-4",
                    taxRegime = TaxRegime.MIXED,
                    defaultVatGroup = VatGroup.VAT_10
                )
            )

        requests.forEachIndexed { index, request ->
            val documentId = "doc-${index + 1}"
            val isOffline = index % 2 == 1
            storage.saveReceipt(
                request = request,
                documentId = documentId,
                shiftId = shift.id,
                createdAt = shift.openedAt + index + 1L
            )
            storage.updateReceiptStatus(
                documentId = documentId,
                fiscalSign = null,
                autonomousSign = if (isOffline) "AUTO-$index" else null,
                ofdStatus = if (isOffline) "TIMEOUT" else "SENT",
                deliveredAt = if (isOffline) null else shift.openedAt + 10 + index,
                isAutonomous = isOffline
            )
            updater.updateForReceipt(kkmId, shift.id, request, isOffline = isOffline)
        }

        val live = storage.loadCounters(kkmId, CounterScopes.SHIFT, shift.id)
        val rebuilt = recalculator.rebuildShiftCounters(kkmId, shift)

        val importantKeys =
            listOf(
                CounterKeyFormats.OPERATION_COUNT.format("OPERATION_SELL"),
                CounterKeyFormats.OPERATION_COUNT.format("OPERATION_SELL_RETURN"),
                CounterKeyFormats.OPERATION_COUNT.format("OPERATION_BUY"),
                CounterKeyFormats.OPERATION_COUNT.format("OPERATION_BUY_RETURN"),
                CounterKeyFormats.OPERATION_SUM.format("OPERATION_SELL"),
                CounterKeyFormats.OPERATION_SUM.format("OPERATION_SELL_RETURN"),
                CounterKeyFormats.OPERATION_SUM.format("OPERATION_BUY"),
                CounterKeyFormats.OPERATION_SUM.format("OPERATION_BUY_RETURN"),
                CounterKeyFormats.DISCOUNT_SUM.format("OPERATION_SELL"),
                CounterKeyFormats.DISCOUNT_SUM.format("OPERATION_BUY"),
                CounterKeyFormats.MARKUP_SUM.format("OPERATION_SELL"),
                CounterKeyFormats.MARKUP_SUM.format("OPERATION_BUY_RETURN"),
                CounterKeyFormats.TICKET_OFFLINE_COUNT.format("OPERATION_SELL_RETURN"),
                CounterKeyFormats.TICKET_OFFLINE_COUNT.format("OPERATION_BUY_RETURN"),
                CounterKeyFormats.PAYMENT_SUM.format("OPERATION_SELL", "PAYMENT_CASH"),
                CounterKeyFormats.PAYMENT_SUM.format("OPERATION_SELL", "PAYMENT_CARD"),
                CounterKeyFormats.PAYMENT_SUM.format("OPERATION_BUY", "PAYMENT_ELECTRONIC"),
                CounterKeyFormats.REVENUE_SUM,
                CounterKeyFormats.REVENUE_IS_NEGATIVE,
                CounterKeyFormats.NON_NULLABLE_SUM.format("OPERATION_SELL"),
                CounterKeyFormats.NON_NULLABLE_SUM.format("OPERATION_SELL_RETURN"),
                CounterKeyFormats.NON_NULLABLE_SUM.format("OPERATION_BUY"),
                CounterKeyFormats.NON_NULLABLE_SUM.format("OPERATION_BUY_RETURN")
            )

        importantKeys.forEach { key ->
            assertEquals(live[key] ?: 0L, rebuilt[key] ?: 0L, "Mismatch for key=$key")
        }

        assertEquals(1L, rebuilt[CounterKeyFormats.OPERATION_COUNT.format("OPERATION_SELL")])
        assertEquals(2160L, rebuilt[CounterKeyFormats.OPERATION_SUM.format("OPERATION_SELL")])
        assertEquals(100L, rebuilt[CounterKeyFormats.DISCOUNT_SUM.format("OPERATION_SELL")])
        assertEquals(50L, rebuilt[CounterKeyFormats.MARKUP_SUM.format("OPERATION_SELL")])
        assertEquals(1L, rebuilt[CounterKeyFormats.TICKET_OFFLINE_COUNT.format("OPERATION_SELL_RETURN")])
        assertEquals(1L, rebuilt[CounterKeyFormats.TICKET_OFFLINE_COUNT.format("OPERATION_BUY_RETURN")])

        val expectedRevenue = 2160L - 600L + 960L - 520L
        assertEquals(expectedRevenue, rebuilt[CounterKeyFormats.REVENUE_SUM])
        assertEquals(0L, rebuilt[CounterKeyFormats.REVENUE_IS_NEGATIVE])
        assertTrue((rebuilt[CounterKeyFormats.TAX_SUM.format("VAT_16", "OPERATION_SELL")] ?: 0L) > 0L)
        assertTrue((rebuilt[CounterKeyFormats.TAX_SUM.format("VAT_5", "OPERATION_SELL")] ?: 0L) > 0L)
    }

    @Test
    fun `rebuildShiftCounters handles large mixed shift`() {
        val storage = TestStoragePort()
        val updater = DefaultCounterUpdater(storage)
        val recalculator = ShiftCountersRecalculator(storage)
        val kkmId = "kkm-stress"
        val shift =
            ShiftInfo(
                id = "shift-stress",
                kkmId = kkmId,
                shiftNo = 77L,
                status = ShiftStatus.OPEN,
                openedAt = 1_700_100_000_000L
            )
        storage.createShift(shift)

        val opCounts = mutableMapOf<String, Long>()
        val opSums = mutableMapOf<String, Long>()
        var expectedRevenue = 0L

        for (i in 1..3000) {
            val operation =
                when (i % 4) {
                    0 -> ReceiptOperationType.SELL
                    1 -> ReceiptOperationType.SELL_RETURN
                    2 -> ReceiptOperationType.BUY
                    else -> ReceiptOperationType.BUY_RETURN
                }
            val operationKey =
                when (operation) {
                    ReceiptOperationType.SELL -> "OPERATION_SELL"
                    ReceiptOperationType.SELL_RETURN -> "OPERATION_SELL_RETURN"
                    ReceiptOperationType.BUY -> "OPERATION_BUY"
                    ReceiptOperationType.BUY_RETURN -> "OPERATION_BUY_RETURN"
                }
            val base = 100L + (i % 50)
            val discount = if (i % 10 == 0) 3L else 0L
            val markup = if (i % 15 == 0) 2L else 0L
            val total = (base - discount + markup).coerceAtLeast(0L)
            val vatGroup =
                when (i % 3) {
                    0 -> VatGroup.VAT_16
                    1 -> VatGroup.VAT_5
                    else -> VatGroup.NO_VAT
                }
            val taxRegime = if (vatGroup == VatGroup.NO_VAT) TaxRegime.NO_VAT else TaxRegime.MIXED
            val paymentType = if (i % 2 == 0) PaymentType.CASH else PaymentType.CARD
            val isOffline = i % 9 == 0

            val request =
                ReceiptRequest(
                    kkmId = kkmId,
                    pin = "1111",
                    operation = operation,
                    items =
                        listOf(
                            ReceiptItem(
                                name = "Item-$i",
                                sectionCode = "001",
                                quantity = 1,
                                price = Money(base, 0),
                                sum = Money(base, 0),
                                vatGroup = vatGroup
                            )
                        ),
                    payments = listOf(ReceiptPayment(paymentType, Money(total, 0))),
                    total = Money(total, 0),
                    idempotencyKey = "idem-$i",
                    taxRegime = taxRegime,
                    defaultVatGroup = vatGroup,
                    discount = if (discount > 0L) Money(discount, 0) else null,
                    markup = if (markup > 0L) Money(markup, 0) else null
                )

            val documentId = "stress-doc-$i"
            storage.saveReceipt(
                request = request,
                documentId = documentId,
                shiftId = shift.id,
                createdAt = shift.openedAt + i
            )
            storage.updateReceiptStatus(
                documentId = documentId,
                fiscalSign = null,
                autonomousSign = if (isOffline) "AUTO-$i" else null,
                ofdStatus = if (isOffline) "TIMEOUT" else "SENT",
                deliveredAt = if (isOffline) null else shift.openedAt + i + 1,
                isAutonomous = isOffline
            )
            updater.updateForReceipt(kkmId, shift.id, request, isOffline = isOffline)

            opCounts[operationKey] = (opCounts[operationKey] ?: 0L) + 1L
            opSums[operationKey] = (opSums[operationKey] ?: 0L) + total
            expectedRevenue +=
                when (operation) {
                    ReceiptOperationType.SELL, ReceiptOperationType.BUY -> total
                    ReceiptOperationType.SELL_RETURN, ReceiptOperationType.BUY_RETURN -> -total
                }
        }

        val rebuilt = recalculator.rebuildAndPersistShiftCounters(kkmId, shift)
        val live = storage.loadCounters(kkmId, CounterScopes.SHIFT, shift.id)

        listOf("OPERATION_SELL", "OPERATION_SELL_RETURN", "OPERATION_BUY", "OPERATION_BUY_RETURN").forEach { op ->
            assertEquals(opCounts[op] ?: 0L, rebuilt[CounterKeyFormats.OPERATION_COUNT.format(op)] ?: 0L)
            assertEquals(opSums[op] ?: 0L, rebuilt[CounterKeyFormats.OPERATION_SUM.format(op)] ?: 0L)
            assertEquals(live[CounterKeyFormats.OPERATION_SUM.format(op)] ?: 0L, rebuilt[CounterKeyFormats.OPERATION_SUM.format(op)] ?: 0L)
        }

        assertEquals(expectedRevenue, rebuilt[CounterKeyFormats.REVENUE_SUM] ?: 0L)
        assertEquals(if (expectedRevenue < 0) 1L else 0L, rebuilt[CounterKeyFormats.REVENUE_IS_NEGATIVE] ?: 0L)
    }
}
