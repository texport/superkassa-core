package io.github.texport.superkassa.receiptrenderer.impl.renderer.component.ticket

import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.formatted
import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.translationKey
import io.github.texport.superkassa.core.domain.api.model.receipt.TaxLine
import io.github.texport.superkassa.core.domain.api.model.common.VatGroup

internal object TaxesSectionComponent {
    fun render(
        ticketTaxes: List<TaxLine>,
        t: (String) -> String
    ): String {
        if (ticketTaxes.isEmpty()) return ""
        val taxesHtml = ticketTaxes.joinToString("") { line ->
            val label = t(line.vatGroup.translationKey)
            val turnoverKey = if (line.vatGroup == VatGroup.NO_VAT) "non_taxable_turnover" else "taxable_turnover"
            io.github.texport.superkassa.receiptrenderer.impl.renderer.component.common.CardComponent.render(
                title = label,
                rows = listOf(
                    io.github.texport.superkassa.receiptrenderer.impl.renderer.component.common.CardComponent.Row(
                        label = t("sum_tax"),
                        value = line.taxSum.formatted(),
                        valueClass = "tax-sum-cell num bold"
                    ),
                    io.github.texport.superkassa.receiptrenderer.impl.renderer.component.common.CardComponent.Row(
                        label = t(turnoverKey),
                        value = line.taxBase.formatted()
                    )
                )
            )
        }
        return """
        <div class="rule"></div>
        <div class="tax-section">
            <div class="section-title">${t("taxes")}</div>
            <div class="taxes-list">
                $taxesHtml
            </div>
        </div>
        """.trimIndent()
    }
}
