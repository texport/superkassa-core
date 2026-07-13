package io.github.texport.superkassa.receiptrenderer.impl.renderer.component.report

import io.github.texport.superkassa.core.domain.api.model.zxreport.TaxAggregate
import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.toOperationKey
import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.toTaxKey

internal object ReportTaxesComponent {
    fun render(
        taxes: List<TaxAggregate>,
        t: (String) -> String,
        translateInlineKey: (String) -> String,
        formatAmount: (Long) -> String
    ): String {
        val taxCards = taxes.flatMap { taxAgg ->
            val label = translateInlineKey(taxAgg.taxTypeCode.toTaxKey())
            taxAgg.operations.map { op ->
                val opLabel = translateInlineKey(op.operation.toOperationKey())
                io.github.texport.superkassa.receiptrenderer.impl.renderer.component.common.CardComponent.render(
                    title = "$label ($opLabel)",
                    rows = listOf(
                        io.github.texport.superkassa.receiptrenderer.impl.renderer.component.common.CardComponent.Row(
                            label = t("sum_tax"),
                            value = formatAmount(op.taxSumBills),
                            valueClass = "tax-sum-cell bold"
                        ),
                        io.github.texport.superkassa.receiptrenderer.impl.renderer.component.common.CardComponent.Row(
                            label = t("turnover"),
                            value = formatAmount(op.turnoverBills)
                        )
                    )
                )
            }
        }.joinToString("")

        return """
            <div class="section-title">${translateInlineKey("taxes_by_operations")}</div>
            <div class="taxes-list">
                $taxCards
            </div>
        """.trimIndent()
    }
}
