package io.github.texport.superkassa.receiptrenderer.impl.renderer.base

import io.github.texport.superkassa.core.domain.model.kkm.*
import io.github.texport.superkassa.core.domain.model.receipt.*
import io.github.texport.superkassa.receiptrenderer.impl.renderer.style.MetaDocumentStyles
import io.github.texport.superkassa.receiptrenderer.impl.renderer.style.SharedStyles
import io.github.texport.superkassa.core.string.api.CoreStrings

data class StandardDocumentInput(
    val titleKey: String,
    val kkm: KkmInfo,
    val createdAt: Long,
    val shiftNo: Long?,
    val docNo: String?,
    val ofdStatus: String?,
    val isFiscal: Boolean,
    val isAutonomous: Boolean = false,
    val fiscalSign: String? = null,
    val ofdProvider: String? = null,
    val receiptUrl: String? = null,
    val qrDataUri: String? = null,
    val additionalMeta: List<Pair<String, String>> = emptyList(),
    val docCss: String = "",
    val showOfdStatus: Boolean = true,
    val bodyContent: String
)

abstract class BaseDocumentRenderer {

    protected fun getTranslationMap(key: String): Map<String, String>? {
        return ReceiptTranslator.getTranslationMap(key)
    }

    protected fun formatDate(epochMillis: Long): String {
        return io.github.texport.superkassa.receiptrenderer.impl.ReceiptFormatter.formatDate(epochMillis)
    }

    protected fun translate(key: String, lang: ReceiptLanguage): String {
        return ReceiptTranslator.translate(key, lang)
    }

    protected fun translate(labelRu: String, labelKk: String, lang: ReceiptLanguage): String {
        return ReceiptTranslator.translate(labelRu, labelKk, lang)
    }

    protected fun translateInline(key: String, lang: ReceiptLanguage): String {
        return ReceiptTranslator.translateInline(key, lang)
    }

    protected fun translateInline(key: String, lang: ReceiptLanguage, separator: String = " / "): String {
        return ReceiptTranslator.translateInline(key, lang, separator)
    }

    protected fun translateMixedInline(labelRu: String, labelKk: String, lang: ReceiptLanguage, separator: String = " / "): String {
        return ReceiptTranslator.translateInline(labelRu, labelKk, lang, separator)
    }

    protected fun renderOfdStatus(ofdStatus: String?, lang: ReceiptLanguage, isFiscal: Boolean = true): String {
        if (ofdStatus == null && !isFiscal) {
            val trans = getTranslationMap("status_not_transmitted")
            return renderStatusBadge(
                "neutral",
                trans?.get("ru") ?: "Not Transmitted",
                trans?.get("kk") ?: "Not Transmitted",
                lang
            )
        }
        return when (ofdStatus) {
            "DELIVERED", "SENT" -> {
                val trans = getTranslationMap("status_sent")
                renderStatusBadge(
                    "success",
                    trans?.get("ru") ?: "Sent",
                    trans?.get("kk") ?: "Sent",
                    lang
                )
            }
            "PENDING", "OFFLINE" -> {
                val trans = getTranslationMap("status_offline")
                renderStatusBadge(
                    "warning",
                    trans?.get("ru") ?: "Offline",
                    trans?.get("kk") ?: "Offline",
                    lang
                )
            }
            "FAILED", "ERROR" -> {
                renderStatusBadge(
                    "error",
                    CoreStrings.statusError().ru,
                    CoreStrings.statusError().kk,
                    lang
                )
            }
            else -> {
                val status = ofdStatus ?: "-"
                renderStatusBadge(
                    "warning",
                    status,
                    status,
                    lang
                )
            }
        }
    }

    protected fun renderStatusBadge(
        classSuffix: String,
        labelRu: String,
        labelKk: String,
        lang: ReceiptLanguage
    ): String {
        val content = when (lang) {
            ReceiptLanguage.RU -> labelRu
            ReceiptLanguage.KK -> labelKk
            ReceiptLanguage.MIXED -> "$labelKk / $labelRu"
        }
        return TemplateRenderer.render(
            "status_badge.html",
            mapOf("suffix" to classSuffix, "content" to content)
        )
    }

    protected fun summaryRow(label: String, valStr: String): String {
        return "<tr><td>$label</td><td>$valStr</td></tr>"
    }

    protected fun formatAmount(bills: Long): String =
        io.github.texport.superkassa.receiptrenderer.impl.ReceiptFormatter.formatCents(bills)

    protected fun renderPageFrame(title: String, bodyContent: String, kkm: KkmInfo, docCss: String = ""): String {
        val cleanTitle = title.replace(Regex("<[^>]*>"), "")
        val widthMm = kkm.branding.paperWidthMm
        val pageRule = if (widthMm == 0) "size: auto;" else "size: ${widthMm}mm auto; margin: 0;"
        val widthClass = if (widthMm == 0) "tape-fullscreen" else "tape-${widthMm}mm"
        val brandingAdapter = HtmlBrandingAdapter(kkm.branding)
        val customCss = brandingAdapter.customCss
        val themeColor = kkm.branding.themeColor
        val themeMode = if (kkm.branding.useForceDarkTheme || customCss.contains("/* force-dark */") || customCss.contains("theme-dark")) "dark" else "light"
        val bodyClass = "$themeMode accent-$themeColor"

        return TemplateRenderer.render(
            "page_frame.html",
            mapOf(
                "title" to cleanTitle,
                "pageRule" to pageRule,
                "sharedCss" to SharedStyles.SHARED_CSS,
                "docCss" to docCss,
                "customCss" to customCss,
                "bodyClass" to bodyClass,
                "widthClass" to widthClass,
                "bodyContent" to bodyContent
            )
        )
    }

    protected fun renderStandardDocument(input: StandardDocumentInput): String {
        val kkm = input.kkm
        val lang = kkm.branding.language
        val docTitle = translate(input.titleKey, lang)

        val headerHtml = io.github.texport.superkassa.receiptrenderer.impl.renderer.component.common.HeaderComponent.render(
            title = docTitle,
            kkm = kkm,
            lang = lang,
            translateKey = { translate(it, lang) },
            translateMixed = { ru, kk -> translate(ru, kk, lang) }
        )

        val fiscalBadge = if (input.isFiscal) {
            val trans = getTranslationMap("status_fiscal")
            val ru = trans?.get("ru") ?: "Fiscal"
            val kk = trans?.get("kk") ?: "Fiscal"
            renderStatusBadge("success", ru, kk, lang)
        } else {
            val trans = getTranslationMap("status_not_fiscal")
            val ru = trans?.get("ru") ?: "Non-Fiscal"
            val kk = trans?.get("kk") ?: "Non-Fiscal"
            renderStatusBadge("warning", ru, kk, lang)
        }

        val ofdBadge = if (input.showOfdStatus) renderOfdStatus(input.ofdStatus, lang, input.isFiscal) else ""

        val errorReasonHtml = if (input.ofdStatus == "FAILED" || input.ofdStatus == "ERROR") {
            val trans = CoreStrings.ofdErrorReason().localize(lang.name.lowercase())
            "<div class=\"status-error-reason\">$trans</div>"
        } else {
            ""
        }

        val statusBlockHtml = io.github.texport.superkassa.receiptrenderer.impl.renderer.component.common.StatusBlockComponent.render(
            fiscalBadge = fiscalBadge,
            ofdBadge = ofdBadge,
            errorReasonHtml = errorReasonHtml
        )

        val metaCardHtml = io.github.texport.superkassa.receiptrenderer.impl.renderer.component.common.KkmMetadataComponent.render(
            kkm = kkm,
            shiftNo = input.shiftNo,
            docNo = input.docNo,
            formattedDateTime = formatDate(input.createdAt),
            additionalMeta = input.additionalMeta,
            translateInlineKey = { translateInline(it, lang) }
        )

        val brandingAdapter = HtmlBrandingAdapter(kkm.branding)
        val fiscalSectionHtml = io.github.texport.superkassa.receiptrenderer.impl.renderer.component.common.FiscalSectionComponent.render(
            isAutonomous = input.isAutonomous,
            fiscalSign = input.fiscalSign,
            ofdProvider = input.ofdProvider,
            receiptUrl = input.receiptUrl,
            qrDataUri = input.qrDataUri,
            beforeQrHtml = brandingAdapter.beforeQrHtml,
            translateInlineKey = { translateInline(it, lang) }
        )

        val ofdAdsHtml = if (kkm.branding.printOfdTicketAds && kkm.branding.ofdTicketAds.isNotEmpty()) {
            val adsList = kkm.branding.ofdTicketAds.joinToString("\n") {
                "<div class=\"ofd-ad-item\">${it.escaped()}</div>"
            }
            """
            <div class="ofd-ads center muted" style="font-size: 0.85em; color: var(--m3-on-surface-variant); line-height: 1.4; padding: 4px 0; margin-top: 8px;">
                $adsList
            </div>
            <div class="rule"></div>
            """.trimIndent()
        } else {
            ""
        }

        val footerHtml = io.github.texport.superkassa.receiptrenderer.impl.renderer.component.common.FooterComponent.render(
            kkm = kkm,
            titleKey = input.titleKey,
            translateInlineKey = { translateInline(it, lang) }
        )

        val body = """
            $headerHtml
            $statusBlockHtml
            $metaCardHtml
            <div class="rule"></div>
            ${input.bodyContent}
            $fiscalSectionHtml
            <div class="rule"></div>
            $ofdAdsHtml
            $footerHtml
        """.trimIndent()

        val fullCss = input.docCss + "\n" + MetaDocumentStyles.META_DOCUMENT_CSS
        return renderPageFrame(translate(input.titleKey, lang), body, kkm, fullCss)
    }
}
