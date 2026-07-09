package io.github.texport.superkassa.receiptrenderer.impl.renderer.component.report

import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.toOperationKey

internal object ReportNonNullableComponent {
    fun render(
        nonNullableSums: List<Pair<String, Long>>,
        startShiftNonNullableSums: List<Pair<String, Long>>,
        t: (String) -> String,
        translateInlineKey: (String) -> String,
        formatAmount: (Long) -> String
    ): String {
        val nonNullableCards = mutableListOf<String>()
        for (i in nonNullableSums.indices) {
            val endPair = nonNullableSums[i]
            val op = endPair.first
            val startSum = startShiftNonNullableSums.firstOrNull { it.first == op }?.second ?: 0L
            val endSum = endPair.second

            val label = translateInlineKey(op.toOperationKey())

            nonNullableCards += io.github.texport.superkassa.receiptrenderer.impl.renderer.component.common.CardComponent.render(
                title = label,
                rows = listOf(
                    io.github.texport.superkassa.receiptrenderer.impl.renderer.component.common.CardComponent.Row(
                        label = t("start_shift"),
                        value = formatAmount(startSum),
                        valueClass = "tax-sum-cell",
                        valueStyle = "font-size: 0.95em; color: var(--m3-on-surface-variant); font-weight: normal;"
                    ),
                    io.github.texport.superkassa.receiptrenderer.impl.renderer.component.common.CardComponent.Row(
                        label = t("end_shift"),
                        value = formatAmount(endSum),
                        valueClass = "tax-sum-cell",
                        valueStyle = "font-size: 0.95em; color: var(--m3-on-surface); font-weight: bold;"
                    )
                )
            )
        }

        return """
            <div class="section-title">${translateInlineKey("non_nullable_totals")}</div>
            <div class="taxes-list">
                ${nonNullableCards.joinToString("")}
            </div>
        """.trimIndent()
    }
}
