package io.github.texport.superkassa.receiptrenderer.impl.renderer.component.report

import io.github.texport.superkassa.core.domain.api.model.zxreport.OperationAggregate
import io.github.texport.superkassa.core.domain.api.model.zxreport.SectionAggregate
import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.toDiscountKey
import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.toMarkupKey
import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.toOperationKey
import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.toTotalResultKey

internal object ReportSectionsComponent {

    fun renderOperations(
        operations: List<OperationAggregate>,
        t: (String) -> String,
        translateInlineKey: (String) -> String,
        formatAmount: (Long) -> String
    ): String {
        val cards = operations.joinToString("") { op ->
            val label = translateInlineKey(op.operation.toOperationKey())
            val totalSumStr = formatAmount(op.sumBills)
            """
            <fieldset class="tax-row-card">
                <legend class="card-label">$label</legend>
                <table class="tax-row-table">
                    <tr>
                        <td class="tax-details-cell">${t("sum")}</td>
                        <td class="tax-sum-cell bold">$totalSumStr</td>
                    </tr>
                    <tr>
                        <td class="tax-details-cell">${t("checks")}</td>
                        <td class="tax-sum-cell" style="font-weight: 600; color: var(--m3-on-surface);">${op.count}</td>
                    </tr>
                </table>
            </fieldset>
            """.trimIndent()
        }
        return """
            <div class="section-title">${translateInlineKey("results_operations")}</div>
            <div class="taxes-list">
                $cards
            </div>
        """.trimIndent()
    }

    fun renderSections(
        sections: List<SectionAggregate>,
        t: (String) -> String,
        translateInlineKey: (String) -> String,
        formatAmount: (Long) -> String
    ): String {
        if (sections.isEmpty()) return ""
        val secCards = sections.flatMap { sec ->
            sec.operations.map { op ->
                val opLabel = translateInlineKey(op.operation.toOperationKey())
                val cardTitle = "${translateInlineKey("department")} ${sec.sectionCode} - $opLabel"
                val totalSumStr = formatAmount(op.sumBills)
                """
                <fieldset class="tax-row-card">
                    <legend class="card-label">$cardTitle</legend>
                    <table class="tax-row-table">
                        <tr>
                            <td class="tax-details-cell">${t("sum")}</td>
                            <td class="tax-sum-cell bold">$totalSumStr</td>
                        </tr>
                        <tr>
                            <td class="tax-details-cell">${t("checks")}</td>
                            <td class="tax-sum-cell" style="font-weight: 600; color: var(--m3-on-surface);">${op.count}</td>
                        </tr>
                    </table>
                </fieldset>
                """.trimIndent()
            }
        }.joinToString("")

        return """
            <div class="section-title">${translateInlineKey("by_departments")}</div>
            <div class="taxes-list">
                $secCards
            </div>
        """.trimIndent()
    }

    fun renderDiscountsMarkups(
        discounts: List<OperationAggregate>,
        markups: List<OperationAggregate>,
        t: (String) -> String,
        translateInlineKey: (String) -> String,
        formatAmount: (Long) -> String
    ): String {
        val discountCards = discounts.map { op ->
            val mappedKey = op.operation.toDiscountKey()
            val label = if (mappedKey == op.operation) {
                "${translateInlineKey("discounts")} (${op.operation})"
            } else {
                translateInlineKey(mappedKey)
            }
            val formattedSum = formatAmount(op.sumBills)
            """
            <fieldset class="tax-row-card">
                <legend class="card-label">$label</legend>
                <table class="tax-row-table">
                    <tr>
                        <td class="tax-details-cell">${t("sum")}</td>
                        <td class="tax-sum-cell" style="color: var(--m3-error); font-weight: bold;">-$formattedSum</td>
                    </tr>
                </table>
            </fieldset>
            """.trimIndent()
        }
        val markupCards = markups.map { op ->
            val mappedKey = op.operation.toMarkupKey()
            val label = if (mappedKey == op.operation) {
                "${translateInlineKey("markups")} (${op.operation})"
            } else {
                translateInlineKey(mappedKey)
            }
            val formattedSum = formatAmount(op.sumBills)
            """
            <fieldset class="tax-row-card">
                <legend class="card-label">$label</legend>
                <table class="tax-row-table">
                    <tr>
                        <td class="tax-details-cell">${t("sum")}</td>
                        <td class="tax-sum-cell" style="color: var(--m3-success); font-weight: bold;">+$formattedSum</td>
                    </tr>
                </table>
            </fieldset>
            """.trimIndent()
        }
        return """
            <div class="section-title">${translateInlineKey("discounts_markups")}</div>
            <div class="taxes-list">
                ${discountCards.joinToString("")}
                ${markupCards.joinToString("")}
            </div>
        """.trimIndent()
    }

    fun renderTotalResult(
        totalResult: List<OperationAggregate>,
        t: (String) -> String,
        translateInlineKey: (String) -> String,
        formatAmount: (Long) -> String
    ): String {
        val totalResultCards = totalResult.joinToString("") { op ->
            val label = translateInlineKey(op.operation.toTotalResultKey())
            val formattedSum = formatAmount(op.sumBills)
            """
            <fieldset class="tax-row-card highlighted">
                <legend class="card-label">$label</legend>
                <table class="tax-row-table">
                    <tr>
                        <td class="tax-details-cell">${t("sum")}</td>
                        <td class="tax-sum-cell bold" style="font-size: 1.05em;">$formattedSum</td>
                    </tr>
                    <tr>
                        <td class="tax-details-cell">${t("checks")}</td>
                        <td class="tax-sum-cell" style="font-weight: 600; color: var(--m3-on-surface);">${op.count}</td>
                    </tr>
                </table>
            </fieldset>
            """.trimIndent()
        }
        return """
            <div class="section-title">${translateInlineKey("final_results")}</div>
            <div class="taxes-list">
                $totalResultCards
            </div>
        """.trimIndent()
    }
}
