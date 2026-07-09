package io.github.texport.superkassa.receiptrenderer.impl.renderer.component.common

import io.github.texport.superkassa.core.domain.model.kkm.*
import io.github.texport.superkassa.core.domain.model.receipt.*

import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.DocumentConstants
import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.escaped

internal object HeaderComponent {
    fun render(
        title: String,
        kkm: KkmInfo,
        lang: ReceiptLanguage,
        translateKey: (String) -> String,
        translateMixed: (String, String) -> String
    ): String {
        val logoUrl = kkm.branding.headerLogoUrl?.trim()
        val logoHtml = if (!logoUrl.isNullOrEmpty()) {
            """<div class="brand-logo-img"><img src="${logoUrl.escaped()}" alt="Logo" /></div>"""
        } else {
            ""
        }
        val adapter = io.github.texport.superkassa.receiptrenderer.impl.renderer.base.HtmlBrandingAdapter(kkm.branding)
        val beforeHeader = adapter.beforeHeaderHtml
        val customHeader = adapter.headerHtml
        val afterHeader = adapter.afterHeaderHtml

        val taxpayerName = kkm.ofdServiceInfo?.orgTitle ?: translateKey(DocumentConstants.DEFAULT_ORG_TITLE_KEY)
        val taxpayerBin = kkm.ofdServiceInfo?.orgInn
        val taxpayerAddress = when (lang) {
            ReceiptLanguage.RU -> kkm.ofdServiceInfo?.orgAddress
            ReceiptLanguage.KK -> kkm.ofdServiceInfo?.orgAddressKz ?: kkm.ofdServiceInfo?.orgAddress
            ReceiptLanguage.MIXED -> {
                val ru = kkm.ofdServiceInfo?.orgAddress?.escaped() ?: ""
                val kz = kkm.ofdServiceInfo?.orgAddressKz?.escaped() ?: ""
                if (ru.isNotEmpty() && kz.isNotEmpty()) {
                    translateMixed(ru, kz)
                } else {
                    ru.ifEmpty { kz }
                }
            }
        }

        val orgTitleHtml = "<div class=\"brand-logo\">${taxpayerName.escaped()}</div>"
        val orgBinHtml = if (!taxpayerBin.isNullOrBlank()) {
            "<div class=\"org-bin\">${translateKey("bin_iin")}: ${taxpayerBin.escaped()}</div>"
        } else {
            ""
        }
        val orgAddressHtml = if (!taxpayerAddress.isNullOrBlank()) {
            if (lang == ReceiptLanguage.MIXED) {
                "<div class=\"org-address\">$taxpayerAddress</div>"
            } else {
                "<div class=\"org-address\">${taxpayerAddress.escaped()}</div>"
            }
        } else {
            ""
        }

        return """
            <div class="center brand-header">
                $beforeHeader
                $logoHtml
                $customHeader
                $orgTitleHtml
                $orgBinHtml
                $orgAddressHtml
                $afterHeader
                <div class="rule"></div>
                <div class="doc-title">$title</div>
                <div class="doc-title-divider"></div>
            </div>
        """.trimIndent()
    }
}
