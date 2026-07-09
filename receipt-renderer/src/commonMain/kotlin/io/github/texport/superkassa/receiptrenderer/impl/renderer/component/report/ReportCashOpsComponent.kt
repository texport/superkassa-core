package io.github.texport.superkassa.receiptrenderer.impl.renderer.component.report

data class ReportCashOpsInput(
    val cashInCount: Long,
    val cashInSum: Long,
    val cashOutCount: Long,
    val cashOutSum: Long,
    val cashSumBills: Long,
    val revenueBills: Long
)

internal object ReportCashOpsComponent {
    fun render(
        input: ReportCashOpsInput,
        t: (String) -> String,
        translateInlineKey: (String) -> String,
        formatAmount: (Long) -> String
    ): String {
        val revenueSign = if (input.revenueBills < 0) "-" else ""
        val revenueSumStr = formatAmount(kotlin.math.abs(input.revenueBills))

        val cashOpsTitle = translateInlineKey("cash_operations")
        val depLabel = t("deposits_count")
        val depSumStr = formatAmount(input.cashInSum)
        val depSumLabel = t("deposited_total")
        val wdrLabel = t("withdrawals_count")
        val wdrSumStr = formatAmount(input.cashOutSum)
        val wdrSumLabel = t("withdrawn_total")
        val cashSumLabel = t("cash_in_drawer")
        val cashSumStr = formatAmount(input.cashSumBills)
        val revenueLabel = t("shift_revenue")

        return """
            <fieldset class="section-card">
                <legend class="card-label">$cashOpsTitle</legend>
                <table class="meta-table" style="margin-top: 4px;">
                    <tr><td>$depLabel</td><td>${input.cashInCount}</td></tr>
                    <tr><td>$depSumLabel</td><td>$depSumStr</td></tr>
                    <tr><td>$wdrLabel</td><td>${input.cashOutCount}</td></tr>
                    <tr><td>$wdrSumLabel</td><td>$wdrSumStr</td></tr>
                    <tr class="bold"><td>$cashSumLabel</td><td>$cashSumStr</td></tr>
                    <tr class="bold"><td>$revenueLabel</td><td>$revenueSign$revenueSumStr</td></tr>
                </table>
            </fieldset>
        """.trimIndent()
    }
}
