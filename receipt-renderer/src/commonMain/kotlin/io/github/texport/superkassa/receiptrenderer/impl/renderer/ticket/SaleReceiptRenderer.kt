package io.github.texport.superkassa.receiptrenderer.impl.renderer.ticket

import io.github.texport.superkassa.receiptrenderer.impl.ReceiptFormatter
import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.formatted
import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.escaped
import io.github.texport.superkassa.receiptrenderer.impl.renderer.style.TicketStyles
import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.BaseDocumentRenderer
import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.DocumentConstants
import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.translationKey
import io.github.texport.superkassa.receiptrenderer.impl.renderer.component.ticket.ParentTicketComponent
import io.github.texport.superkassa.receiptrenderer.impl.renderer.component.ticket.PaymentsListComponent
import io.github.texport.superkassa.receiptrenderer.impl.renderer.component.ticket.SaleItemsComponent
import io.github.texport.superkassa.receiptrenderer.impl.renderer.component.ticket.TaxesSectionComponent
import io.github.texport.superkassa.core.domain.api.model.kkm.*
import io.github.texport.superkassa.core.domain.api.model.ofd.*
import io.github.texport.superkassa.core.domain.api.model.common.*
import io.github.texport.superkassa.core.domain.api.model.receipt.*
import io.github.texport.superkassa.core.domain.api.port.integration.QrCodeGeneratorPort
import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.MetadataBuilder
import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.StandardDocumentInput

/**
 * Рендерер фискальных чеков продаж/возвратов.
 *
 * @property qrCodeGenerator генератор QR-кодов для чеков
 * @property ofdProviders список доступных провайдеров ОФД
 */
internal class SaleReceiptRenderer(
    private val qrCodeGenerator: QrCodeGeneratorPort
) : BaseDocumentRenderer() {

    /**
     * Формирует HTML-представление фискального чека.
     *
     * @param receipt запрос на чек
     * @param doc фискальный документ
     * @param kkm информация о ККМ
     * @return HTML-строка отрендеренного чека
     */
    fun render(receipt: ReceiptRequest, doc: FiscalDocumentSnapshot, kkm: KkmInfo): String {
        val lang = kkm.branding.language
        fun t(key: String): String = translate(key, lang)
        fun translateInlineKey(key: String): String = translateInline(key, lang)

        // У автономного чека фискального признака нет: его выдаёт ОФД. Раньше
        // в эту строку подставлялся автономный признак — время в миллисекундах,
        // и покупатель читал тринадцать цифр эпохи как фискальный признак.
        // Автономный чек опознаётся своим номером документа и строкой
        // «Автономный режим», как и на кассах парка.
        val sign = doc.fiscalSign
        val totalStr = receipt.total.formatted()
        val opTitleKey = operationTitleKey(receipt.operation)

        // КГД требует QR-код на каждом чеке. Сетевому ссылку присылает ОФД,
        // автономному касса собирает её сама — иначе чек, пробитый в разрыве
        // связи, уходит покупателю без кода вовсе.
        val receiptUrl = doc.receiptUrl?.trim()?.takeIf { it.isNotEmpty() }
            ?: OfflineReceiptLink.of(doc, ReceiptFormatter.moneyToTiyn(receipt.total))
        val qrDataUri = receiptUrl?.let { qrCodeGenerator.generatePngDataUri(it, DocumentConstants.QR_CODE_SIZE_PX) }

        // Сторно-позиция отменяет ранее пробитую: в промежуточный итог она
        // входит со знаком минус. Со сложением итог расходился с чеком
        // ровно на удвоенную сумму отмены.
        val itemsSumTiyn = receipt.items.sumOf {
            val sum = ReceiptFormatter.moneyToTiyn(it.sum)
            if (it.isStorno) -sum else sum
        }
        val itemsSumStr = ReceiptFormatter.formatTiyn(itemsSumTiyn)

        val itemsHtml = SaleItemsComponent.render(
            items = receipt.items,
            defaultVatGroup = receipt.defaultVatGroup ?: VatGroup.NO_VAT,
            taxRegime = receipt.taxRegime,
            receiptDiscount = receipt.discount,
            t = { t(it) },
            translateInlineKey = { translateInlineKey(it) },
            namePair = { ru, kk -> translate(ru, kk, lang) }
        )

        val paymentsHtml = PaymentsListComponent.render(
            payments = receipt.payments,
            taken = receipt.taken,
            change = receipt.change,
            t = { t(it) }
        )

        val summaryRowsSb = StringBuilder()
        summaryRowsSb.append(summaryRow(t("subtotal"), itemsSumStr))
        receipt.discount?.let {
            summaryRowsSb.append(summaryRow(t("discount_total"), "-${it.formatted()}"))
        }
        receipt.markup?.let {
            summaryRowsSb.append(summaryRow(t("markup_total"), it.formatted()))
        }
        summaryRowsSb.append(summaryRow(t("grand_total"), totalStr, "grand"))
        val summaryHtml = summaryRowsSb.toString()

        val taxSectionHtml = TaxesSectionComponent.render(
            ticketTaxes = receipt.ticketTaxes ?: emptyList(),
            t = { t(it) }
        )

        // На чеке печатается имя оператора, а не служебный тег: покупателю
        // «BFD:DEV» не говорит ничего, а по названию он знает, кому жаловаться.
        val ofdProviderName = providerName(doc.ofdProvider, lang)

        val additionalMeta = MetadataBuilder { translateInlineKey(it) }.apply {
            add("buyer_bin_iin", receipt.customerBin)
        }.build()

        val adapter = io.github.texport.superkassa.receiptrenderer.impl.renderer.base.HtmlBrandingAdapter(kkm.branding)
        val beforeItemsHtml = adapter.beforeItemsHtml
        val afterItemsHtml = adapter.afterItemsHtml
        val beforeTotalsHtml = adapter.beforeTotalsHtml
        val afterTotalsHtml = adapter.afterTotalsHtml

        val parentTicketHtml = receipt.parentTicket?.let { parent ->
            val parentDateStr = formatDate(parent.parentTicketDateTimeMillis)
            val parentTitle = if (receipt.operation == ReceiptOperationType.SELL_RETURN ||
                receipt.operation == ReceiptOperationType.BUY_RETURN
            ) {
                t("parent_ticket_return")
            } else {
                t("parent_ticket_storno")
            }
            ParentTicketComponent.render(
                parent = parent,
                parentTitle = parentTitle,
                formattedDateTime = parentDateStr,
                t = { t(it) }
            )
        } ?: ""

        val bodyContent = """
            $parentTicketHtml
            $beforeItemsHtml
            <div class="items-list">
                $itemsHtml
            </div>
            $afterItemsHtml
            <div class="rule"></div>
            $beforeTotalsHtml
            <table class="summary-table">
                <tbody>
                    $summaryHtml
                </tbody>
            </table>
            <div class="rule"></div>
            <table class="payments-table">
                <tbody>
                    $paymentsHtml
                </tbody>
            </table>
            $taxSectionHtml
            $afterTotalsHtml
        """.trimIndent()

        return renderStandardDocument(
            StandardDocumentInput(
                titleKey = opTitleKey,
                kkm = kkm,
                createdAt = doc.createdAt,
                shiftNo = doc.shiftNo,
                docNo = doc.docNo?.toString() ?: doc.id,
                ofdStatus = doc.ofdStatus,
                isFiscal = true,
                isAutonomous = doc.isAutonomous,
                fiscalSign = sign,
                ofdProvider = ofdProviderName,
                receiptUrl = receiptUrl,
                qrDataUri = qrDataUri,
                additionalMeta = additionalMeta,
                docCss = TicketStyles.TICKET_CSS,
                bodyContent = bodyContent
            )
        )
    }

    private fun summaryRow(label: String, value: String, cssClass: String = ""): String {
        val classAttr = if (cssClass.isNotEmpty()) " class=\"$cssClass\"" else ""
        return """
            <tr$classAttr>
                <td>$label</td>
                <td class="num">$value</td>
            </tr>
        """.trimIndent()
    }

    private fun operationTitleKey(type: ReceiptOperationType): String = type.translationKey

    /**
     * Название ОФД по его тегу.
     *
     * @param tag тег вида `BFD:DEV`.
     * @param lang язык чека.
     * @return имя оператора либо сам тег, если такого оператора нет в реестре.
     */
    private fun providerName(tag: String?, lang: ReceiptLanguage): String {
        val id = tag?.substringBefore(TAG_SEPARATOR) ?: return DASH
        val provider = OfdProvider.findProvider(id) ?: return tag.escaped()
        // Экранируется имя, а не результат: на двух языках `translate` отдаёт
        // готовую двухэтажную разметку, и её экранирование печатало на чеке
        // сам тег `<span class=...>` вместо названия оператора.
        return translate(provider.nameRu.escaped(), provider.nameKk.escaped(), lang)
    }
}

/** Разделитель тега провайдера и окружения. */
private const val TAG_SEPARATOR = ':'

/** Прочерк там, где сведений нет. */
private const val DASH = "-"
