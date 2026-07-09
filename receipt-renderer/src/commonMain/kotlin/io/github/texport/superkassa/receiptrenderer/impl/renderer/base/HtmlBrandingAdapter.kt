package io.github.texport.superkassa.receiptrenderer.impl.renderer.base

import io.github.texport.superkassa.core.domain.model.receipt.*

internal class HtmlBrandingAdapter(private val branding: ReceiptBranding) {
    val beforeHeaderHtml: String get() = wrap(branding.beforeHeaderMsg, "custom-before-header-container")
    val headerHtml: String get() = wrap(branding.headerMsg, "custom-header-container")
    val afterHeaderHtml: String get() = wrap(branding.afterHeaderMsg, "custom-after-header-container")
    val beforeItemsHtml: String get() = wrap(branding.beforeItemsMsg, "custom-before-items-container")
    val afterItemsHtml: String get() = wrap(branding.afterItemsMsg, "custom-after-items-container")
    val beforeTotalsHtml: String get() = wrap(branding.beforeTotalsMsg, "custom-before-totals-container")
    val afterTotalsHtml: String get() = wrap(branding.afterTotalsMsg, "custom-after-totals-container")
    val beforeQrHtml: String get() = wrap(branding.beforeQrMsg, "custom-before-qr-container")
    val footerHtml: String get() = wrap(branding.footerMsg, "custom-footer-container")

    val customCss: String get() {
        val sb = StringBuilder()
        if (branding.useForceDarkTheme) {
            sb.append("/* force-dark */\n")
            sb.append("body { background-color: #1e1d20; }\n")
            sb.append(".receipt-card { box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.5); }\n")
        } else {
            sb.append("body { background-color: #faf8f5; }\n")
            sb.append(".receipt-card { box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1); }\n")
        }

        branding.customBackgroundColorHex?.let { color ->
            sb.append("body { background-color: $color !important; }\n")
        }

        val borderColor = branding.customCardTopBorderColorHex
            ?: if (branding.useForceDarkTheme) "#ffb833" else "#d97706"
        sb.append(".receipt-card { border-top: 5px solid $borderColor !important; }\n")

        return sb.toString()
    }

    private fun wrap(rawMsg: String?, cssClass: String): String {
        val trimmed = rawMsg?.trim() ?: ""
        if (trimmed.isEmpty()) return ""
        // Keep fallback for legacy HTML strings (like starting/ending with < and >)
        return if (trimmed.startsWith("<") && trimmed.endsWith(">")) {
            "<div class=\"$cssClass\">$trimmed</div>"
        } else {
            // Escape plain text and convert newlines to <br/>
            val escaped = trimmed
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replace("\n", "<br/>")
            "<div class=\"$cssClass\">$escaped</div>"
        }
    }
}
