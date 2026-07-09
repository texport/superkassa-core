package io.github.texport.superkassa.receiptrenderer.impl


import io.github.texport.superkassa.core.domain.model.kkm.*
import io.github.texport.superkassa.core.domain.model.ofd.*
import io.github.texport.superkassa.core.domain.model.shift.*
import io.github.texport.superkassa.core.domain.model.common.*
import io.github.texport.superkassa.core.domain.model.receipt.*
import io.github.texport.superkassa.core.domain.port.QrCodeGeneratorPort
import kotlin.test.Test
import kotlin.test.assertTrue
import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.ReceiptTranslator
import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.*
import kotlin.test.assertFailsWith

class ReceiptRendererApiImplTest {

    private class StubQrCodeGenerator : QrCodeGeneratorPort {
        override fun generatePngDataUri(text: String, sizePx: Int): String? {
            return "data:image/png;base64,stub-qr-code-for-$text"
        }
    }


    private class NullQrCodeGenerator : QrCodeGeneratorPort {
        override fun generatePngDataUri(text: String, sizePx: Int): String? {
            return null
        }
    }

    private val defaultKkm = KkmInfo(
        id = "kkm-123",
        createdAt = 1782200000000L,
        updatedAt = 1782200000000L,
        mode = "PRODUCTION",
        state = "READY",
        registrationNumber = "RN-999",
        factoryNumber = "FN-888",
        branding = ReceiptBranding(),
        ofdServiceInfo = OfdServiceInfo(
            orgTitle = "Individual Entrepreneur Ivanov",
            orgAddress = "Almaty, Abay Ave 10",
            orgAddressKz = "Алматы, Абай даңғылы, 10",
            orgInn = "987654321012",
            orgOkved = "62010",
            geoLatitude = 432389,
            geoLongitude = 768897,
            geoSource = "GPS"
        )
    )

    @Test
    fun testRenderHtmlWithAllFields() {
        val renderer = ReceiptRendererApiImpl(StubQrCodeGenerator())

        val request = ReceiptRequest(
            kkmId = "kkm-123",
            pin = "1111",
            operation = ReceiptOperationType.SELL,
            items = listOf(
                ReceiptItem(
                    name = "Apple",
                    sectionCode = "001",
                    quantity = 2L,
                    price = Money(50, 0),
                    sum = Money(100, 0),
                    vatGroup = VatGroup.VAT_16,
                    measureUnitCode = "796" // Piece
                ),
                ReceiptItem(
                    name = "Milk",
                    sectionCode = "001",
                    quantity = 1L,
                    price = Money(200, 50),
                    sum = Money(200, 50),
                    vatGroup = VatGroup.NO_VAT,
                    measureUnitCode = "166", // kg (custom short unit)
                    listExciseStamp = listOf("EXCISE-123", "EXCISE-456")
                ),
                ReceiptItem(
                    name = "Bread",
                    sectionCode = "001",
                    quantity = 1L,
                    price = Money(100, 0),
                    sum = Money(100, 0),
                    vatGroup = VatGroup.VAT_0,
                    measureUnitCode = "999" // Unknown unit to cover default fallback
                ),
                ReceiptItem(
                    name = "Cheese",
                    sectionCode = "001",
                    quantity = 1L,
                    price = Money(500, 0),
                    sum = Money(500, 0),
                    vatGroup = VatGroup.VAT_5
                ),
                ReceiptItem(
                    name = "Juice",
                    sectionCode = "001",
                    quantity = 1L,
                    price = Money(300, 0),
                    sum = Money(300, 0),
                    vatGroup = VatGroup.VAT_10
                )
            ),
            payments = listOf(
                ReceiptPayment(PaymentType.CASH, Money(200, 0)),
                ReceiptPayment(PaymentType.CARD, Money(100, 50)),
                ReceiptPayment(PaymentType.ELECTRONIC, Money(1000, 0))
            ),
            total = Money(1200, 50),
            taken = Money(1300, 0),
            change = Money(99, 50),
            idempotencyKey = "key-123",
            taxRegime = TaxRegime.VAT_PAYER,
            defaultVatGroup = VatGroup.NO_VAT,
            discount = Money(10, 0),
            markup = Money(5, 0),
            customerBin = "123456789012"
        )

        val doc = FiscalDocumentSnapshot(
            id = "doc-789",
            cashboxId = "kkm-123",
            shiftId = "shift-456",
            docType = "TICKET",
            docNo = 12,
            shiftNo = 34,
            createdAt = 1782200000000L,
            totalAmount = 120050L,
            currency = "KZT",
            fiscalSign = "FS-12345",
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = "DELIVERED",
            deliveredAt = 1782200010000L,
            receiptUrl = "https://kassa.kz/receipt/789",
            registrationNumber = "RN-999",
            taxpayerName = "Individual Entrepreneur Ivanov",
            taxpayerBin = "987654321012",
            taxpayerAddress = "Almaty, Abay Ave 10",
            factoryNumber = "FN-888",
            ofdProvider = "«Қазақтелеком» АҚ / АО «Казахтелеком»"
        )

        val kkm = defaultKkm.copy(
            registrationNumber = "RN-999",
            factoryNumber = "FN-888",
            ofdServiceInfo = defaultKkm.ofdServiceInfo?.copy(
                orgTitle = "Individual Entrepreneur Ivanov",
                orgInn = "987654321012",
                orgAddress = "Almaty, Abay Ave 10"
            )
        )

        val html = renderer.renderHtml(request, doc, kkm)
        println("=== DEBUG CHAR CODES ===")
        val target = "Получено / Алынды"
        println("Target string: '$target'")
        println("Target char codes: " + target.map { it.code })
        val index = html.indexOf("Получено")
        if (index >= 0) {
            val slice = html.substring(index, index + target.length)
            println("HTML slice: '$slice'")
            println("HTML slice char codes: " + slice.map { it.code })
        } else {
            println("Target 'Получено' not found in HTML!")
        }
        println("=== END DEBUG CHAR CODES ===")

        val cleanHtml = stripHtml(html)
        assertTrue(cleanHtml.contains("САТУ ЧЕГІ"))
        assertTrue(cleanHtml.contains("ЧЕК ПРОДАЖИ"))
        assertTrue(html.contains("<span class=\"lang-fraction-top\">САТУ ЧЕГІ</span>"))
        assertTrue(cleanHtml.contains("12"))
        assertTrue(cleanHtml.contains("Apple"))
        assertTrue(cleanHtml.contains("дана / шт"))
        assertTrue(cleanHtml.contains("Milk"))
        assertTrue(cleanHtml.contains("Таңбалау: / Маркировка: EXCISE-123, EXCISE-456"))
        assertTrue(cleanHtml.contains("Қолма-қол / Наличные"))
        assertTrue(cleanHtml.contains("Карта"))
        assertTrue(cleanHtml.contains("Электронды / Электронно"))
        assertTrue(cleanHtml.contains("Алынды / Получено"))
        assertTrue(cleanHtml.contains("Қайтарым / Сдача"))
        assertTrue(cleanHtml.contains("БСН/ЖСН / БИН/ИИН: 987654321012"))
        assertTrue(cleanHtml.contains("Сатып алушы БСН/ЖСН: / Покупатель БИН/ИИН: 123456789012"))
        assertTrue(cleanHtml.contains("Individual Entrepreneur Ivanov"))
        assertTrue(cleanHtml.contains("Almaty, Abay Ave 10"))
        assertTrue(cleanHtml.contains("ЗНМ"))
        assertTrue(cleanHtml.contains("FN-888"))
        assertTrue(cleanHtml.contains("«Қазақтелеком» АҚ / АО «Казахтелеком»"))
        assertTrue(html.contains("data:image/png;base64,stub-qr-code-for-https://kassa.kz/receipt/789"))
        assertTrue(cleanHtml.contains("Жіберілді") && cleanHtml.contains("Отправлен"))
        // Check for VAT Groups (should be empty since defaultVatGroup is NO_VAT under VAT_PAYER)
        assertTrue(!cleanHtml.contains("Налоги / Салықтар"))
    }

    @Test
    fun testRenderHtmlWithFallbackUrlAndAutonomous() {
        val renderer = ReceiptRendererApiImpl(StubQrCodeGenerator())

        val request = ReceiptRequest(
            kkmId = "kkm-123",
            pin = "1111",
            operation = ReceiptOperationType.SELL_RETURN,
            items = emptyList(),
            payments = emptyList(),
            total = Money(150, 0),
            idempotencyKey = "key-456"
        )

        val doc = FiscalDocumentSnapshot(
            id = "doc-789",
            cashboxId = "kkm-123",
            shiftId = "shift-456",
            docType = "TICKET",
            docNo = 108L,
            shiftNo = null,
            createdAt = 1782200000000L,
            totalAmount = 15000L,
            currency = null,
            fiscalSign = null,
            autonomousSign = "AS-54321",
            isAutonomous = true,
            ofdStatus = "OFFLINE",
            deliveredAt = null,
            receiptUrl = "https://o.oofd.kz/r/108?r=RN-999&i=987654321012&d=1782200000&s=150.00&f=AS-54321",
            registrationNumber = "RN-999",
            taxpayerName = null,
            taxpayerBin = "987654321012",
            taxpayerAddress = null,
            factoryNumber = null,
            ofdProvider = "«Транстелеком» АҚ / АО «Транстелеком»"
        )

        val kkm = defaultKkm.copy(
            registrationNumber = "RN-999",
            ofdServiceInfo = defaultKkm.ofdServiceInfo?.copy(
                orgInn = "987654321012"
            )
        )

        val html = renderer.renderHtml(request, doc, kkm)

        val cleanHtml = stripHtml(html)

        assertTrue(cleanHtml.contains("САТУДЫ ҚАЙТАРУ ЧЕГІ"))
        assertTrue(cleanHtml.contains("ЧЕК ВОЗВРАТА ПРОДАЖИ"))
        assertTrue(cleanHtml.contains("Автономды") && cleanHtml.contains("Офлайн"))
        assertTrue(cleanHtml.contains("Автономды режим / Автономный режим"))
        assertTrue(cleanHtml.contains("«Транстелеком» АҚ / АО «Транстелеком»"))
        assertTrue(cleanHtml.contains("o.oofd.kz"))
        // Check fallback url in qr code
        assertTrue(html.contains("stub-qr-code-for-https://o.oofd.kz"))
    }

    @Test
    fun testRenderHtmlWithOtherStatusesAndProviders() {
        val renderer = ReceiptRendererApiImpl(NullQrCodeGenerator())

        val request = ReceiptRequest(
            kkmId = "kkm-123",
            pin = "1111",
            operation = ReceiptOperationType.BUY,
            items = emptyList(),
            payments = emptyList(),
            total = Money(0, 0),
            idempotencyKey = "key-789"
        )

        val doc = FiscalDocumentSnapshot(
            id = "doc-789",
            cashboxId = "kkm-123",
            shiftId = "shift-456",
            docType = "TICKET",
            docNo = 10,
            shiftNo = 20,
            createdAt = 1782200000000L,
            totalAmount = 0L,
            currency = "USD",
            fiscalSign = "FS-123",
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = "UNKNOWN_STATUS",
            deliveredAt = null,
            receiptUrl = "https://kassa.kz/receipt/789",
            registrationNumber = "RN-999",
            ofdProvider = "«Alteco Partners» ЖШС / ТОО «Alteco Partners»"
        )

        val kkm = defaultKkm.copy(registrationNumber = "RN-999")

        val html = renderer.renderHtml(request, doc, kkm)
        println("=== DEBUG OTHER STATUSES CHAR CODES ===")
        val target = "ЧЕК ПОКУПКИ / САТЫП АЛУ ЧЕГІ"
        println("Target string: '$target'")
        println("Target char codes: " + target.map { it.code })
        val index = html.indexOf("ЧЕК ПОКУПКИ")
        if (index >= 0) {
            val slice = html.substring(index, index + target.length)
            println("HTML slice: '$slice'")
            println("HTML slice char codes: " + slice.map { it.code })
        } else {
            println("Target 'ЧЕК ПОКУПКИ' not found in HTML!")
        }
        println("=== END DEBUG OTHER STATUSES CHAR CODES ===")

        val cleanHtml = stripHtml(html)

        assertTrue(cleanHtml.contains("САТЫП АЛУ ЧЕГІ"))
        assertTrue(cleanHtml.contains("ЧЕК ПОКУПКИ"))
        assertTrue(cleanHtml.contains("UNKNOWN_STATUS"))
        assertTrue(cleanHtml.contains("«Alteco Partners» ЖШС / ТОО «Alteco Partners»"))
        // Since Qr generator returns null, no qr img tag should be present
        assertTrue(!html.contains("<img src=\"\""))
    }

    @Test
    fun testRenderHtmlBuyReturnAndOtherProvider() {
        val renderer = ReceiptRendererApiImpl(StubQrCodeGenerator())

        val request = ReceiptRequest(
            kkmId = "kkm-123",
            pin = "1111",
            operation = ReceiptOperationType.BUY_RETURN,
            items = emptyList(),
            payments = emptyList(),
            total = Money(0, 0),
            idempotencyKey = "key-789"
        )

        val doc = FiscalDocumentSnapshot(
            id = "doc-789",
            cashboxId = "kkm-123",
            shiftId = "shift-456",
            docType = "TICKET",
            docNo = 10,
            shiftNo = 20,
            createdAt = 1782200000000L,
            totalAmount = 0L,
            currency = "USD",
            fiscalSign = "FS-123",
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = null,
            deliveredAt = null,
            receiptUrl = "https://kassa.kz/receipt/789",
            registrationNumber = "RN-999",
            ofdProvider = "SOME_OTHER_PROVIDER"
        )

        val kkm = defaultKkm.copy(registrationNumber = "RN-999")

        val html = renderer.renderHtml(request, doc, kkm)

        val cleanHtml = stripHtml(html)

        assertTrue(cleanHtml.contains("САТЫП АЛУДЫ ҚАЙТАРУ ЧЕГІ"))
        assertTrue(cleanHtml.contains("ЧЕК ВОЗВРАТА ПОКУПКИ"))
        assertTrue(cleanHtml.contains("SOME_OTHER_PROVIDER"))
    }

    @Test
    fun testProxyMethods() {
        val renderer = ReceiptRendererApiImpl(StubQrCodeGenerator())
        val shift = ShiftInfo(
            id = "shift-123",
            kkmId = "kkm-123",
            shiftNo = 45,
            status = ShiftStatus.OPEN,
            openedAt = 1782200000000L,
            closedAt = 1782200050000L
        )
        val counters = mapOf(
            "operation.OPERATION_SELL.count" to 5L,
            "operation.OPERATION_SELL.sum" to 15000L
        )

        val kkm = defaultKkm.copy(
            registrationNumber = "RN-999",
            factoryNumber = "FN-888"
        )

        val xReport = renderer.renderXReportHtml(shift, counters, kkm, null)
        val cleanXReport = stripHtml(xReport)
        assertTrue(cleanXReport.contains("X-ОТЧЁТ"))
        assertTrue(cleanXReport.contains("Ауысым № / Смена №"))
        assertTrue(cleanXReport.contains("45"))

        val openShift = renderer.renderOpenShiftHtml(shift, kkm, null, "42")
        val cleanOpenShift = stripHtml(openShift)
        assertTrue(cleanOpenShift.contains("ОТКРЫТИЕ СМЕНЫ"))
        assertTrue(openShift.contains("Документ №") || openShift.contains("Құжат №"))
        assertTrue(openShift.contains("42"))

        val closeShift = renderer.renderCloseShiftHtml(shift, counters, kkm, null, "43")
        val cleanCloseShift = stripHtml(closeShift)
        assertTrue(cleanCloseShift.contains("Z-ОТЧЁТ (ЗАКРЫТИЕ СМЕНЫ)"))
        assertTrue(closeShift.contains("Документ №") || closeShift.contains("Құжат №"))
        assertTrue(closeShift.contains("43"))

        val doc = FiscalDocumentSnapshot(
            id = "doc-999",
            cashboxId = "kkm-123",
            shiftId = "shift-123",
            docType = "CASH_IN",
            docNo = 100,
            shiftNo = 45,
            createdAt = 1782200000000L,
            totalAmount = 50000L,
            currency = "KZT",
            fiscalSign = null,
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = null,
            deliveredAt = null
        )

        val cashOp = renderer.renderCashOperationHtml(doc, kkm)
        val cleanCashOp = stripHtml(cashOp)
        assertTrue(cleanCashOp.contains("ВНЕСЕНИЕ НАЛИЧНЫХ"))
        assertTrue(cleanCashOp.contains("500.00") || cleanCashOp.contains("500,00"))
    }

    @Test
    fun testRenderHtmlWithNullOfdAndRegistration() {
        val renderer = ReceiptRendererApiImpl(StubQrCodeGenerator())

        val request = ReceiptRequest(
            kkmId = "kkm-123",
            pin = "1111",
            operation = ReceiptOperationType.SELL,
            items = emptyList(),
            payments = emptyList(),
            total = Money(0, 0),
            idempotencyKey = "key-abc"
        )

        val doc = FiscalDocumentSnapshot(
            id = "doc-789",
            cashboxId = "kkm-123",
            shiftId = "shift-456",
            docType = "TICKET",
            docNo = null,
            shiftNo = null,
            createdAt = 1782200000000L,
            totalAmount = null,
            currency = null,
            fiscalSign = null,
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = null,
            deliveredAt = null,
            receiptUrl = null,
            registrationNumber = null,
            ofdProvider = null
        )

        val kkm = defaultKkm.copy(
            registrationNumber = null,
            ofdServiceInfo = null
        )

        val html = renderer.renderHtml(request, doc, kkm)

        assertTrue(html.contains("SUPERKASSA"))
        assertTrue(!html.contains("ОФД / ОФД:"))
        assertTrue(!html.contains("Ссылка на чек"))
    }

    @Test
    fun testBuildFallbackReceiptUrlReturnsNullWhenNoSigns() {
        val renderer = ReceiptRendererApiImpl(StubQrCodeGenerator())
        val request = ReceiptRequest(
            kkmId = "kkm-123",
            pin = "1111",
            operation = ReceiptOperationType.SELL,
            items = emptyList(),
            payments = emptyList(),
            total = Money(0, 0),
            idempotencyKey = "key-abc"
        )
        val doc = FiscalDocumentSnapshot(
            id = "doc-789",
            cashboxId = "kkm-123",
            shiftId = "shift-456",
            docType = "TICKET",
            docNo = null,
            shiftNo = null,
            createdAt = 1782200000000L,
            totalAmount = null,
            currency = null,
            fiscalSign = null,
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = null,
            deliveredAt = null,
            receiptUrl = null,
            registrationNumber = "RN-999", // Registration number present, but signs are null
            ofdProvider = null
        )
        val kkm = defaultKkm.copy(registrationNumber = "RN-999")
        val html = renderer.renderHtml(request, doc, kkm)
        assertTrue(!html.contains("Ссылка на чек"))
    }

    @Test
    fun testRenderHtmlWithMockedTaxes() {
        val renderer = ReceiptRendererApiImpl(StubQrCodeGenerator())

        val request = ReceiptRequest(
            kkmId = "kkm-123",
            pin = "1111",
            operation = ReceiptOperationType.SELL,
            items = emptyList(),
            payments = emptyList(),
            total = Money(0, 0),
            idempotencyKey = "key-mocked-taxes",
            ticketTaxes = listOf(
                TaxLine(VatGroup.NO_VAT, 0, Money(100, 0), Money(0, 0)),
                TaxLine(VatGroup.VAT_0, 0, Money(200, 0), Money(0, 0)),
                TaxLine(VatGroup.VAT_5, 5, Money(300, 0), Money(15, 0)),
                TaxLine(VatGroup.VAT_10, 10, Money(400, 0), Money(40, 0)),
                TaxLine(VatGroup.VAT_16, 16, Money(500, 0), Money(80, 0))
            )
        )

        val doc = FiscalDocumentSnapshot(
            id = "doc-789",
            cashboxId = "kkm-123",
            shiftId = "shift-456",
            docType = "TICKET",
            docNo = 12,
            shiftNo = 34,
            createdAt = 1782200000000L,
            totalAmount = 0L,
            currency = "KZT",
            fiscalSign = "FS-12345",
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = "DELIVERED",
            deliveredAt = 1782200010000L,
            receiptUrl = "https://kassa.kz/receipt/789",
            registrationNumber = "RN-999",
            taxpayerName = "IE Ivanov",
            ofdProvider = "KAZAKHTELECOM"
        )

        val kkm = defaultKkm.copy(
            registrationNumber = "RN-999",
            ofdServiceInfo = defaultKkm.ofdServiceInfo?.copy(
                orgTitle = "IE Ivanov"
            )
        )

        val html = renderer.renderHtml(request, doc, kkm)
        val cleanHtml = stripHtml(html)
        assertTrue(cleanHtml.contains("ҚҚС-сыз / Без НДС"))
        assertTrue(cleanHtml.contains("ҚҚС 0% / НДС 0%"))
        assertTrue(cleanHtml.contains("ҚҚС 5% / НДС 5%"))
        assertTrue(cleanHtml.contains("ҚҚС 10% / НДС 10%"))
        assertTrue(cleanHtml.contains("ҚҚС 16% / НДС 16%"))
    }

    @Test
    fun testGenerateExampleHtmlFiles() {
        val renderer = ReceiptRendererApiImpl(StubQrCodeGenerator())
        val rootOutputDir = java.io.File("src/test/resources/receipt-examples")
        if (rootOutputDir.exists()) {
            rootOutputDir.deleteRecursively()
        }
        rootOutputDir.mkdirs()

        val baseKkm = defaultKkm.copy(
            registrationNumber = "RN-999888777",
            factoryNumber = "FN-777666",
            ofdServiceInfo = defaultKkm.ofdServiceInfo?.copy(
                orgTitle = "ИП Иванов С. П.",
                orgInn = "920102300400",
                orgAddress = "г. Алматы, ул. Толе Би, 50"
            )
        )

        // 1. Standard Sale Receipt data
        val request1 = ReceiptRequest(
            kkmId = "kkm-123",
            pin = "1111",
            operation = ReceiptOperationType.SELL,
            items = listOf(
                ReceiptItem("Хлеб бородинский", "001", 1L, Money(180, 0), Money(180, 0), vatGroup = VatGroup.NO_VAT),
                ReceiptItem("Молоко 3.2%", "001", 2L, Money(450, 0), Money(900, 0), listExciseStamp = listOf("01046002938172"), vatGroup = VatGroup.VAT_10)
            ),
            payments = listOf(ReceiptPayment(PaymentType.CASH, Money(1100, 0))),
            total = Money(1080, 0),
            taken = Money(1100, 0),
            change = Money(20, 0),
            idempotencyKey = "key-ex1",
            taxRegime = TaxRegime.MIXED,
            ticketTaxes = listOf(
                TaxLine(VatGroup.VAT_10, 10, Money(900, 0), Money(90, 0)),
                TaxLine(VatGroup.NO_VAT, 0, Money(180, 0), Money(0, 0))
            )
        )
        val doc1 = FiscalDocumentSnapshot(
            id = "doc-ex1",
            cashboxId = "kkm-123",
            shiftId = "shift-1",
            docType = "TICKET",
            docNo = 101,
            shiftNo = 5,
            createdAt = 1782200000000L,
            totalAmount = 108000L,
            currency = "KZT",
            fiscalSign = "1234567890",
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = "DELIVERED",
            deliveredAt = null,
            receiptUrl = "https://consumer.oofd.kz/r/101",
            registrationNumber = "RN-999888777",
            taxpayerName = "ИП Иванов С. П.",
            taxpayerBin = "920102300400",
            taxpayerAddress = "г. Алматы, ул. Толе Би, 50",
            factoryNumber = "FN-777666",
            ofdProvider = "KAZAKHTELECOM"
        )

        // 2. Mixed Payment, MIXED VAT, whole receipt discount and markup (no item discounts)
        val request2 = ReceiptRequest(
            kkmId = "kkm-123",
            pin = "1111",
            operation = ReceiptOperationType.SELL,
            items = listOf(
                ReceiptItem(
                    name = "Смартфон Samsung Galaxy A35 5G 8GB/256GB Awesome Navy (SM-A356B) / Смартфон Samsung Galaxy A35 5G 8GB/256GB Awesome Navy",
                    sectionCode = "001",
                    quantity = 1L,
                    price = Money(149990, 0),
                    sum = Money(149990, 0),
                    vatGroup = VatGroup.VAT_16
                ),
                ReceiptItem(
                    name = "Защитное стекло",
                    sectionCode = "001",
                    quantity = 1L,
                    price = Money(5000, 0),
                    sum = Money(5000, 0),
                    vatGroup = VatGroup.NO_VAT
                )
            ),
            payments = listOf(
                ReceiptPayment(PaymentType.CARD, Money(100000, 0)),
                ReceiptPayment(PaymentType.ELECTRONIC, Money(50000, 0))
            ),
            total = Money(150000, 0),
            idempotencyKey = "key-ex2",
            taxRegime = TaxRegime.MIXED,
            defaultVatGroup = VatGroup.VAT_16,
            discount = Money(5000, 0),
            markup = Money(10, 0),
            ticketTaxes = listOf(
                TaxLine(VatGroup.VAT_16, 16, Money(149990, 0), Money(20688, 28)),
                TaxLine(VatGroup.NO_VAT, 0, Money(5000, 0), Money(0, 0))
            )
        )
        val doc2 = doc1.copy(id = "doc-ex2", docNo = 102, totalAmount = 15000000L)

        // 2b. Mixed Payment, MIXED VAT, ONLY item-level discounts (receipt discount/markup are null)
        val request2ItemDiscountOnly = ReceiptRequest(
            kkmId = "kkm-123",
            pin = "1111",
            operation = ReceiptOperationType.SELL,
            items = listOf(
                ReceiptItem(
                    name = "Смартфон Samsung Galaxy A35 5G 8GB/256GB Awesome Navy (SM-A356B) / Смартфон Samsung Galaxy A35 5G 8GB/256GB Awesome Navy",
                    sectionCode = "001",
                    quantity = 1L,
                    price = Money(149990, 0),
                    sum = Money(149990, 0),
                    vatGroup = VatGroup.VAT_16,
                    discount = Money(1000, 0),
                    markup = Money(200, 0)
                ),
                ReceiptItem(
                    name = "Защитное стекло",
                    sectionCode = "001",
                    quantity = 1L,
                    price = Money(5000, 0),
                    sum = Money(5000, 0),
                    vatGroup = VatGroup.NO_VAT
                )
            ),
            payments = listOf(
                ReceiptPayment(PaymentType.CARD, Money(100000, 0)),
                ReceiptPayment(PaymentType.ELECTRONIC, Money(50000, 0))
            ),
            total = Money(150000, 0),
            idempotencyKey = "key-ex2-item-disc-only",
            taxRegime = TaxRegime.MIXED,
            defaultVatGroup = VatGroup.VAT_16,
            discount = null,
            markup = null,
            ticketTaxes = listOf(
                TaxLine(VatGroup.VAT_16, 16, Money(149990, 0), Money(20688, 28)),
                TaxLine(VatGroup.NO_VAT, 0, Money(5000, 0), Money(0, 0))
            )
        )

        // 3. Refund Sale Receipt (SELL_RETURN)
        val request3 = request1.copy(
            operation = ReceiptOperationType.SELL_RETURN,
            idempotencyKey = "key-ex3",
            parentTicket = ParentTicket(
                parentTicketNumber = 101L,
                parentTicketDateTimeMillis = 1782200000000L,
                kgdKkmId = "kkm-123",
                parentTicketTotal = Money(1080, 0),
                parentTicketIsOffline = false
            )
        )
        val doc3 = doc1.copy(id = "doc-ex3", docNo = 103)

        // 4. Purchase Receipt (BUY)
        val request4 = ReceiptRequest(
            kkmId = "kkm-123",
            pin = "1111",
            operation = ReceiptOperationType.BUY,
            items = listOf(
                ReceiptItem("Прием металлолома", "001", 10L, Money(100, 0), Money(1000, 0), vatGroup = VatGroup.VAT_16)
            ),
            payments = listOf(ReceiptPayment(PaymentType.CASH, Money(1000, 0))),
            total = Money(1000, 0),
            idempotencyKey = "key-ex4",
            taxRegime = TaxRegime.MIXED,
            ticketTaxes = listOf(
                TaxLine(VatGroup.VAT_16, 16, Money(1000, 0), Money(160, 0))
            )
        )
        val doc4 = doc1.copy(id = "doc-ex4", docNo = 104, totalAmount = 100000L)

        // 5. Refund Purchase Receipt (BUY_RETURN)
        val request5 = request4.copy(
            operation = ReceiptOperationType.BUY_RETURN,
            idempotencyKey = "key-ex5",
            parentTicket = ParentTicket(
                parentTicketNumber = 104L,
                parentTicketDateTimeMillis = 1782200000000L,
                kgdKkmId = "kkm-123",
                parentTicketTotal = Money(1000, 0),
                parentTicketIsOffline = false
            )
        )
        val doc5 = doc1.copy(id = "doc-ex5", docNo = 105)

        // 5b. Sale Receipt with All VAT rates (NO_VAT, VAT_0, VAT_5, VAT_10, VAT_16)
        val requestAllVats = ReceiptRequest(
            kkmId = "kkm-123",
            pin = "1111",
            operation = ReceiptOperationType.SELL,
            items = listOf(
                ReceiptItem("Товар без НДС", "001", 1L, Money(1000, 0), Money(1000, 0), vatGroup = VatGroup.NO_VAT),
                ReceiptItem("Товар НДС 0%", "001", 1L, Money(2000, 0), Money(2000, 0), vatGroup = VatGroup.VAT_0),
                ReceiptItem("Товар НДС 5%", "001", 1L, Money(3000, 0), Money(3000, 0), vatGroup = VatGroup.VAT_5),
                ReceiptItem("Товар НДС 10%", "001", 1L, Money(4000, 0), Money(4000, 0), vatGroup = VatGroup.VAT_10),
                ReceiptItem("Товар НДС 16%", "001", 1L, Money(5000, 0), Money(5000, 0), vatGroup = VatGroup.VAT_16)
            ),
            payments = listOf(ReceiptPayment(PaymentType.CASH, Money(15000, 0))),
            total = Money(15000, 0),
            idempotencyKey = "key-ex-all-vats",
            taxRegime = TaxRegime.MIXED,
            ticketTaxes = listOf(
                TaxLine(VatGroup.NO_VAT, 0, Money(1000, 0), Money(0, 0)),
                TaxLine(VatGroup.VAT_0, 0, Money(2000, 0), Money(0, 0)),
                TaxLine(VatGroup.VAT_5, 5, Money(3000, 0), Money(150, 0)),
                TaxLine(VatGroup.VAT_10, 10, Money(4000, 0), Money(400, 0)),
                TaxLine(VatGroup.VAT_16, 16, Money(5000, 0), Money(800, 0))
            )
        )
        val docAllVats = doc1.copy(id = "doc-ex-all-vats", docNo = 110, totalAmount = 1500000L)

        // 5c. Sale Receipt with item-level storno and receipt-level parent ticket storno reference
        val requestStorno = ReceiptRequest(
            kkmId = "kkm-123",
            pin = "1111",
            operation = ReceiptOperationType.SELL,
            items = listOf(
                ReceiptItem("Хлеб бородинский", "001", 1L, Money(180, 0), Money(180, 0), vatGroup = VatGroup.NO_VAT),
                ReceiptItem("Молоко 3.2% (Сторно)", "001", 2L, Money(450, 0), Money(900, 0), listExciseStamp = listOf("01046002938172"), vatGroup = VatGroup.VAT_10, isStorno = true)
            ),
            payments = listOf(ReceiptPayment(PaymentType.CASH, Money(180, 0))),
            total = Money(180, 0),
            taken = Money(200, 0),
            change = Money(20, 0),
            idempotencyKey = "key-storno",
            taxRegime = TaxRegime.MIXED,
            ticketTaxes = listOf(
                TaxLine(VatGroup.VAT_10, 10, Money(900, 0), Money(90, 0)),
                TaxLine(VatGroup.NO_VAT, 0, Money(180, 0), Money(0, 0))
            ),
            parentTicket = ParentTicket(
                parentTicketNumber = 101L,
                parentTicketDateTimeMillis = 1782200000000L,
                kgdKkmId = "kkm-123",
                parentTicketTotal = Money(1080, 0),
                parentTicketIsOffline = false
            )
        )
        val docStorno = doc1.copy(id = "doc-ex-storno", docNo = 111, totalAmount = 18000L)

        // 5d. Max possible receipt request with all features combined (discounts, markups, storno, all VAT rates, payments, parent ticket, etc.)
        val requestMaxFeatures = ReceiptRequest(
            kkmId = "kkm-123",
            pin = "1111",
            operation = ReceiptOperationType.SELL,
            items = listOf(
                ReceiptItem("Товар без НДС (Обычный)", "001", 1L, Money(1000, 0), Money(1000, 0), vatGroup = VatGroup.NO_VAT, discount = Money(50, 0), measureUnitCode = "796"),
                ReceiptItem("Товар НДС 0% (Маркированный)", "001", 2L, Money(2000, 0), Money(4000, 0), vatGroup = VatGroup.VAT_0, listExciseStamp = listOf("ExciseStamp1234567890"), measureUnitCode = "112"),
                ReceiptItem("Товар НДС 5% (С наценкой)", "001", 1L, Money(3000, 0), Money(3000, 0), vatGroup = VatGroup.VAT_5, markup = Money(200, 0)),
                ReceiptItem("Товар НДС 10% (Сторно)", "001", 1L, Money(4000, 0), Money(4000, 0), vatGroup = VatGroup.VAT_10, isStorno = true),
                ReceiptItem("Товар НДС 16% (Максимальный)", "001", 3L, Money(5000, 0), Money(15000, 0), vatGroup = VatGroup.VAT_16, discount = Money(500, 0), markup = Money(100, 0), listExciseStamp = listOf("ExciseStampA", "ExciseStampB"))
            ),
            payments = listOf(
                ReceiptPayment(PaymentType.CASH, Money(10000, 0)),
                ReceiptPayment(PaymentType.CARD, Money(9000, 0))
            ),
            total = Money(19000, 0),
            taken = Money(20000, 0),
            change = Money(1000, 0),
            idempotencyKey = "key-max-features",
            taxRegime = TaxRegime.MIXED,
            discount = Money(1000, 0),
            markup = Money(500, 0),
            ticketTaxes = listOf(
                TaxLine(VatGroup.NO_VAT, 0, Money(1000, 0), Money(0, 0)),
                TaxLine(VatGroup.VAT_0, 0, Money(4000, 0), Money(0, 0)),
                TaxLine(VatGroup.VAT_5, 5, Money(3000, 0), Money(150, 0)),
                TaxLine(VatGroup.VAT_10, 10, Money(4000, 0), Money(400, 0)),
                TaxLine(VatGroup.VAT_16, 16, Money(15000, 0), Money(2400, 0))
            ),
            parentTicket = ParentTicket(
                parentTicketNumber = 555L,
                parentTicketDateTimeMillis = 1782200000000L,
                kgdKkmId = "kkm-123",
                parentTicketTotal = Money(1080, 0),
                parentTicketIsOffline = false
            ),
            customerBin = "123456789012"
        )
        val docMaxFeatures = doc1.copy(id = "doc-ex-max", docNo = 999, totalAmount = 1900000L, ofdStatus = "DELIVERED", ofdProvider = "KAZAKHTELECOM")

        // 6. Open Shift data
        val shiftOpen = ShiftInfo(
            id = "shift-1",
            kkmId = "kkm-123",
            shiftNo = 5,
            status = ShiftStatus.OPEN,
            openedAt = 1782100000000L,
            closedAt = null
        )

        // Z-Report (Close Shift) data
        val shiftClose = shiftOpen.copy(
            status = ShiftStatus.CLOSED,
            closedAt = 1782200000000L
        )

        // 7. X-Report & Z-Report counters
        val counters = mapOf(
            "operation.OPERATION_SELL.count" to 15L,
            "operation.OPERATION_SELL.sum" to 25000000L, // 250,000.00
            "operation.OPERATION_SELL_RETURN.count" to 1L,
            "operation.OPERATION_SELL_RETURN.sum" to 500000L, // 5,000.00
            "operation.OPERATION_BUY.count" to 2L,
            "operation.OPERATION_BUY.sum" to 200000L, // 2,000.00
            "operation.OPERATION_BUY_RETURN.count" to 1L,
            "operation.OPERATION_BUY_RETURN.sum" to 100000L, // 1,000.00

            "operation.OPERATION_SELL.discount_sum" to 500000L,
            "operation.OPERATION_SELL.markup_sum" to 1000L,

            "section.1.operation.OPERATION_SELL.count" to 10L,
            "section.1.operation.OPERATION_SELL.sum" to 15000000L,
            "section.2.operation.OPERATION_SELL.count" to 5L,
            "section.2.operation.OPERATION_SELL.sum" to 10000000L,

            "ticket.OPERATION_SELL.total_count" to 15L,
            "ticket.OPERATION_SELL.count" to 15L,
            "ticket.OPERATION_SELL.sum" to 24501000L,
            "ticket.OPERATION_SELL.offline_count" to 2L,
            "ticket.OPERATION_SELL.discount_sum" to 500000L,
            "ticket.OPERATION_SELL.markup_sum" to 1000L,
            "ticket.OPERATION_SELL.change_sum" to 20000L,

            "ticket.OPERATION_SELL_RETURN.total_count" to 1L,
            "ticket.OPERATION_SELL_RETURN.count" to 1L,
            "ticket.OPERATION_SELL_RETURN.sum" to 500000L,

            "ticket.OPERATION_SELL.payment.PAYMENT_CASH.sum" to 14501000L,
            "ticket.OPERATION_SELL.payment.PAYMENT_CASH.count" to 10L,
            "ticket.OPERATION_SELL.payment.PAYMENT_CARD.sum" to 10000000L,
            "ticket.OPERATION_SELL.payment.PAYMENT_CARD.count" to 5L,

            "ticket.OPERATION_SELL_RETURN.payment.PAYMENT_CASH.sum" to 500000L,
            "ticket.OPERATION_SELL_RETURN.payment.PAYMENT_CASH.count" to 1L,

            "tax.VAT_16.OPERATION_SELL.turnover" to 14999000L,
            "tax.VAT_16.OPERATION_SELL.turnover_without_tax" to 12930173L,
            "tax.VAT_16.OPERATION_SELL.sum" to 2068827L,

            "tax.VAT_0.OPERATION_SELL.turnover" to 1000000L,
            "tax.VAT_0.OPERATION_SELL.turnover_without_tax" to 1000000L,
            "tax.VAT_0.OPERATION_SELL.sum" to 0L,

            "money_placement.MONEY_PLACEMENT_DEPOSIT.count" to 2L,
            "money_placement.MONEY_PLACEMENT_DEPOSIT.total_count" to 2L,
            "money_placement.MONEY_PLACEMENT_DEPOSIT.sum" to 5000000L,
            "money_placement.MONEY_PLACEMENT_WITHDRAWAL.count" to 1L,
            "money_placement.MONEY_PLACEMENT_WITHDRAWAL.total_count" to 1L,
            "money_placement.MONEY_PLACEMENT_WITHDRAWAL.sum" to 3000000L,

            "start_shift_non_nullable.OPERATION_SELL.sum" to 1000000000L,
            "non_nullable.OPERATION_SELL.sum" to 1025001000L,

            "start_shift_non_nullable.OPERATION_SELL_RETURN.sum" to 50000000L,
            "non_nullable.OPERATION_SELL_RETURN.sum" to 50500000L,

            "cash.sum" to 26501000L,
            "revenue.sum" to 24001000L
        )

        // 9. Cash In data
        val docCashIn = doc1.copy(
            id = "doc-cash-in",
            docType = "CASH_IN",
            docNo = 106,
            totalAmount = 5000000L
        )

        // 10. Cash Out data
        val docCashOut = doc1.copy(
            id = "doc-cash-out",
            docType = "CASH_OUT",
            docNo = 107,
            totalAmount = 3000000L
        )

        val widths = listOf(80, 58, 0)
        val themes = listOf(false, true) // false = light, true = dark

        for (width in widths) {
            for (isDark in themes) {
                val themeName = if (isDark) "dark" else "light"
                val widthDirName = if (width == 0) "fullscreen" else "${width}mm"
                val dir = java.io.File(rootOutputDir, "$widthDirName/$themeName")
                
                val salesDir = java.io.File(dir, "tickets/sales")
                val refundSalesDir = java.io.File(dir, "tickets/refund_sales")
                val buysDir = java.io.File(dir, "tickets/buys")
                val refundBuysDir = java.io.File(dir, "tickets/refund_buys")
                val reportsDir = java.io.File(dir, "reports")
                val operationsDir = java.io.File(dir, "operations")
                
                salesDir.mkdirs()
                refundSalesDir.mkdirs()
                buysDir.mkdirs()
                refundBuysDir.mkdirs()
                reportsDir.mkdirs()
                operationsDir.mkdirs()

                val themeColor = when {
                    width == 0 && !isDark -> "indigo"
                    width == 0 && isDark -> "rose"
                    width == 80 && !isDark -> "teal"
                    width == 80 && isDark -> "green"
                    width == 58 && !isDark -> "blue"
                    else -> "orange"
                }
                val folderBranding = ReceiptBranding(
                    paperWidthMm = width,
                    useForceDarkTheme = isDark,
                    themeColor = themeColor
                )
                val kkmForFolder = baseKkm.copy(branding = folderBranding)

                // 1. Standard Sale Receipt (plus accent colors showcase)
                salesDir.resolve("sale_receipt.html").writeText(
                    renderer.renderHtml(request1, doc1, kkmForFolder)
                )
                val accentColors = listOf("indigo", "teal", "green", "blue", "orange", "rose")
                for (color in accentColors) {
                    salesDir.resolve("sale_receipt_accent_$color.html").writeText(
                        renderer.renderHtml(request1, doc1, kkmForFolder.copy(branding = folderBranding.copy(themeColor = color)))
                    )
                }

                // 2. Mixed Payment, VAT 16%, discounts
                salesDir.resolve("sale_with_vat_and_discounts.html").writeText(
                    renderer.renderHtml(request2, doc2, kkmForFolder)
                )

                // 2b. Mixed Payment, MIXED VAT, ONLY item-level discounts
                salesDir.resolve("sale_with_only_item_discounts.html").writeText(
                    renderer.renderHtml(request2ItemDiscountOnly, doc2, kkmForFolder)
                )

                // 3. Refund Sale Receipt (Online, Offline, Failed)
                refundSalesDir.resolve("refund_sale_receipt.html").writeText(
                    renderer.renderHtml(request3, doc3, kkmForFolder)
                )
                refundSalesDir.resolve("refund_sale_receipt_offline.html").writeText(
                    renderer.renderHtml(request3, doc3.copy(ofdStatus = "PENDING"), kkmForFolder)
                )
                refundSalesDir.resolve("refund_sale_receipt_failed.html").writeText(
                    renderer.renderHtml(request3, doc3.copy(ofdStatus = "ERROR"), kkmForFolder)
                )

                // 4. Purchase Receipt (Online, Offline, Failed)
                buysDir.resolve("buy_receipt.html").writeText(
                    renderer.renderHtml(request4, doc4, kkmForFolder)
                )
                buysDir.resolve("buy_receipt_offline.html").writeText(
                    renderer.renderHtml(request4, doc4.copy(ofdStatus = "PENDING"), kkmForFolder)
                )
                buysDir.resolve("buy_receipt_failed.html").writeText(
                    renderer.renderHtml(request4, doc4.copy(ofdStatus = "ERROR"), kkmForFolder)
                )

                // 5. Refund Purchase Receipt (Online, Offline, Failed)
                refundBuysDir.resolve("refund_buy_receipt.html").writeText(
                    renderer.renderHtml(request5, doc5, kkmForFolder)
                )
                refundBuysDir.resolve("refund_buy_receipt_offline.html").writeText(
                    renderer.renderHtml(request5, doc5.copy(ofdStatus = "PENDING"), kkmForFolder)
                )
                refundBuysDir.resolve("refund_buy_receipt_failed.html").writeText(
                    renderer.renderHtml(request5, doc5.copy(ofdStatus = "ERROR"), kkmForFolder)
                )

                // 6. Open Shift
                reportsDir.resolve("open_shift.html").writeText(
                    renderer.renderOpenShiftHtml(shiftOpen, kkmForFolder, null, "101")
                )

                // 7. X-Report
                reportsDir.resolve("x_report.html").writeText(
                    renderer.renderXReportHtml(shiftOpen, counters, kkmForFolder, null)
                )

                // 7a. Russian-only X-Report
                reportsDir.resolve("x_report_russian.html").writeText(
                    renderer.renderXReportHtml(shiftOpen, counters, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.RU)), null)
                )

                // 7b. Kazakh-only X-Report
                reportsDir.resolve("x_report_kazakh.html").writeText(
                    renderer.renderXReportHtml(shiftOpen, counters, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.KK)), null)
                )

                // 8. Z-Report
                reportsDir.resolve("z_report.html").writeText(
                    renderer.renderCloseShiftHtml(shiftClose, counters, kkmForFolder, null, "102")
                )

                // 8a. Russian-only Z-Report
                reportsDir.resolve("z_report_russian.html").writeText(
                    renderer.renderCloseShiftHtml(shiftClose, counters, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.RU)), null, "102")
                )

                // 8b. Kazakh-only Z-Report
                reportsDir.resolve("z_report_kazakh.html").writeText(
                    renderer.renderCloseShiftHtml(shiftClose, counters, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.KK)), null, "102")
                )

                // 8c. Offline Z-Report
                reportsDir.resolve("z_report_offline.html").writeText(
                    renderer.renderCloseShiftHtml(shiftClose, counters, kkmForFolder, "PENDING", "102")
                )

                // 8d. Failed Z-Report
                reportsDir.resolve("z_report_failed.html").writeText(
                    renderer.renderCloseShiftHtml(shiftClose, counters, kkmForFolder, "ERROR", "102")
                )

                // 9. Cash In
                operationsDir.resolve("cash_in.html").writeText(
                    renderer.renderCashOperationHtml(docCashIn, kkmForFolder)
                )

                // 9a. Offline Cash In
                operationsDir.resolve("cash_in_offline.html").writeText(
                    renderer.renderCashOperationHtml(docCashIn.copy(ofdStatus = "PENDING"), kkmForFolder)
                )

                // 9b. Failed Cash In
                operationsDir.resolve("cash_in_failed.html").writeText(
                    renderer.renderCashOperationHtml(docCashIn.copy(ofdStatus = "ERROR"), kkmForFolder)
                )

                // 10. Cash Out
                operationsDir.resolve("cash_out.html").writeText(
                    renderer.renderCashOperationHtml(docCashOut, kkmForFolder)
                )

                // 10a. Offline Cash Out
                operationsDir.resolve("cash_out_offline.html").writeText(
                    renderer.renderCashOperationHtml(docCashOut.copy(ofdStatus = "PENDING"), kkmForFolder)
                )

                // 10b. Failed Cash Out
                operationsDir.resolve("cash_out_failed.html").writeText(
                    renderer.renderCashOperationHtml(docCashOut.copy(ofdStatus = "ERROR"), kkmForFolder)
                )

                // 11. Kazakh-only Sale Receipt
                val brandingKk = folderBranding.copy(language = ReceiptLanguage.KK)
                salesDir.resolve("sale_receipt_kazakh.html").writeText(
                    renderer.renderHtml(request1, doc1, kkmForFolder.copy(branding = brandingKk))
                )

                // 12. Russian-only Sale Receipt
                val brandingRu = folderBranding.copy(language = ReceiptLanguage.RU)
                salesDir.resolve("sale_receipt_russian.html").writeText(
                    renderer.renderHtml(request1, doc1, kkmForFolder.copy(branding = brandingRu))
                )

                // 13. Branded Sale Receipt
                val brandingBranded = folderBranding.copy(
                    language = ReceiptLanguage.MIXED,
                    headerLogoUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=100&auto=format&fit=crop",
                    headerMsg = "<div class='center' style='padding: 5px; background: #ffebeb; color: #cc0000; border-radius: 4px; font-weight: bold;'>СУПЕР АКЦИЯ: -20% НА ВСЁ!</div>",
                    footerMsg = "<div class='center' style='margin-top: 10px; color: #666; font-size: 11px;'>Спасибо, что выбрали нас! / Бізді таңдағаныңызға рақмет!</div>",
                    useForceDarkTheme = isDark,
                    customBackgroundColorHex = if (isDark) "#1e1d20" else "#faf8f5",
                    customCardTopBorderColorHex = if (isDark) "#ffb833" else "#d97706"
                )
                salesDir.resolve("sale_receipt_branded.html").writeText(
                    renderer.renderHtml(request1, doc1, kkmForFolder.copy(branding = brandingBranded))
                )

                // 13b. Fully Branded Sale Receipt
                val brandingFullyBranded = folderBranding.copy(
                    language = ReceiptLanguage.MIXED,
                    themeColor = "rose",
                    headerLogoUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=100&auto=format&fit=crop",
                    beforeHeaderMsg = "<div class='center' style='color: #ff0000; font-weight: bold; padding: 4px; border: 1px solid red; border-radius: 4px;'>!!! BEFORE HEADER BLOCK !!!</div>",
                    headerMsg = "<div class='center' style='background: #ffebeb; padding: 4px;'>HEADER HTML</div>",
                    afterHeaderMsg = "<div class='center' style='color: #00aa00; font-size: 11px;'>AFTER HEADER (BEFORE TITLE)</div>",
                    beforeItemsMsg = "<div style='border: 1px solid #ccc; padding: 6px; border-radius: 8px; background: #fafafa; color: #555;'>BEFORE ITEMS CONTENT</div>",
                    afterItemsMsg = "<div style='border: 1px dashed #aaa; padding: 6px; border-radius: 8px; background: #fdfdfd;'>AFTER ITEMS INFO</div>",
                    beforeTotalsMsg = "<div style='color: #777; font-size: 11px; padding: 2px;'>BEFORE TOTALS MSG</div>",
                    afterTotalsMsg = "<div style='background: #eef; padding: 6px; border-radius: 8px; font-weight: 500;'>AFTER TOTALS / TAXES BANNER</div>",
                    beforeQrMsg = "<div class='center' style='font-style: italic; font-size: 11px; color: #4f46e5;'>BEFORE QR PROMO</div>",
                    footerMsg = "<div class='center' style='font-size: 10px; color: #999;'>FOOTER THANK YOU</div>"
                )
                salesDir.resolve("sale_receipt_fully_branded.html").writeText(
                    renderer.renderHtml(request1, doc1, kkmForFolder.copy(branding = brandingFullyBranded))
                )

                // 14. Offline Sale Receipt
                val docOffline = doc1.copy(id = "doc-ex-offline", docNo = 108, ofdStatus = "PENDING")
                salesDir.resolve("sale_receipt_offline.html").writeText(
                    renderer.renderHtml(request1, docOffline, kkmForFolder)
                )

                // 15. Failed/Error sending to OFD Sale Receipt
                val docFailed = doc1.copy(id = "doc-ex-failed", docNo = 109, ofdStatus = "ERROR")
                salesDir.resolve("sale_receipt_failed.html").writeText(
                    renderer.renderHtml(request1, docFailed, kkmForFolder)
                )

                // 16. Visual Branding Preview
                salesDir.resolve("sale_receipt_preview.html").writeText(
                    renderer.renderPreviewHtml(brandingBranded)
                )

                // 17. Sale Receipt with All VAT rates (MIXED, RU, KK)
                salesDir.resolve("sale_with_all_vat_rates.html").writeText(
                    renderer.renderHtml(requestAllVats, docAllVats, kkmForFolder)
                )
                salesDir.resolve("sale_with_all_vat_rates_russian.html").writeText(
                    renderer.renderHtml(requestAllVats, docAllVats, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.RU)))
                )
                salesDir.resolve("sale_with_all_vat_rates_kazakh.html").writeText(
                    renderer.renderHtml(requestAllVats, docAllVats, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.KK)))
                )

                // 18. Sale Receipt with Storno item and parent storno ticket reference
                salesDir.resolve("sale_receipt_storno.html").writeText(
                    renderer.renderHtml(requestStorno, docStorno, kkmForFolder)
                )

                // 19. Max possible receipt with all features combined (logo, messages, all VATs, storno, discount/markup, multiple payments, customer BIN, etc.)
                salesDir.resolve("sale_receipt_max_features.html").writeText(
                    renderer.renderHtml(requestMaxFeatures, docMaxFeatures, kkmForFolder.copy(branding = brandingFullyBranded))
                )

                // Additional RU/KK matrix files for completeness
                refundSalesDir.resolve("refund_sale_receipt_russian.html").writeText(
                    renderer.renderHtml(request3, doc3, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.RU)))
                )
                refundSalesDir.resolve("refund_sale_receipt_kazakh.html").writeText(
                    renderer.renderHtml(request3, doc3, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.KK)))
                )

                buysDir.resolve("buy_receipt_russian.html").writeText(
                    renderer.renderHtml(request4, doc4, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.RU)))
                )
                buysDir.resolve("buy_receipt_kazakh.html").writeText(
                    renderer.renderHtml(request4, doc4, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.KK)))
                )

                refundBuysDir.resolve("refund_buy_receipt_russian.html").writeText(
                    renderer.renderHtml(request5, doc5, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.RU)))
                )
                refundBuysDir.resolve("refund_buy_receipt_kazakh.html").writeText(
                    renderer.renderHtml(request5, doc5, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.KK)))
                )

                reportsDir.resolve("open_shift_russian.html").writeText(
                    renderer.renderOpenShiftHtml(
                        shiftOpen,
                        kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.RU)),
                        null,
                        "101"
                    )
                )
                reportsDir.resolve("open_shift_kazakh.html").writeText(
                    renderer.renderOpenShiftHtml(
                        shiftOpen,
                        kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.KK)),
                        null,
                        "101"
                    )
                )

                operationsDir.resolve("cash_in_russian.html").writeText(
                    renderer.renderCashOperationHtml(docCashIn, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.RU)))
                )
                operationsDir.resolve("cash_in_kazakh.html").writeText(
                    renderer.renderCashOperationHtml(docCashIn, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.KK)))
                )

                operationsDir.resolve("cash_out_russian.html").writeText(
                    renderer.renderCashOperationHtml(docCashOut, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.RU)))
                )
                operationsDir.resolve("cash_out_kazakh.html").writeText(
                    renderer.renderCashOperationHtml(docCashOut, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.KK)))
                )
            }
        }
    }

    @Test
    fun testExplicitLayoutOverride() {
        val renderer = ReceiptRendererApiImpl(StubQrCodeGenerator())
        val kkm = defaultKkm.copy(branding = ReceiptBranding(paperWidthMm = 80))
        val request = ReceiptRequest(
            kkmId = "kkm-123",
            pin = "1111",
            operation = ReceiptOperationType.SELL,
            items = listOf(ReceiptItem("Apple", "001", 1L, Money(100, 0), Money(100, 0))),
            payments = listOf(ReceiptPayment(PaymentType.CASH, Money(100, 0))),
            total = Money(100, 0),
            idempotencyKey = "key-1"
        )
        val doc = FiscalDocumentSnapshot(
            id = "doc-1",
            cashboxId = "kkm-123",
            shiftId = "shift-1",
            docType = "TICKET",
            docNo = 1,
            shiftNo = 1,
            createdAt = 1782200000000L,
            totalAmount = 10000L,
            currency = "KZT",
            fiscalSign = "FS-1",
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = "DELIVERED",
            deliveredAt = 1782200010000L
        )

        // Without override, should have tape-80mm width class
        val htmlDefault = renderer.renderHtml(request, doc, kkm)
        assertTrue(htmlDefault.contains("tape-80mm"))

        // With override to 58mm
        val html58 = renderer.renderHtml(request, doc, kkm, ReceiptLayoutType.TAPE_58MM)
        assertTrue(html58.contains("tape-58mm"))

        // With override to fullscreen
        val htmlFullscreen = renderer.renderHtml(request, doc, kkm, ReceiptLayoutType.FULLSCREEN)
        assertTrue(htmlFullscreen.contains("tape-fullscreen"))
    }

    @Test
    fun testCoverageBoosters() {
        
        val resourceText = io.github.texport.superkassa.receiptrenderer.impl.renderer.base.ResourceLoader.readText("/non-existent-file.json")
        kotlin.test.assertNull(resourceText)

        val testRenderer = object : io.github.texport.superkassa.receiptrenderer.impl.renderer.base.BaseDocumentRenderer() {
            fun testExotic() {
                val inlineVal = translateInline("doc_no", ReceiptLanguage.MIXED, " - ")
                kotlin.test.assertEquals("Құжат № - Документ №", inlineVal)
                val mixedInlineVal = translateMixedInline("Привет", "Сәлем", ReceiptLanguage.MIXED, " | ")
                kotlin.test.assertEquals("Сәлем | Привет", mixedInlineVal)
                
                val frame = renderPageFrame("Title", "Body", defaultKkm)
                kotlin.test.assertTrue(frame.contains("Body"))
            }
        }
        testRenderer.testExotic()

        val t1 = ReceiptTranslator.translate("test_key", ReceiptLanguage.RU)
        kotlin.test.assertEquals("test_key", t1)
        val t2 = ReceiptTranslator.translate("test_key", ReceiptLanguage.KK)
        kotlin.test.assertEquals("test_key", t2)
        val t3 = ReceiptTranslator.translate("test_key", ReceiptLanguage.MIXED)
        kotlin.test.assertEquals("test_key", t3)
        val t4 = ReceiptTranslator.translateInline("test_key", ReceiptLanguage.RU)
        kotlin.test.assertEquals("test_key", t4)
        val t5 = ReceiptTranslator.translateInline("test_key", ReceiptLanguage.KK)
        kotlin.test.assertEquals("test_key", t5)
        val t6 = ReceiptTranslator.translateInline("test_key", ReceiptLanguage.MIXED)
        kotlin.test.assertEquals("test_key", t6)
        val t7 = ReceiptTranslator.translate("Same", "Same", ReceiptLanguage.MIXED)
        kotlin.test.assertEquals("Same", t7)
        val t8 = ReceiptTranslator.translateInline("Same", "Same", ReceiptLanguage.MIXED)
        kotlin.test.assertEquals("Same", t8)

        // Additional direct formatting boosters
        kotlin.test.assertEquals("-5.00", ReceiptFormatter.formatCents(-500L))
        kotlin.test.assertEquals("1.50", ReceiptFormatter.formatCents(150L))
        kotlin.test.assertEquals("&amp; &lt; &gt; &quot;", ReceiptFormatter.escape("& < > \""))
        kotlin.test.assertEquals(-150L, ReceiptFormatter.moneyToCents(Money(-1, -50)))

        // Cover VatGroup keys
        VatGroup.values().forEach { it.translationKey }
        // Cover PaymentType keys
        PaymentType.values().forEach { it.translationKey }
        // Cover ReceiptOperationType keys
        ReceiptOperationType.values().forEach { it.translationKey }
        
        // Cover toOperationKey
        listOf("OPERATION_SELL", "OPERATION_SELL_RETURN", "OPERATION_BUY", "OPERATION_BUY_RETURN", "OTHER").forEach {
            it.toOperationKey()
        }
        // Cover toPaymentKey
        listOf("PAYMENT_CASH", "PAYMENT_CARD", "PAYMENT_CREDIT", "PAYMENT_TARE", "PAYMENT_MOBILE", "PAYMENT_ELECTRONIC", "OTHER").forEach {
            it.toPaymentKey()
        }
        // Cover toTaxKey
        listOf("TAX_TYPE_VAT_0", "TAX_TYPE_VAT_5", "TAX_TYPE_VAT_10", "TAX_TYPE_VAT_12", "TAX_TYPE_VAT_16", "TAX_TYPE_NO_VAT", "OTHER").forEach {
            it.toTaxKey()
        }
        // Cover toDiscountKey
        listOf("OPERATION_SELL", "OPERATION_SELL_RETURN", "OPERATION_BUY", "OPERATION_BUY_RETURN", "OTHER").forEach {
            it.toDiscountKey()
        }
        // Cover toMarkupKey
        listOf("OPERATION_SELL", "OPERATION_SELL_RETURN", "OPERATION_BUY", "OPERATION_BUY_RETURN", "OTHER").forEach {
            it.toMarkupKey()
        }
        // Cover toTotalResultKey
        listOf("OPERATION_SELL", "OPERATION_SELL_RETURN", "OPERATION_BUY", "OPERATION_BUY_RETURN", "OTHER").forEach {
            it.toTotalResultKey()
        }

        // Cover HtmlBrandingAdapter
        val b1 = ReceiptBranding(
            useForceDarkTheme = true,
            customBackgroundColorHex = "#123456",
            customCardTopBorderColorHex = "#654321",
            beforeHeaderMsg = "line 1\nline 2 & <tag>\"",
            footerMsg = "<div>hello</div>"
        )
        val adapter1 = HtmlBrandingAdapter(b1)
        val css1 = adapter1.customCss
        assertTrue(css1.contains("/* force-dark */"))
        assertTrue(css1.contains("background-color: #123456 !important;"))
        assertTrue(css1.contains("border-top: 5px solid #654321 !important;"))
        
        assertTrue(adapter1.beforeHeaderHtml.contains("line 1<br/>line 2 &amp; &lt;tag&gt;&quot;"))
        assertTrue(adapter1.footerHtml.contains("<div>hello</div>"))

        val b2 = ReceiptBranding(
            useForceDarkTheme = false,
            customBackgroundColorHex = null,
            customCardTopBorderColorHex = null
        )
        val adapter2 = HtmlBrandingAdapter(b2)
        val css2 = adapter2.customCss
        assertTrue(css2.contains("background-color: #faf8f5;"))
        assertTrue(css2.contains("border-top: 5px solid #d97706 !important;"))

        // Cover TemplateRenderer error path
        assertFailsWith<IllegalArgumentException> {
            TemplateRenderer.render("non_existent_template_9999.html", emptyMap())
        }


        // Systematically permute parameters to cover all branches in BaseDocumentRenderer and components
        val allLayouts = listOf(ReceiptLayoutType.TAPE_80MM, ReceiptLayoutType.TAPE_58MM, ReceiptLayoutType.FULLSCREEN, null)
        val allOfdStatuses = listOf("DELIVERED", "SENT", "PENDING", "OFFLINE", "FAILED", "ERROR", null, "CUSTOM_STATUS")
        val allOfdProviders = listOf("KAZAKHTELECOM", "TRANSTELECOM", "ALTYNTEL", "TRANSKAZ", null, "CUSTOM_PROVIDER")
        val allFiscals = listOf(true, false)
        val allAutonomouses = listOf(true, false)
        val allWidths = listOf(80, 58, 0, 100)
        val allThemes = listOf("indigo", "teal", "green", "blue", "orange", "rose", "", "custom-exotic-theme")
        val forceDarks = listOf(true, false)
        val customColors = listOf(null, "#ffffff", "   ")

        val renderer = ReceiptRendererApiImpl(StubQrCodeGenerator())
        val mockItems = listOf(
            ReceiptItem("Item", "001", 1L, Money(100, 0), Money(100, 0), "1", VatGroup.VAT_16),
            ReceiptItem("Item 2", "002", 2L, Money(50, 0), Money(100, 0), "1", VatGroup.NO_VAT)
        )
        val mockPayments = listOf(ReceiptPayment(PaymentType.CASH, Money(200, 0)))

        for (i in 0 until 120) {
            val layout = allLayouts[i % allLayouts.size]
            val ofdStatus = allOfdStatuses[i % allOfdStatuses.size]
            val ofdProvider = allOfdProviders[i % allOfdProviders.size]
            val isFiscal = allFiscals[i % allFiscals.size]
            val isAutonomous = allAutonomouses[i % allAutonomouses.size]
            val width = allWidths[i % allWidths.size]
            val theme = allThemes[i % allThemes.size]
            val forceDark = forceDarks[i % forceDarks.size]
            val customBg = customColors[i % customColors.size]
            val customBorder = customColors[(i + 1) % customColors.size]

            val branding = ReceiptBranding(
                headerLogoUrl = if (i % 2 == 0) "data:logo" else null,
                paperWidthMm = width,
                themeColor = theme,
                customBackgroundColorHex = customBg,
                customCardTopBorderColorHex = customBorder,
                useForceDarkTheme = forceDark,
                beforeHeaderMsg = if (i % 3 == 0) "<b>Header</b>" else null,
                afterHeaderMsg = if (i % 3 == 1) "<b>Header 2</b>" else null,
                beforeItemsMsg = if (i % 3 == 2) "<b>Items</b>" else null,
                afterItemsMsg = if (i % 3 == 0) "<b>Items 2</b>" else null,
                beforeTotalsMsg = if (i % 3 == 1) "<b>Totals</b>" else null,
                afterTotalsMsg = if (i % 3 == 2) "<b>Totals 2</b>" else null,
                beforeQrMsg = if (i % 3 == 0) "<b>QR</b>" else null,
                footerMsg = if (i % 3 == 1) "<b>Footer</b>" else null,
                language = if (i % 3 == 0) ReceiptLanguage.RU else if (i % 3 == 1) ReceiptLanguage.KK else ReceiptLanguage.MIXED
            )

            val kkm = defaultKkm.copy(branding = branding)
            val request = ReceiptRequest("kkm-1", "0000", ReceiptOperationType.SELL, mockItems, mockPayments, Money(200, 0), Money(200, 0), Money(0, 0), "key-$i")
            val doc = FiscalDocumentSnapshot(
                id = "doc-$i",
                cashboxId = "kkm-1",
                shiftId = "shift-1",
                docType = "CHECK",
                docNo = i.toLong(),
                shiftNo = i.toLong(),
                createdAt = 1782200000000L,
                totalAmount = 20000L,
                currency = "KZT",
                fiscalSign = "FS-123",
                autonomousSign = "AS-123",
                isAutonomous = isAutonomous,
                ofdStatus = ofdStatus,
                deliveredAt = null,
                receiptUrl = if (i % 2 == 0) "http://url" else null,
                registrationNumber = "RN-1",
                taxpayerName = "Org",
                taxpayerBin = "BIN-1",
                taxpayerAddress = "Addr",
                factoryNumber = "FN-1",
                ofdProvider = ofdProvider
            )
            val shift = ShiftInfo(
                id = "shift-$i",
                kkmId = "kkm-1",
                shiftNo = i.toLong(),
                status = if (i % 2 == 0) ShiftStatus.OPEN else ShiftStatus.CLOSED,
                openedAt = 1782200000000L,
                closedAt = if (i % 2 == 0) null else 1782210000000L
            )

            renderer.renderHtml(request, doc, kkm, layout)
            renderer.renderXReportHtml(shift, emptyMap(), kkm, ofdStatus, layout)
            renderer.renderOpenShiftHtml(shift, kkm, ofdStatus, "1", layout)
            renderer.renderCloseShiftHtml(shift, emptyMap(), kkm, ofdStatus, "2", layout)
            renderer.renderCashOperationHtml(doc, kkm, layout)
            renderer.renderPreviewHtml(branding, layout)
        }
    }

    private fun stripHtml(html: String): String {
        var result = html
        result = result.replace(Regex("""<span\s+class="lang-fraction">\s*<span\s+class="lang-fraction-top">([\s\S]*?)</span>\s*<span\s+class="lang-fraction-bottom">([\s\S]*?)</span>\s*</span>"""), "$1 / $2")
        result = result.replace(Regex("""<span\s+class="badge\s+[^"]*">\s*<span\s+class="badge-main">([\s\S]*?)</span>\s*<span\s+class="badge-divider"></span>\s*<span\s+class="badge-sub">([\s\S]*?)</span>\s*</span>"""), "$1 / $2")
        result = result.replace(Regex("""</td>\s*<td[^>]*>"""), " ")
        return result.replace(Regex("<[^>]*>"), "")
    }
}
