package kz.mybrain.superkassa.core.data.receipt

import kz.mybrain.superkassa.core.domain.model.*
import kz.mybrain.superkassa.core.domain.port.ReceiptRenderPort
import kz.mybrain.superkassa.core.domain.port.QrCodeGeneratorPort
import kz.mybrain.superkassa.core.domain.tax.TaxCalculationService
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Рендерит чек в HTML для печати и доставки.
 */
class ReceiptHtmlRenderer(private val qrCodeGenerator: QrCodeGeneratorPort) : ReceiptRenderPort {

    companion object {
        private const val QR_CODE_SIZE_PX = 180
    }

    override fun renderHtml(receipt: ReceiptRequest, doc: FiscalDocumentSnapshot): String {
        val dateStr = ReceiptFormatter.formatDate(doc.createdAt)
        val sign = doc.fiscalSign ?: doc.autonomousSign ?: "-"
        val totalStr = ReceiptFormatter.formatMoney(receipt.total)
        val opTitle = operationTitle(receipt.operation)
        val docNoStr = doc.docNo?.toString() ?: doc.id
        val shiftNoStr = doc.shiftNo?.toString() ?: "-"
        
        val receiptUrl = doc.receiptUrl?.trim()?.takeIf { it.isNotEmpty() } ?: buildFallbackReceiptUrl(receipt, doc)
        
        val qrDataUri = receiptUrl?.let { qrCodeGenerator.generatePngDataUri(it, QR_CODE_SIZE_PX) }
        
        val ofdStatusHtml = when (doc.ofdStatus) {
            "DELIVERED", "SENT" -> "<span class=\"badge badge-success\">Отправлен / Жіберілді</span>"
            "PENDING", "OFFLINE" -> "<span class=\"badge badge-warning\">Офлайн / Автономды</span>"
            else -> ReceiptFormatter.escape(doc.ofdStatus ?: "-")
        }
        
        val currency = doc.currency ?: "KZT"
        
        val itemsSumCents = receipt.items.sumOf { ReceiptFormatter.moneyToCents(it.sum) }
        val itemsSumStr = ReceiptFormatter.formatCents(itemsSumCents)
        
        val itemsHtml = receipt.items.joinToString("") { item ->
            val priceStr = ReceiptFormatter.formatMoney(item.price)
            val sumStr = ReceiptFormatter.formatMoney(item.sum)
            val unit = try {
                item.measureUnitCode?.let { UnitOfMeasurement.fromCode(it) } ?: UnitOfMeasurement.DEFAULT
            } catch (e: Exception) {
                UnitOfMeasurement.DEFAULT
            }
            val unitStr = if (unit == UnitOfMeasurement.PIECE) "шт / дана" else "${unit.shortRus} / ${unit.shortKaz}"
            val exciseHtml = if (!item.listExciseStamp.isNullOrEmpty()) {
                val stamps = item.listExciseStamp.joinToString(", ") { ReceiptFormatter.escape(it) }
                "<div class=\"excise-stamps\">Маркировка / Таңбалау: $stamps</div>"
            } else {
                ""
            }
            """
            <tr>
                <td class="name">
                    ${ReceiptFormatter.escape(item.name)}
                    $exciseHtml
                </td>
                <td class="num">${item.quantity} $unitStr</td>
                <td class="num">$priceStr</td>
                <td class="num">$sumStr</td>
            </tr>
            """.trimIndent()
        }
        
        val paymentsHtml = receipt.payments.joinToString("") { p ->
            val typeStr = when (p.type) {
                PaymentType.CASH -> "Наличные / Қолма-қол"
                PaymentType.CARD -> "Карта / Карта"
                PaymentType.ELECTRONIC -> "Электронно / Электронды"
            }
            """
            <tr>
                <td>$typeStr</td>
                <td class="num">${ReceiptFormatter.formatMoney(p.sum)}</td>
            </tr>
            """.trimIndent()
        }
        
        val summaryRowsSb = StringBuilder()
        summaryRowsSb.append(summaryRow("Промежуточный итог / Аралық жиынтық", itemsSumStr))
        receipt.discount?.let {
            summaryRowsSb.append(summaryRow("Скидка / Жеңілдік", "-${ReceiptFormatter.formatMoney(it)}"))
        }
        receipt.markup?.let {
            summaryRowsSb.append(summaryRow("Наценка / Үстеме", ReceiptFormatter.formatMoney(it)))
        }
        receipt.taken?.let {
            summaryRowsSb.append(summaryRow("Получено / Алынды", ReceiptFormatter.formatMoney(it)))
        }
        receipt.change?.let {
            summaryRowsSb.append(summaryRow("Сдача / Қайтарым", ReceiptFormatter.formatMoney(it)))
        }
        summaryRowsSb.append(summaryRow("ИТОГО / ЖИЫНЫ", totalStr, "grand"))
        val summaryHtml = summaryRowsSb.toString()
        
        val taxResult = TaxCalculationService().calculateTicketTaxes(
            items = receipt.items,
            taxRegime = receipt.taxRegime,
            defaultVatGroup = receipt.defaultVatGroup ?: VatGroup.NO_VAT
        )
        
        val taxSectionHtml = if (taxResult.ticketTaxes.isNotEmpty()) {
            val taxesRows = taxResult.ticketTaxes.joinToString("") { line ->
                val label = when (line.vatGroup) {
                    VatGroup.NO_VAT -> "Без НДС / ҚҚС-сыз"
                    VatGroup.VAT_0 -> "НДС / ҚҚС 0%"
                    VatGroup.VAT_5 -> "НДС / ҚҚС 5%"
                    VatGroup.VAT_10 -> "НДС / ҚҚС 10%"
                    VatGroup.VAT_16 -> "НДС / ҚҚС 16%"
                }
                """
                <tr>
                    <td>$label</td>
                    <td class="num">${ReceiptFormatter.formatMoney(line.taxBase)}</td>
                    <td class="num">${ReceiptFormatter.formatMoney(line.taxSum)}</td>
                </tr>
                """.trimIndent()
            }
            """
            <div class="rule"></div>
            <div class="tax-section">
                <div class="section-title center">Налоги / Салықтар</div>
                <table class="tax-table">
                    <thead>
                        <tr>
                            <th>Ставка / Ставка</th>
                            <th class="num">Облагаемый оборот / Облыс</th>
                            <th class="num">НДС / ҚҚС</th>
                        </tr>
                    </thead>
                    <tbody>
                        $taxesRows
                    </tbody>
                </table>
            </div>
            """.trimIndent()
        } else {
            ""
        }
        
        val escapedSign = ReceiptFormatter.escape(sign)
        val orgTitleHtml = if (!doc.taxpayerName.isNullOrBlank()) {
            "<div class=\"brand-logo\">${ReceiptFormatter.escape(doc.taxpayerName)}</div>"
        } else {
            "<div class=\"brand-logo\">SUPERKASSA</div>"
        }
        val orgBinHtml = if (!doc.taxpayerBin.isNullOrBlank()) {
            "<div class=\"org-bin\">БИН/ИИН: ${ReceiptFormatter.escape(doc.taxpayerBin)}</div>"
        } else {
            ""
        }
        val orgAddressHtml = if (!doc.taxpayerAddress.isNullOrBlank()) {
            "<div class=\"org-address\">${ReceiptFormatter.escape(doc.taxpayerAddress)}</div>"
        } else {
            ""
        }
        val customerBinHtml = if (!receipt.customerBin.isNullOrBlank()) {
            "<div class=\"org-bin\">Покупатель / Сатып алушы БИН/ЖСН: ${ReceiptFormatter.escape(receipt.customerBin)}</div>"
        } else {
            ""
        }
        val factoryNumberHtml = if (!doc.factoryNumber.isNullOrBlank()) {
            "<tr><td>ЗНМ / ЗНМ</td><td>${ReceiptFormatter.escape(doc.factoryNumber)}</td></tr>"
        } else {
            ""
        }
        
        val ofdProviderName = when (doc.ofdProvider?.uppercase()) {
            "KAZAKHTELECOM" -> "АО «Казахтелеком» / «Қазақтелеком» АҚ"
            "TRANSTELECOM" -> "АО «Транстелеком» / «Транстелеком» АҚ"
            "ALTECO" -> "ТОО «Alteco Partners» / «Alteco Partners» ЖШС"
            else -> doc.ofdProvider ?: "-"
        }
        val ofdProviderHtml = if (!doc.ofdProvider.isNullOrBlank()) {
            "<div class=\"footer-item\">ОФД / ОФД: ${ReceiptFormatter.escape(ofdProviderName)}</div>"
        } else {
            ""
        }
        val ofdWebSite = when (doc.ofdProvider?.uppercase()) {
            "KAZAKHTELECOM" -> "oofd.kz"
            "TRANSTELECOM" -> "o.oofd.kz"
            "ALTECO" -> "alteco.kz"
            else -> "consumer.oofd.kz"
        }
        val ofdSiteHtml = "<div class=\"footer-item\">Сайт проверки / Тексеру сайты: $ofdWebSite</div>"

        val receiptLinkHtml = receiptUrl?.let { url ->
            val escapedUrl = ReceiptFormatter.escape(url)
            """<div class="receipt-link">Ссылка на чек / Чек сілтемесі: """ +
            """<a href="$escapedUrl" target="_blank" rel="noopener noreferrer">$escapedUrl</a></div>"""
        } ?: ""
        
        val qrHtml = qrDataUri?.let { uri ->
            """<div class="qr"><img src="${ReceiptFormatter.escape(uri)}" alt="QR-код чека" /></div>"""
        } ?: ""
        
        val autonomousModeHtml = if (doc.isAutonomous) {
            "<div class=\"muted center\">Автономный режим / Автономды режим</div>"
        } else {
            ""
        }
        
        return """
        <!DOCTYPE html>
        <html lang="ru">
        <head>
            <meta charset="UTF-8" />
            <title>Чек #$docNoStr</title>
            <style>
                ${ReceiptHtmlStyles.CSS}
            </style>
        </head>
        <body>
            <div class="receipt">
                <div class="center brand-header">
                    $orgTitleHtml
                    $orgBinHtml
                    $orgAddressHtml
                    $customerBinHtml
                    <div class="rule"></div>
                    <div class="doc-title">$opTitle</div>
                </div>
                <table class="meta-table">
                    <tr><td>Документ / Құжат</td><td>№ $docNoStr</td></tr>
                    <tr><td>Смена / Ауысым</td><td>$shiftNoStr</td></tr>
                    <tr><td>РНМ / ТНМ</td><td>${ReceiptFormatter.escape(doc.registrationNumber ?: "-")}</td></tr>
                    $factoryNumberHtml
                    <tr><td>ККМ ID / БАҚ ID</td><td>${ReceiptFormatter.escape(doc.cashboxId ?: "-")}</td></tr>
                    <tr><td>Дата и время / Күні мен уақыты</td><td>$dateStr</td></tr>
                    <tr><td>Валюта / Валюта</td><td>$currency</td></tr>
                    <tr><td>Статус ОФД / ОФД статусы</td><td>$ofdStatusHtml</td></tr>
                </table>
                
                <div class="rule"></div>
                <table class="items-table">
                    <thead>
                        <tr>
                            <th>Наименование / Атауы</th>
                            <th class="num">Кол-во / Саны</th>
                            <th class="num">Цена / Бағасы</th>
                            <th class="num">Сумма / Сомасы</th>
                        </tr>
                    </thead>
                    <tbody>
                        $itemsHtml
                    </tbody>
                </table>
        
                <div class="rule"></div>
                <table class="payments-table">
                    <tbody>
                        $paymentsHtml
                    </tbody>
                </table>
        
                <div class="rule"></div>
                <table class="summary-table">
                    <tbody>
                        $summaryHtml
                    </tbody>
                </table>
                
                $taxSectionHtml
        
                <div class="rule"></div>
                <div class="footer">
                    <div class="footer-item">Фискальный признак / Фискалдық белгі: $escapedSign</div>
                    $ofdProviderHtml
                    $ofdSiteHtml
                    $receiptLinkHtml
                    $qrHtml
                    $autonomousModeHtml
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    private fun summaryRow(label: String, value: String, cssClass: String? = null): String {
        val classAttr = if (!cssClass.isNullOrBlank()) " class=\"$cssClass\"" else ""
        return "<tr$classAttr><td>${ReceiptFormatter.escape(label)}</td><td class=\"num\">${ReceiptFormatter.escape(value)}</td></tr>"
    }

    private fun operationTitle(op: ReceiptOperationType): String = when (op) {
        ReceiptOperationType.SELL -> "ЧЕК ПРОДАЖИ / САТУ ЧЕГІ"
        ReceiptOperationType.SELL_RETURN -> "ВОЗВРАТ ПРОДАЖИ / САТУДЫ ҚАЙТАРУ"
        ReceiptOperationType.BUY -> "ЧЕК ПОКУПКИ / САТЫП АЛУ ЧЕГІ"
        ReceiptOperationType.BUY_RETURN -> "ВОЗВРАТ ПОКУПКИ / САТЫП АЛУДЫ ҚАЙТАРУ"
    }

    private fun buildFallbackReceiptUrl(receipt: ReceiptRequest, doc: FiscalDocumentSnapshot): String? {
        val regNum = doc.registrationNumber?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val sign = doc.fiscalSign?.trim()?.takeIf { it.isNotEmpty() }
            ?: doc.autonomousSign?.trim()?.takeIf { it.isNotEmpty() }
            ?: return null
        val totalStr = ReceiptFormatter.formatMoney(receipt.total)
        val dtStr = Instant.ofEpochMilli(doc.createdAt).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
        return "https://consumer.oofd.kz?i=$regNum&f=$sign&s=$totalStr&t=$dtStr"
    }
}
