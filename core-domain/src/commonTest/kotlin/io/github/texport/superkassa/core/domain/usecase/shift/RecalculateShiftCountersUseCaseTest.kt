package io.github.texport.superkassa.core.domain.impl.usecase.shift

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import io.github.texport.superkassa.core.domain.api.model.common.CounterKeyFormats
import io.github.texport.superkassa.core.domain.api.model.common.CounterScopes
import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.common.TaxRegime
import io.github.texport.superkassa.core.domain.api.model.common.VatGroup
import io.github.texport.superkassa.core.domain.api.model.receipt.PaymentType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptDocumentTypes
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptItem
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptPayment
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftStatus
import io.github.texport.superkassa.core.domain.impl.usecase.counter.UpdateCountersUseCase
import io.github.texport.superkassa.core.support.TestStoragePort

class RecalculateShiftCountersUseCaseTest {

    @Test
    fun `rebuildShiftCounters matches live counters for all receipt operation types`() {
        val storage = TestStoragePort()
        val updater = UpdateCountersUseCase(storage)
        val recalculator = RecalculateShiftCountersUseCase(storage)
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
            updater.execute(kkmId, shift.id, request, isOffline = isOffline)
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
        // Операции — сумма позиций до скидки и наценки на чек (2210), а не сумма чека (2160).
        assertEquals(221_000L, rebuilt[CounterKeyFormats.OPERATION_SUM.format("OPERATION_SELL")])
        assertEquals(10_000L, rebuilt[CounterKeyFormats.DISCOUNT_SUM.format("OPERATION_SELL")])
        assertEquals(5_000L, rebuilt[CounterKeyFormats.MARKUP_SUM.format("OPERATION_SELL")])
        assertEquals(1L, rebuilt[CounterKeyFormats.TICKET_OFFLINE_COUNT.format("OPERATION_SELL_RETURN")])
        assertEquals(1L, rebuilt[CounterKeyFormats.TICKET_OFFLINE_COUNT.format("OPERATION_BUY_RETURN")])

        // Знак выручки у покупки тот же, что у денежного ящика: покупка
        // деньги выдаёт и выручку уменьшает, возврат покупки возвращает
        // их и увеличивает (эталон OperationCalculator.addTicket).
        // Прежде здесь стоял обратный знак, и проверка закрепляла его.
        val expectedRevenue = 216_000L - 60_000L - 96_000L + 52_000L
        assertEquals(expectedRevenue, rebuilt[CounterKeyFormats.REVENUE_SUM])
        assertEquals(0L, rebuilt[CounterKeyFormats.REVENUE_IS_NEGATIVE])
        assertTrue((rebuilt[CounterKeyFormats.TAX_SUM.format("VAT_16", "OPERATION_SELL")] ?: 0L) > 0L)
        assertTrue((rebuilt[CounterKeyFormats.TAX_SUM.format("VAT_5", "OPERATION_SELL")] ?: 0L) > 0L)
    }

    @Test
    fun `rebuildShiftCounters handles large mixed shift`() {
        val storage = TestStoragePort()
        val updater = UpdateCountersUseCase(storage)
        val recalculator = RecalculateShiftCountersUseCase(storage)
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
            updater.execute(kkmId, shift.id, request, isOffline = isOffline)

            opCounts[operationKey] = (opCounts[operationKey] ?: 0L) + 1L
            // Операции — без скидки и наценки на чек: сумма позиций, как у БФД.
            opSums[operationKey] = (opSums[operationKey] ?: 0L) + base * 100L
            // Знак выручки тот же, что у денежного ящика: покупка деньги
            // выдаёт, возврат покупки возвращает (эталон
            // OperationCalculator). Прежде ожидание считалось обратным
            // правилом и закрепляло дефект.
            expectedRevenue +=
                when (operation) {
                    ReceiptOperationType.SELL, ReceiptOperationType.BUY_RETURN -> total * 100L
                    ReceiptOperationType.SELL_RETURN, ReceiptOperationType.BUY -> -total * 100L
                }
        }

        val rebuilt = recalculator.execute(kkmId, shift)
        val live = storage.loadCounters(kkmId, CounterScopes.SHIFT, shift.id)

        listOf("OPERATION_SELL", "OPERATION_SELL_RETURN", "OPERATION_BUY", "OPERATION_BUY_RETURN").forEach { op ->
            assertEquals(opCounts[op] ?: 0L, rebuilt[CounterKeyFormats.OPERATION_COUNT.format(op)] ?: 0L)
            assertEquals(opSums[op] ?: 0L, rebuilt[CounterKeyFormats.OPERATION_SUM.format(op)] ?: 0L)
            assertEquals(live[CounterKeyFormats.OPERATION_SUM.format(op)] ?: 0L, rebuilt[CounterKeyFormats.OPERATION_SUM.format(op)] ?: 0L)
        }

        assertEquals(expectedRevenue, rebuilt[CounterKeyFormats.REVENUE_SUM] ?: 0L)
        assertEquals(if (expectedRevenue < 0) 1L else 0L, rebuilt[CounterKeyFormats.REVENUE_IS_NEGATIVE] ?: 0L)
    }

    @Test
    fun `rebuildShiftCounters preserves OFD initial shift counters snapshot when no local docs exist`() {
        val storage = TestStoragePort()
        val recalculator = RecalculateShiftCountersUseCase(storage)
        val kkmId = "kkm-ofd-1"
        val shift = ShiftInfo(
            id = "shift-ofd-1",
            kkmId = kkmId,
            shiftNo = 5L,
            status = ShiftStatus.OPEN,
            openedAt = 1_700_000_000_000L
        )
        storage.createShift(shift)

        // Имитируем начальный слепок счетчиков, полученный от ОФД при добавлении существующей ККМ
        storage.upsertCounter(kkmId, CounterScopes.SHIFT, shift.id, "start_shift_cash.sum", 50_000L)
        storage.upsertCounter(kkmId, CounterScopes.SHIFT, shift.id, CounterKeyFormats.START_SHIFT_NON_NULLABLE_SUM.format("OPERATION_SELL"), 179_440_00L)
        storage.upsertCounter(kkmId, CounterScopes.SHIFT, shift.id, CounterKeyFormats.NON_NULLABLE_SUM.format("OPERATION_SELL"), 179_440_00L)
        storage.upsertCounter(kkmId, CounterScopes.SHIFT, shift.id, CounterKeyFormats.OPERATION_COUNT.format("OPERATION_SELL"), 7L)
        storage.upsertCounter(kkmId, CounterScopes.SHIFT, shift.id, CounterKeyFormats.OPERATION_SUM.format("OPERATION_SELL"), 120_000_00L)

        val rebuilt = recalculator.execute(kkmId, shift)

        // Проверяем, что стартовые необнуляемые суммы и счетчики ОФД не затерлись нулевыми значениями
        assertEquals(50_000L, rebuilt["start_shift_cash.sum"])
        assertEquals(179_440_00L, rebuilt[CounterKeyFormats.START_SHIFT_NON_NULLABLE_SUM.format("OPERATION_SELL")])
        assertEquals(179_440_00L, rebuilt[CounterKeyFormats.NON_NULLABLE_SUM.format("OPERATION_SELL")])
    }

    @Test
    fun `rebuildShiftCounters correctly aggregates stored local receipt docs with BUY and SELL docTypes`() {
        val storage = TestStoragePort()
        val recalculator = RecalculateShiftCountersUseCase(storage)
        val kkmId = "kkm-doctypes-1"
        val shift = ShiftInfo(
            id = "shift-dt-1",
            kkmId = kkmId,
            shiftNo = 2L,
            status = ShiftStatus.OPEN,
            openedAt = 1_700_000_000_000L
        )
        storage.createShift(shift)

        val sellReq = ReceiptRequest(
            kkmId = kkmId,
            pin = "1111",
            operation = ReceiptOperationType.SELL,
            items = listOf(ReceiptItem("Sell Item", "001", 1, Money(5000, 0), Money(5000, 0), vatGroup = VatGroup.VAT_16)),
            payments = listOf(ReceiptPayment(PaymentType.CASH, Money(5000, 0))),
            total = Money(5000, 0),
            idempotencyKey = "idem-sell",
            taxRegime = TaxRegime.VAT_PAYER,
            defaultVatGroup = VatGroup.VAT_16
        )
        storage.saveReceipt(sellReq, "doc-sell-1", shift.id, shift.openedAt + 100)
        // Проведённый чек несёт фискальный признак: без него пересчёт
        // справедливо считает документ непроведённым и в счётчики не берёт.
        storage.updateReceiptStatus(
            documentId = "doc-sell-1",
            fiscalSign = "fs-doc-sell-1",
            autonomousSign = null,
            ofdStatus = "SENT",
            deliveredAt = shift.openedAt + 100 + 1,
            isAutonomous = false
        )

        val buyReq = ReceiptRequest(
            kkmId = kkmId,
            pin = "1111",
            operation = ReceiptOperationType.BUY,
            items = listOf(ReceiptItem("Buy Item", "001", 1, Money(2000, 0), Money(2000, 0), vatGroup = VatGroup.VAT_16)),
            payments = listOf(ReceiptPayment(PaymentType.CARD, Money(2000, 0))),
            total = Money(2000, 0),
            idempotencyKey = "idem-buy",
            taxRegime = TaxRegime.VAT_PAYER,
            defaultVatGroup = VatGroup.VAT_16
        )
        storage.saveReceipt(buyReq, "doc-buy-1", shift.id, shift.openedAt + 200)
        // Проведённый чек несёт фискальный признак: без него пересчёт
        // справедливо считает документ непроведённым и в счётчики не берёт.
        storage.updateReceiptStatus(
            documentId = "doc-buy-1",
            fiscalSign = "fs-doc-buy-1",
            autonomousSign = null,
            ofdStatus = "SENT",
            deliveredAt = shift.openedAt + 200 + 1,
            isAutonomous = false
        )

        val rebuilt = recalculator.execute(kkmId, shift)

        assertEquals(1L, rebuilt[CounterKeyFormats.OPERATION_COUNT.format("OPERATION_SELL")])
        assertEquals(500_000L, rebuilt[CounterKeyFormats.OPERATION_SUM.format("OPERATION_SELL")])
        assertEquals(1L, rebuilt[CounterKeyFormats.OPERATION_COUNT.format("OPERATION_BUY")])
        assertEquals(200_000L, rebuilt[CounterKeyFormats.OPERATION_SUM.format("OPERATION_BUY")])
    }

    /**
     * Чек, который узел отказался провести, остаётся в журнале без признаков
     * и с состоянием доставки PENDING. При пробитии он в счётчики не попал,
     * и пересчёт обязан вести себя так же: иначе X-отчёт добавлял бы в кассу
     * деньги по чекам, которых не было.
     */
    @Test
    fun `непроведённый чек в пересчёт не попадает`() {
        val storage = TestStoragePort()
        val recalculator = RecalculateShiftCountersUseCase(storage)
        val kkmId = "kkm-refused-1"
        val shift = ShiftInfo(
            id = "shift-refused-1",
            kkmId = kkmId,
            shiftNo = 3L,
            status = ShiftStatus.OPEN,
            openedAt = 1_700_000_000_000L
        )
        storage.createShift(shift)

        val refused = ReceiptRequest(
            kkmId = kkmId,
            pin = "1111",
            operation = ReceiptOperationType.SELL,
            items = listOf(ReceiptItem("Item", "001", 1, Money(1000, 0), Money(1000, 0))),
            payments = listOf(ReceiptPayment(PaymentType.CASH, Money(1000, 0))),
            total = Money(1000, 0),
            idempotencyKey = "idem-refused"
        )
        storage.saveReceipt(refused, "doc-refused", shift.id, shift.openedAt + 100)

        val rebuilt = recalculator.execute(kkmId, shift)

        assertNull(rebuilt[CounterKeyFormats.OPERATION_SUM.format("OPERATION_SELL")])
        assertEquals(0L, rebuilt[CounterKeyFormats.CASH_SUM])
    }

    /**
     * Тип документа-чека называет операцию: SALE, RETURN, BUY, BUY_RETURN.
     * Пересчёт знал только прежние названия и продажу с возвратом продажи
     * пропускал: X-отчёт показывал пустую кассу при полном ящике.
     */
    @Test
    fun `пересчёт видит чек продажи под его нынешним типом документа`() {
        val storage = TestStoragePort()
        val recalculator = RecalculateShiftCountersUseCase(storage)
        val kkmId = "kkm-sale-doctype"
        val shift = ShiftInfo(
            id = "shift-sale-doctype",
            kkmId = kkmId,
            shiftNo = 6L,
            status = ShiftStatus.OPEN,
            openedAt = 1_700_000_000_000L
        )
        storage.createShift(shift)

        val sale = ReceiptRequest(
            kkmId = kkmId,
            pin = "1111",
            operation = ReceiptOperationType.SELL,
            items = listOf(ReceiptItem("Item", "001", 1, Money(1000, 0), Money(1000, 0))),
            payments = listOf(ReceiptPayment(PaymentType.CASH, Money(1000, 0))),
            total = Money(1000, 0),
            idempotencyKey = "idem-sale-doctype"
        )
        storage.saveReceipt(sale, "doc-sale", shift.id, shift.openedAt + 100)
        storage.updateReceiptStatus(
            documentId = "doc-sale",
            fiscalSign = "fs-doc-sale",
            autonomousSign = null,
            ofdStatus = "SENT",
            deliveredAt = shift.openedAt + 101,
            isAutonomous = false
        )

        assertEquals(ReceiptDocumentTypes.SALE, storage.findFiscalDocumentById("doc-sale")?.docType)

        val rebuilt = recalculator.execute(kkmId, shift)

        assertEquals(1L, rebuilt[CounterKeyFormats.OPERATION_COUNT.format("OPERATION_SELL")])
        assertEquals(100_000L, rebuilt[CounterKeyFormats.OPERATION_SUM.format("OPERATION_SELL")])
        assertEquals(100_000L, rebuilt[CounterKeyFormats.CASH_SUM])
    }

    /**
     * Наличные в ящике живут в двух областях сразу: сменной и глобальной.
     * Пересчёт переписывал только сменную, и после него кассир видел в ящике
     * одно, а в журнале другое.
     */
    @Test
    fun `пересчёт приводит глобальный счётчик наличных к согласию со сменой`() {
        val storage = TestStoragePort()
        val updater = UpdateCountersUseCase(storage)
        val recalculator = RecalculateShiftCountersUseCase(storage)
        val kkmId = "kkm-global-cash"
        val shift = ShiftInfo(
            id = "shift-global-cash",
            kkmId = kkmId,
            shiftNo = 4L,
            status = ShiftStatus.OPEN,
            openedAt = 1_700_000_000_000L
        )
        storage.createShift(shift)

        val request = ReceiptRequest(
            kkmId = kkmId,
            pin = "1111",
            operation = ReceiptOperationType.SELL,
            items = listOf(ReceiptItem("Item", "001", 1, Money(5000, 0), Money(5000, 0))),
            payments = listOf(ReceiptPayment(PaymentType.CASH, Money(5000, 0))),
            total = Money(5000, 0),
            idempotencyKey = "idem-global-cash"
        )
        // Чек прошёл по счётчикам обеих областей...
        updater.execute(kkmId, shift.id, request, isOffline = false)
        // ...но фискальным так и не стал: признака у документа нет.
        storage.saveReceipt(request, "doc-global-cash", shift.id, shift.openedAt + 100)

        assertEquals(500_000L, storage.loadCounters(kkmId, CounterScopes.SHIFT, shift.id)[CounterKeyFormats.CASH_SUM])
        assertEquals(500_000L, storage.loadCounters(kkmId, CounterScopes.GLOBAL, null)[CounterKeyFormats.CASH_SUM])

        val rebuilt = recalculator.execute(kkmId, shift)

        assertEquals(0L, rebuilt[CounterKeyFormats.CASH_SUM])
        assertEquals(
            0L,
            storage.loadCounters(kkmId, CounterScopes.GLOBAL, null)[CounterKeyFormats.CASH_SUM]
        )
    }
}
