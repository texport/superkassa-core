package io.github.texport.superkassa.receiptrenderer.impl.renderer.report

import io.github.texport.superkassa.core.domain.api.model.kkm.*
import io.github.texport.superkassa.core.domain.api.model.shift.*

import io.github.texport.superkassa.core.domain.impl.helper.zxreport.ZxReportBuilder
import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.BaseDocumentRenderer
import io.github.texport.superkassa.receiptrenderer.impl.renderer.component.report.ReportCashOpsComponent
import io.github.texport.superkassa.receiptrenderer.impl.renderer.component.report.ReportCashOpsInput
import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.StandardDocumentInput
import io.github.texport.superkassa.receiptrenderer.impl.renderer.component.report.ReportNonNullableComponent
import io.github.texport.superkassa.receiptrenderer.impl.renderer.component.report.ReportPaymentsComponent
import io.github.texport.superkassa.receiptrenderer.impl.renderer.component.report.ReportSectionsComponent
import io.github.texport.superkassa.receiptrenderer.impl.renderer.component.report.ReportTaxesComponent

import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.MetadataBuilder

abstract class ZxReportCommonRenderer : BaseDocumentRenderer() {

    protected fun renderZxReportHtml(
        titleKey: String,
        shift: ShiftInfo,
        counters: Map<String, Long>,
        isZReport: Boolean,
        kkm: KkmInfo,
        ofdStatus: String?,
        docNo: String? = null
    ): String {
        val lang = kkm.branding.language
        fun t(key: String): String = translate(key, lang)
        fun translateInlineKey(key: String): String = translateInline(key, lang)

        val reportInput = ZxReportBuilder.build(
            counters = counters,
            dateTimeMillis = shift.closedAt ?: kotlin.time.Clock.System.now().toEpochMilliseconds(),
            shiftNumber = shift.shiftNo.toInt(),
            openShiftTimeMillis = shift.openedAt,
            closeShiftTimeMillis = shift.closedAt
        )

        // 1. Meta-information setup for the base class
        val additionalMeta = MetadataBuilder { translateInlineKey(it) }.apply {
            addRaw("opened", formatDate(reportInput.openShiftTimeMillis))
            if (isZReport) {
                val closedStr = reportInput.closeShiftTimeMillis?.let { formatDate(it) } ?: "-"
                addRaw("closed", closedStr)
            }
            addRaw("report_time", formatDate(reportInput.dateTimeMillis))
        }.build()

        // 2. Operations (Sales, Returns, Purchases, Purchase Returns)
        val operationsHtml = ReportSectionsComponent.renderOperations(
            operations = reportInput.operations,
            t = { t(it) },
            translateInlineKey = { translateInlineKey(it) },
            formatAmount = { formatAmount(it) }
        )

        // 3. Section totals (Departments)
        val sectionHtml = ReportSectionsComponent.renderSections(
            sections = reportInput.sections,
            t = { t(it) },
            translateInlineKey = { translateInlineKey(it) },
            formatAmount = { formatAmount(it) }
        )

        // 4. Discounts and markups
        val discountsMarkupsHtml = ReportSectionsComponent.renderDiscountsMarkups(
            discounts = reportInput.discounts,
            markups = reportInput.markups,
            t = { t(it) },
            translateInlineKey = { translateInlineKey(it) },
            formatAmount = { formatAmount(it) }
        )

        // 5. Total result
        val totalResultHtml = ReportSectionsComponent.renderTotalResult(
            totalResult = reportInput.totalResult,
            t = { t(it) },
            translateInlineKey = { translateInlineKey(it) },
            formatAmount = { formatAmount(it) }
        )

        // 6. Taxes
        val taxesHtml = ReportTaxesComponent.render(
            taxes = reportInput.taxes,
            t = { t(it) },
            translateInlineKey = { translateInlineKey(it) },
            formatAmount = { formatAmount(it) }
        )

        // 7. Payment types summary
        val paymentsSummaryHtml = ReportPaymentsComponent.render(
            ticketOperations = reportInput.ticketOperations,
            t = { t(it) },
            translateInlineKey = { translateInlineKey(it) },
            formatAmount = { formatAmount(it) }
        )

        // 8. Non-nullable sums
        val nonNullableHtml = ReportNonNullableComponent.render(
            nonNullableSums = reportInput.nonNullableSums,
            startShiftNonNullableSums = reportInput.startShiftNonNullableSums,
            t = { t(it) },
            translateInlineKey = { translateInlineKey(it) },
            formatAmount = { formatAmount(it) }
        )

        // 9. Cash and revenue operations
        val cashInPl = reportInput.moneyPlacements.firstOrNull { op -> op.operation == "MONEY_PLACEMENT_DEPOSIT" }
        val cashOutPl = reportInput.moneyPlacements.firstOrNull { op -> op.operation == "MONEY_PLACEMENT_WITHDRAWAL" }
        val cashInSum = cashInPl?.operationsSumBills ?: 0L
        val cashInCount = cashInPl?.operationsCount ?: 0L
        val cashOutSum = cashOutPl?.operationsSumBills ?: 0L
        val cashOutCount = cashOutPl?.operationsCount ?: 0L

        val cashOperationsHtml = ReportCashOpsComponent.render(
            input = ReportCashOpsInput(
                cashInCount = cashInCount,
                cashInSum = cashInSum,
                cashOutCount = cashOutCount,
                cashOutSum = cashOutSum,
                cashSumBills = reportInput.cashSumBills,
                revenueBills = reportInput.revenueBills
            ),
            t = { t(it) },
            translateInlineKey = { translateInlineKey(it) },
            formatAmount = { formatAmount(it) }
        )

        val bodyContent = """
            $operationsHtml
            $sectionHtml
            $discountsMarkupsHtml
            $totalResultHtml
            $taxesHtml
            $paymentsSummaryHtml
            $nonNullableHtml
            <div class="rule"></div>
            $cashOperationsHtml
        """.trimIndent()

        return renderStandardDocument(
            StandardDocumentInput(
                titleKey = titleKey,
                kkm = kkm,
                createdAt = reportInput.dateTimeMillis,
                shiftNo = shift.shiftNo,
                docNo = docNo,
                ofdStatus = ofdStatus,
                isFiscal = isZReport,
                additionalMeta = additionalMeta,
                bodyContent = bodyContent
            )
        )
    }
}
