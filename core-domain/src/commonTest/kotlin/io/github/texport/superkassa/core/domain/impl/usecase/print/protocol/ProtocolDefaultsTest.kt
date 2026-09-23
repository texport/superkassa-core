package io.github.texport.superkassa.core.domain.impl.usecase.print.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Чего в пакете нет.
 *
 * Необязательное поле протокол не пишет вовсе, и у документа с чужой
 * кассы не бывает половины того, что есть у своего. Каждое такое место
 * обязано отвечать пустотой, а не падением: на экране владельца падение
 * разбора выглядит как сломанный кабинет.
 */
class ProtocolDefaultsTest {

    private val kkm = ProtocolPackets.localKkm()

    private fun documentOf(body: String): ProtocolDocument? {
        val packet = requireNotNull(ProtocolPacket.of(body))
        return documentOf(packet, kkm.withProtocolRegistration(packet.service))
    }

    @Test
    fun `отчёт без единой строки собирается пустым`() {
        val document = documentOf(
            """{"request": {"command": "COMMAND_REPORT", "report": {"zxReport": {}}}}"""
        ) as ProtocolDocument.Report

        assertEquals(0, document.report.shiftNumber)
        assertEquals(0L, document.report.dateTimeMillis)
        assertEquals(0L, document.report.openShiftTimeMillis)
        assertNull(document.report.closeShiftTimeMillis)
        assertEquals(0L, document.report.cashSumTiyn)
        assertEquals(0L, document.report.revenueTiyn)
        assertTrue(document.report.taxes.isEmpty())
        assertTrue(document.report.ticketOperations.isEmpty())
        assertNull(document.documentNumber)
        assertTrue(document.closesShift)
    }

    @Test
    fun `строки отчёта без полей читаются нулями и умолчаниями`() {
        val document = documentOf(
            """
            {"request": {"command": "COMMAND_REPORT", "report": {"report": "REPORT_X", "zxReport": {
              "revenue": {"sum": ${ProtocolPackets.money(7)}},
              "operations": [{}], "sections": [{"operations": [{}]}],
              "nonNullableSums": [{}],
              "ticketOperations": [{"payments": [{}]}],
              "moneyPlacements": [{}],
              "taxes": [{"percent": 16000, "operations": [{}]}, {}]
            }}}}
            """.trimIndent()
        ) as ProtocolDocument.Report
        val report = document.report

        assertEquals(700L, report.revenueTiyn)
        assertEquals("OPERATION_SELL", report.operations.single().operation)
        assertEquals(0L, report.operations.single().count)
        assertEquals("", report.sections.single().sectionCode)
        assertEquals("OPERATION_SELL" to 0L, report.nonNullableSums.single())
        assertEquals(0L, report.ticketOperations.single().ticketsTotalCount)
        assertEquals("PAYMENT_CASH", report.ticketOperations.single().payments.single().payment)
        assertEquals("MONEY_PLACEMENT_DEPOSIT", report.moneyPlacements.single().operation)
        assertEquals(100, report.taxes.single().taxType)
        assertEquals(0L, report.taxes.single().operations.single().turnoverTiyn)
    }

    @Test
    fun `пакет без документа внутри команды не рисуется`() {
        assertNull(documentOf("""{"request": {"command": "COMMAND_TICKET"}}"""))
        assertNull(documentOf("""{"request": {"command": "COMMAND_REPORT"}}"""))
        assertNull(documentOf("""{"request": {"command": "COMMAND_REPORT", "report": {}}}"""))
        assertNull(documentOf("""{"request": {"command": "COMMAND_CLOSE_SHIFT"}}"""))
        assertNull(documentOf("""{"request": {"command": "COMMAND_CLOSE_SHIFT", "closeShift": {}}}"""))
        assertNull(documentOf("""{"request": {"command": "COMMAND_MONEY_PLACEMENT"}}"""))
    }
}
