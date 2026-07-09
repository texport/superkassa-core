package io.github.texport.superkassa.receiptrenderer.impl.renderer.component.ticket

import io.github.texport.superkassa.core.domain.model.receipt.*

import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.escaped
import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.formatted

internal object ParentTicketComponent {
    fun render(
        parent: ParentTicket,
        parentTitle: String,
        formattedDateTime: String,
        t: (String) -> String
    ): String {
        val parentTotalStr = parent.parentTicketTotal.formatted()
        val escapedKgdKkmId = parent.kgdKkmId.escaped()
        val receiptNoLabel = t("fiscal_sign")
        val dateTimeLabel = t("date_time")
        val kkmIdLabel = t("rnm")
        val receiptSumLabel = t("receipt_sum")
        val cardHtml = io.github.texport.superkassa.receiptrenderer.impl.renderer.component.common.CardComponent.render(
            title = parentTitle,
            rows = listOf(
                io.github.texport.superkassa.receiptrenderer.impl.renderer.component.common.CardComponent.Row(
                    label = receiptNoLabel,
                    value = parent.parentTicketNumber.toString(),
                    valueClass = "tax-sum-cell num bold",
                    valueStyle = "color: var(--m3-on-surface);"
                ),
                io.github.texport.superkassa.receiptrenderer.impl.renderer.component.common.CardComponent.Row(
                    label = dateTimeLabel,
                    value = formattedDateTime,
                    valueClass = "tax-sum-cell num",
                    valueStyle = "color: var(--m3-on-surface);"
                ),
                io.github.texport.superkassa.receiptrenderer.impl.renderer.component.common.CardComponent.Row(
                    label = kkmIdLabel,
                    value = escapedKgdKkmId,
                    valueClass = "tax-sum-cell num",
                    valueStyle = "color: var(--m3-on-surface);"
                ),
                io.github.texport.superkassa.receiptrenderer.impl.renderer.component.common.CardComponent.Row(
                    label = receiptSumLabel,
                    value = parentTotalStr,
                    valueClass = "tax-sum-cell num bold"
                )
            )
        )
        return """
        $cardHtml
        <div class="rule"></div>
        """.trimIndent()
    }
}
