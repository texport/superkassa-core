package io.github.texport.superkassa.receiptrenderer.impl

import io.github.texport.superkassa.core.domain.model.kkm.*
import io.github.texport.superkassa.core.domain.model.shift.*
import io.github.texport.superkassa.core.domain.model.receipt.*


import io.github.texport.superkassa.receiptrenderer.impl.renderer.operation.CashOperationRenderer
import io.github.texport.superkassa.receiptrenderer.impl.renderer.report.OpenShiftRenderer
import io.github.texport.superkassa.receiptrenderer.impl.renderer.report.XReportRenderer
import io.github.texport.superkassa.receiptrenderer.impl.renderer.report.ZReportRenderer





import kotlin.test.Test
import kotlin.test.assertTrue

class ReportPrintRendererTest {

    private val kkm = KkmInfo(
        id = "kkm-123",
        createdAt = 1782200000000L,
        updatedAt = 1782200000000L,
        mode = "PRODUCTION",
        state = "READY",
        branding = ReceiptBranding()
    )

    private val xRenderer = XReportRenderer()
    private val zRenderer = ZReportRenderer()
    private val openShiftRenderer = OpenShiftRenderer()
    private val cashOperationRenderer = CashOperationRenderer()

    @Test
    fun testRenderXReportHtml() {
        val shift = ShiftInfo(
            id = "shift-123",
            kkmId = "kkm-123",
            shiftNo = 12,
            status = ShiftStatus.OPEN,
            openedAt = 1782200000000L
        )
        val counters = mapOf(
            "operation.OPERATION_SELL.count" to 10L,
            "operation.OPERATION_SELL.sum" to 100050L,
            "operation.OPERATION_SELL_RETURN.count" to 0L,
            "operation.OPERATION_SELL_RETURN.sum" to 0L
        )

        val html = xRenderer.render(shift, counters, kkm, null)
        val cleanHtml = stripHtml(html)
        assertTrue(cleanHtml.contains("X-ОТЧЁТ"))
        assertTrue(cleanHtml.contains("Ауысым № / Смена №"))
        assertTrue(cleanHtml.contains("12"))
        assertTrue(cleanHtml.contains("Сату / Продажа"))
        assertTrue(cleanHtml.contains("10"))
        assertTrue(cleanHtml.contains("1000.50"))
        // Zero counters should be displayed with 0.00 value
        assertTrue(cleanHtml.contains("Возврат продажи"))
        assertTrue(cleanHtml.contains("0.00"))
    }

    @Test
    fun testRenderXReportHtmlEmptyCounters() {
        val shift = ShiftInfo(
            id = "shift-123",
            kkmId = "kkm-123",
            shiftNo = 12,
            status = ShiftStatus.OPEN,
            openedAt = 1782200000000L
        )
        val html = xRenderer.render(shift, emptyMap(), kkm, null)
        val cleanHtml = stripHtml(html)
        assertTrue(cleanHtml.contains("Сату / Продажа"))
        assertTrue(cleanHtml.contains("0.00"))
    }

    @Test
    fun testRenderOpenShiftHtml() {
        val shift = ShiftInfo(
            id = "shift-123",
            kkmId = "kkm-123",
            shiftNo = 12,
            status = ShiftStatus.OPEN,
            openedAt = 1782200000000L
        )
        val html = openShiftRenderer.render(shift, kkm, null)
        val cleanHtml = stripHtml(html)
        assertTrue(cleanHtml.contains("АУЫСЫМДЫ АШУ"))
        assertTrue(cleanHtml.contains("ОТКРЫТИЕ СМЕНЫ"))
        assertTrue(cleanHtml.contains("Ауысым № / Смена №"))
        assertTrue(cleanHtml.contains("12"))
    }

    @Test
    fun testRenderCloseShiftHtml() {
        val shift = ShiftInfo(
            id = "shift-123",
            kkmId = "kkm-123",
            shiftNo = 12,
            status = ShiftStatus.CLOSED,
            openedAt = 1782200000000L,
            closedAt = 1782200050000L
        )
        val counters = mapOf(
            "operation.OPERATION_SELL.count" to 10L,
            "operation.OPERATION_SELL.sum" to 100050L
        )

        val html = zRenderer.render(shift, counters, kkm, "DELIVERED")
        val cleanHtml = stripHtml(html)
        assertTrue(cleanHtml.contains("Z-ЕСЕП (АУЫСЫМДЫ ЖАБУ)"))
        assertTrue(cleanHtml.contains("Z-ОТЧЁТ (ЗАКРЫТИЕ СМЕНЫ)"))
        assertTrue(cleanHtml.contains("Ауысым № / Смена №"))
        assertTrue(cleanHtml.contains("12"))
        assertTrue(cleanHtml.contains("Сату / Продажа"))
        assertTrue(cleanHtml.contains("1000.50"))
    }

    @Test
    fun testRenderCloseShiftHtmlNullClosedAt() {
        val shift = ShiftInfo(
            id = "shift-123",
            kkmId = "kkm-123",
            shiftNo = 12,
            status = ShiftStatus.CLOSED,
            openedAt = 1782200000000L,
            closedAt = null
        )
        val html = zRenderer.render(shift, emptyMap(), kkm, null)
        val cleanHtml = stripHtml(html)
        assertTrue(cleanHtml.contains("Жабылған күні / Закрыта"))
        assertTrue(cleanHtml.contains("-"))
    }

    @Test
    fun testRenderCashOperationHtml() {
        val docIn = FiscalDocumentSnapshot(
            id = "doc-1",
            cashboxId = "kkm-123",
            shiftId = "shift-123",
            docType = "CASH_IN",
            docNo = 45L,
            shiftNo = 12L,
            createdAt = 1782200000000L,
            totalAmount = 250000L,
            currency = "KZT",
            fiscalSign = null,
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = null,
            deliveredAt = null
        )

        val htmlIn = cashOperationRenderer.render(docIn, kkm)
        val cleanHtmlIn = stripHtml(htmlIn)
        assertTrue(cleanHtmlIn.contains("ВНЕСЕНИЕ НАЛИЧНЫХ"))
        assertTrue(cleanHtmlIn.contains("2500.00 ₸") || cleanHtmlIn.contains("2500,00 ₸"))
        assertTrue(cleanHtmlIn.contains("Құжат № / Документ №"))

        val docOut = FiscalDocumentSnapshot(
            id = "doc-2",
            cashboxId = "kkm-123",
            shiftId = "shift-123",
            docType = "CASH_OUT",
            docNo = null,
            shiftNo = 12L,
            createdAt = 1782200000000L,
            totalAmount = 10000L,
            currency = null,
            fiscalSign = null,
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = null,
            deliveredAt = null
        )

        val htmlOut = cashOperationRenderer.render(docOut, kkm)
        val cleanHtmlOut = stripHtml(htmlOut)
        assertTrue(cleanHtmlOut.contains("ИЗЪЯТИЕ НАЛИЧНЫХ"))
        assertTrue(cleanHtmlOut.contains("100.00 ₸") || cleanHtmlOut.contains("100,00 ₸"))

        val docOther = FiscalDocumentSnapshot(
            id = "doc-3",
            cashboxId = "kkm-123",
            shiftId = "shift-123",
            docType = "OTHER",
            docNo = null,
            shiftNo = 12L,
            createdAt = 1782200000000L,
            totalAmount = null,
            currency = null,
            fiscalSign = null,
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = null,
            deliveredAt = null
        )

        val htmlOther = cashOperationRenderer.render(docOther, kkm)
        val cleanHtmlOther = stripHtml(htmlOther)
        assertTrue(cleanHtmlOther.contains("ОПЕРАЦИЯ С НАЛИЧНЫМИ"))
        assertTrue(cleanHtmlOther.contains("0.00 ₸"))
    }

    private fun stripHtml(html: String): String {
        var result = html
        result = result.replace(Regex("""<span\s+class="lang-fraction">\s*<span\s+class="lang-fraction-top">([\s\S]*?)</span>\s*<span\s+class="lang-fraction-bottom">([\s\S]*?)</span>\s*</span>"""), "$1 / $2")
        result = result.replace(Regex("""<span\s+class="badge\s+[^"]*">\s*<span\s+class="badge-main">([\s\S]*?)</span>\s*<span\s+class="badge-divider"></span>\s*<span\s+class="badge-sub">([\s\S]*?)</span>\s*</span>"""), "$1 / $2")
        return result.replace(Regex("<[^>]*>"), "")
    }
}
