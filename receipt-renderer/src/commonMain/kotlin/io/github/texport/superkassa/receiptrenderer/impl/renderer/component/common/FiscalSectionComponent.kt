package io.github.texport.superkassa.receiptrenderer.impl.renderer.component.common

import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.escaped

internal object FiscalSectionComponent {
    fun render(
        isAutonomous: Boolean,
        fiscalSign: String?,
        ofdProvider: String?,
        receiptUrl: String?,
        qrDataUri: String?,
        beforeQrHtml: String = "",
        translateInlineKey: (String) -> String
    ): String {
        val fiscalSignHtml = if (!fiscalSign.isNullOrBlank()) {
            val label = translateInlineKey("fiscal_sign")
            "<tr><td>$label</td><td class=\"bold\">${fiscalSign.escaped()}</td></tr>"
        } else {
            ""
        }

        val ofdProviderHtml = if (!ofdProvider.isNullOrBlank()) {
            val label = translateInlineKey("ofd_provider")
            "<tr><td>$label</td><td>$ofdProvider</td></tr>"
        } else {
            ""
        }

        val autonomousHtml = if (isAutonomous) {
            val label = translateInlineKey("autonomous_mode")
            "<div class=\"muted center\">$label</div>"
        } else {
            ""
        }

        val linkHtml = if (!receiptUrl.isNullOrBlank()) {
            val label = translateInlineKey("receipt_url_label")
            val escUrl = receiptUrl.escaped()
            """
            <div class="receipt-link center">
                $label 
                <a href="$escUrl" target="_blank">$escUrl</a>
            </div>
            """.trimIndent()
        } else {
            ""
        }

        val qrCodeHtml = if (!qrDataUri.isNullOrBlank()) {
            val scanLabel = translateInlineKey("scan_to_verify")
            """
            $beforeQrHtml
            <div class="qr">
                <img src="$qrDataUri" alt="QR Link" />
                <div style="font-size: 0.85em; color: var(--m3-on-surface-variant); margin-top: 6px; text-transform: uppercase;">$scanLabel</div>
            </div>
            """.trimIndent()
        } else {
            ""
        }

        val hasFiscalMeta = listOf(fiscalSignHtml, ofdProviderHtml, autonomousHtml, linkHtml, qrCodeHtml)
            .any { it.isNotEmpty() }

        return if (hasFiscalMeta) {
            """
            <div class="rule"></div>
            <table class="meta-table">
                $fiscalSignHtml
                $ofdProviderHtml
            </table>
            $autonomousHtml
            $linkHtml
            $qrCodeHtml
            """.trimIndent()
        } else {
            ""
        }
    }
}
