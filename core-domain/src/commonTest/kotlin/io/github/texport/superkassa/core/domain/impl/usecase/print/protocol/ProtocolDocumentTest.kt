package io.github.texport.superkassa.core.domain.impl.usecase.print.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Перевод отчётов, кассовых ордеров и реквизитов пакета в то, что принимает рисовальщик. */
class ProtocolDocumentTest {

    private val kkm = ProtocolPackets.localKkm()

    private fun documentOf(body: String) =
        documentOf(
            requireNotNull(ProtocolPacket.of(body)),
            kkm.withProtocolRegistration(requireNotNull(ProtocolPacket.of(body)).service)
        )

    @Test
    fun `X-отчёт переводится оборотами, налогами, оплатами и остатком ящика`() {
        val document = documentOf(ProtocolPackets.report()) as ProtocolDocument.Report
        val report = document.report

        assertFalse(document.closesShift)
        assertEquals("418", document.documentNumber)
        assertEquals("DELIVERED", document.ofdStatus)
        assertEquals(12, report.shiftNumber)
        assertEquals(1_500_025L, report.cashSumTiyn)
        assertEquals(-2_000_000L, report.revenueTiyn)
        assertEquals("001", report.sections.single().sectionCode)
        assertEquals(7L, report.operations.single().count)
        assertEquals(1_990_000L, report.totalResult.single().sumTiyn)
        assertEquals(10_000L, report.discounts.single().sumTiyn)
        assertEquals(10_000_000L, report.startShiftNonNullableSums.single().second)
        assertEquals(12_000_000L, report.nonNullableSums.single().second)
    }

    @Test
    fun `налог отчёта берёт ставку из пакета, а неизвестную ставку не печатает`() {
        val report = (documentOf(ProtocolPackets.report()) as ProtocolDocument.Report).report
        val tax = report.taxes.single()

        assertEquals("TAX_TYPE_VAT_16", tax.taxTypeCode)
        assertEquals(16_000, tax.percent)
        assertEquals(100, tax.taxType)
        assertEquals(275_862L, tax.operations.single().taxSumTiyn)
        assertEquals(1_724_138L, tax.operations.single().turnoverWithoutTaxTiyn)
    }

    @Test
    fun `Z-отчёт запроса отчёта помечается закрывающим смену`() {
        val document = documentOf(ProtocolPackets.report(kind = "REPORT_Z")) as ProtocolDocument.Report

        assertTrue(document.closesShift)
    }

    @Test
    fun `закрытие смены рисуется Z-отчётом из самого запроса`() {
        val document = documentOf(ProtocolPackets.closeShift()) as ProtocolDocument.Report

        assertTrue(document.closesShift)
        assertEquals("419", document.documentNumber)
        assertEquals(12, document.report.shiftNumber)
        assertNotNull(document.report.closeShiftTimeMillis)
    }

    @Test
    fun `итоги отчёта берутся из ответа, когда в запросе их нет`() {
        val fromRequest = documentOf(ProtocolPackets.report(totals = "")) as ProtocolDocument.Report
        val fromClose = documentOf(ProtocolPackets.closeShift(totals = "")) as ProtocolDocument.Report

        assertEquals(12, fromRequest.report.shiftNumber)
        assertEquals(12, fromClose.report.shiftNumber)
    }

    @Test
    fun `внесение и изъятие денег становятся кассовым ордером`() {
        val out = (documentOf(ProtocolPackets.placement()) as ProtocolDocument.CashOperation).document
        val deposit = documentOf(ProtocolPackets.placement("MONEY_PLACEMENT_DEPOSIT")) as ProtocolDocument.CashOperation

        assertEquals("CASH_OUT", out.docType)
        assertEquals(100_050L, out.totalAmount)
        assertEquals(420L, out.docNo)
        assertEquals(12L, out.shiftNo)
        assertEquals("CASH_IN", deposit.document.docType)
    }

    @Test
    fun `реквизиты документа берутся из пакета, а не у рисующей кассы`() {
        val packet = requireNotNull(ProtocolPacket.of(ProtocolPackets.receipt()))
        val drawnBy = kkm.withProtocolRegistration(packet.service)

        assertEquals("260940000021", drawnBy.id)
        assertEquals("NZ7700123456", drawnBy.registrationNumber)
        assertEquals("SW7700987654", drawnBy.factoryNumber)
        assertEquals("ТОО Пример", drawnBy.ofdServiceInfo?.orgTitle)
        assertEquals("123456789012", drawnBy.ofdServiceInfo?.orgIinOrBin)
        assertEquals("Алматы қ., Абай даңғылы, 1", drawnBy.ofdServiceInfo?.orgAddressKz)
    }

    @Test
    fun `пакет без служебного блока оставляет реквизиты рисующей кассы`() {
        val bare = """{"request": {"command": "COMMAND_TICKET"}}"""
        val packet = requireNotNull(ProtocolPacket.of(bare))

        assertEquals(kkm, kkm.withProtocolRegistration(packet.service))
        assertNull(documentOf(bare))
    }

    @Test
    fun `команда без документа и нечитаемый пакет отвечают пустотой`() {
        assertNull(documentOf("""{"request": {"command": "COMMAND_INFO"}}"""))
        assertNull(ProtocolPacket.of("не JSON вовсе"))
        assertNull(ProtocolPacket.of("""{"response": {"command": "COMMAND_TICKET"}}"""))
    }

    @Test
    fun `команда читается из ответа, когда запрос её не назвал`() {
        val packet = requireNotNull(
            ProtocolPacket.of("""{"request": {}, "response": {"command": "COMMAND_TICKET", "ticket": {}}}""")
        )

        assertEquals("COMMAND_TICKET", packet.command)
        assertEquals("DELIVERED", packet.ofdStatus)
    }
}
