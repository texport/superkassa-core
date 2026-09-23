package io.github.texport.superkassa.core.domain.impl.usecase.print.protocol

import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptLayoutType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.api.model.zxreport.ZxReportInput
import io.github.texport.superkassa.core.domain.api.port.internal.ReceiptRenderPort
import kotlinx.serialization.json.JsonObject

/**
 * Документ пакета, разобранный до того, что принимает рисовальщик.
 *
 * Видов ровно столько, сколько команд протокола порождают документ:
 * чек, сменный отчёт и движение денег в ящике. Всё остальное документом
 * не является и печатной формы не имеет.
 */
internal sealed interface ProtocolDocument {

    /**
     * Рисует себя тем же рисовальщиком, каким касса рисует свои документы.
     *
     * Какой вход рисовальщика выбрать, знает сам документ: чек, отчёт
     * и кассовый ордер рисуются по-разному, и разбирать это на стороне
     * вызова значило бы держать один и тот же выбор в двух местах.
     *
     * @param renderer рисовальщик печатных форм.
     * @param kkm касса, которой рисуется документ.
     * @param layout ширина ленты; не задана — та, что настроена у кассы.
     * @return HTML печатной формы.
     */
    fun draw(renderer: ReceiptRenderPort, kkm: KkmInfo, layout: ReceiptLayoutType?): String

    /** Торговый чек: продажа, покупка и возвраты по ним. */
    class Receipt(val receipt: ReceiptRequest, val document: FiscalDocumentSnapshot) : ProtocolDocument {
        override fun draw(renderer: ReceiptRenderPort, kkm: KkmInfo, layout: ReceiptLayoutType?): String =
            renderer.renderHtml(receipt, document, kkm, layout)
    }

    /** X- или Z-отчёт смены. */
    class Report(
        val report: ZxReportInput,
        val closesShift: Boolean,
        val documentNumber: String?,
        val ofdStatus: String
    ) : ProtocolDocument {
        override fun draw(renderer: ReceiptRenderPort, kkm: KkmInfo, layout: ReceiptLayoutType?): String =
            if (closesShift) {
                renderer.renderCloseShiftHtml(report, kkm, ofdStatus, documentNumber, layout)
            } else {
                renderer.renderXReportHtml(report, kkm, ofdStatus, documentNumber, layout)
            }
    }

    /** Внесение денег в ящик или изъятие из него. */
    class CashOperation(val document: FiscalDocumentSnapshot) : ProtocolDocument {
        override fun draw(renderer: ReceiptRenderPort, kkm: KkmInfo, layout: ReceiptLayoutType?): String =
            renderer.renderCashOperationHtml(document, kkm, layout)
    }
}

/**
 * Разбирает пакет в документ.
 *
 * Вид документа решает команда: она же решала это и на кассе, когда
 * документ печатался там.
 *
 * @param packet пакет протокола.
 * @param kkm касса, которой рисуется документ, с реквизитами из пакета.
 * @return документ либо `null`, если команда пакета документа не порождает.
 */
internal fun documentOf(packet: ProtocolPacket, kkm: KkmInfo): ProtocolDocument? = when (packet.command) {
    "COMMAND_TICKET" -> packet.request.child("ticket")?.let { ticketOf(it, packet, kkm) }
    "COMMAND_REPORT" -> packet.request.child("report")?.let { reportDocumentOf(it, packet) }
    "COMMAND_CLOSE_SHIFT" -> packet.request.child("closeShift")?.let { closeShiftOf(it, packet) }
    "COMMAND_MONEY_PLACEMENT" -> packet.request.child("moneyPlacement")?.let { placementOf(it, packet, kkm) }
    else -> null
}

/** Чек вместе с тем, что ответил о нём ОФД. */
private fun ticketOf(ticket: JsonObject, packet: ProtocolPacket, kkm: KkmInfo): ProtocolDocument.Receipt {
    val receipt = receiptOf(ticket, kkm)
    val answer = packet.response?.child("ticket")
    val offlineNumber = ticket.number("offlineTicketNumber")
    val common = snapshotOf(ticket, kkm, "CHECK", receipt.total.tiyn(), packet.ofdStatus)
    return ProtocolDocument.Receipt(
        receipt,
        common.copy(
            fiscalSign = answer?.text("ticketNumber"),
            autonomousSign = offlineNumber?.toString(),
            isAutonomous = offlineNumber != null,
            receiptUrl = answer?.let(::receiptLinkOf)
        )
    )
}

/** Отчёт: вид берётся из запроса, сами итоги — из вложенного отчёта. */
private fun reportDocumentOf(report: JsonObject, packet: ProtocolPacket): ProtocolDocument.Report? {
    val totals = report.child("zxReport") ?: answeredTotals(packet) ?: return null
    return ProtocolDocument.Report(
        report = reportOf(totals),
        closesShift = report.text("report") != "REPORT_X",
        documentNumber = report.text("printedDocumentNumber"),
        ofdStatus = packet.ofdStatus
    )
}

/** Закрытие смены: Z-отчёт лежит в самом запросе закрытия. */
private fun closeShiftOf(close: JsonObject, packet: ProtocolPacket): ProtocolDocument.Report? {
    val totals = close.child("zReport") ?: answeredTotals(packet) ?: return null
    return ProtocolDocument.Report(
        report = reportOf(totals),
        closesShift = true,
        documentNumber = close.text("printedDocumentNumber"),
        ofdStatus = packet.ofdStatus
    )
}

/** Итоги смены из ответа ОФД: запасной путь, когда запрос их не несёт. */
private fun answeredTotals(packet: ProtocolPacket): JsonObject? =
    packet.response?.child("report")?.child("zxReport")

/** Движение денег в ящике: документ нефискальный, и признака у него нет. */
private fun placementOf(placement: JsonObject, packet: ProtocolPacket, kkm: KkmInfo): ProtocolDocument.CashOperation {
    val type = if (placement.text("operation") == "MONEY_PLACEMENT_WITHDRAWAL") "CASH_OUT" else "CASH_IN"
    return ProtocolDocument.CashOperation(snapshotOf(placement, kkm, type, placement.tiyn("sum"), packet.ofdStatus))
}
