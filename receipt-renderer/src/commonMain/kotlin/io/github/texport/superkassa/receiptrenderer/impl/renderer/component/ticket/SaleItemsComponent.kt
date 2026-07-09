package io.github.texport.superkassa.receiptrenderer.impl.renderer.component.ticket

import io.github.texport.superkassa.core.domain.model.common.*
import io.github.texport.superkassa.core.domain.model.receipt.*

import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.escaped
import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.formatted
import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.formatQuantity
import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.translationKey

internal object SaleItemsComponent {
    fun render(
        items: List<ReceiptItem>,
        defaultVatGroup: VatGroup,
        taxRegime: TaxRegime,
        receiptDiscount: Money?,
        t: (String) -> String,
        translateInlineKey: (String) -> String
    ): String {
        return items.joinToString("") { item ->
            val priceStr = item.price.formatted()
            val sumStr = item.sum.formatted()
            val unit = try {
                item.measureUnitCode?.let { UnitOfMeasurement.fromCode(it) } ?: UnitOfMeasurement.DEFAULT
            } catch (_: Exception) {
                UnitOfMeasurement.DEFAULT
            }
            val unitStr = translateInlineKey("unit_" + unit.name.lowercase())
            val exciseStamps = item.listExciseStamp
            val exciseHtml = if (!exciseStamps.isNullOrEmpty()) {
                val stamps = exciseStamps.joinToString(", ") { it.escaped() }
                "<div class=\"excise-stamps\">${t("excise_stamp")} $stamps</div>"
            } else {
                ""
            }
            val discountVal = item.discount
            val discountHtml = if (discountVal != null && receiptDiscount == null) {
                "<div class=\"item-discount\">${t("discount")} -${discountVal.formatted()}</div>"
            } else {
                ""
            }
            val markupVal = item.markup
            val markupHtml = if (markupVal != null) {
                "<div class=\"item-markup\">${t("markup")} +${markupVal.formatted()}</div>"
            } else {
                ""
            }
            val itemVat = item.vatGroup ?: defaultVatGroup
            val vatLabel = t(itemVat.translationKey)
            val vatHtml = if (taxRegime == TaxRegime.MIXED) {
                "<div class=\"item-vat\">$vatLabel</div>"
            } else {
                ""
            }
            val itemClass = if (item.isStorno) "storno-item" else ""
            val stornoBadgeHtml = if (item.isStorno) {
                """ <span class="storno-badge">${t("storno")}</span>"""
            } else {
                ""
            }
            """
            <div class="item-row-card $itemClass">
                <table class="item-row-table">
                    <tr>
                        <td class="item-name-cell">${item.name.escaped()}$stornoBadgeHtml</td>
                        <td class="item-sum-cell">${if (item.isStorno) "-" else ""}$sumStr</td>
                    </tr>
                    <tr>
                        <td class="item-details-cell" colspan="2">
                            ${item.quantity.formatQuantity()} $unitStr × $priceStr
                            $exciseHtml
                            $vatHtml
                            $discountHtml
                            $markupHtml
                        </td>
                    </tr>
                </table>
            </div>
            """.trimIndent()
        }
    }
}
