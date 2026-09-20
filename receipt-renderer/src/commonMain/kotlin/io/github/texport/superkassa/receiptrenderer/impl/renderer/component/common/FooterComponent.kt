package io.github.texport.superkassa.receiptrenderer.impl.renderer.component.common

import io.github.texport.superkassa.core.domain.api.model.kkm.*

internal object FooterComponent {
    fun render(
        kkm: KkmInfo,
        titleKey: String,
        translateInlineKey: (String) -> String
    ): String {
        val adapter = io.github.texport.superkassa.receiptrenderer.impl.renderer.base.HtmlBrandingAdapter(kkm.branding)
        val footerHtml = adapter.footerHtml
        val footerStatus = if (titleKey.contains("z_report")) {
            translateInlineKey("shift_closed")
        } else if (titleKey.contains("open_shift")) {
            translateInlineKey("shift_opened")
        } else {
            ""
        }

        val footerStatusHtml = if (footerStatus.isNotEmpty()) {
            "<div class=\"footer-item bold\">$footerStatus</div>"
        } else {
            ""
        }

        // Строки «Печатная форма документа» на ленте нет: покупателю она
        // ничего не сообщает, требованием КГД не предусмотрена и отнимала
        // место у того, что кассир печатает сам.
        return """
            <div class="footer center">
                $footerStatusHtml
                $footerHtml
            </div>
        """.trimIndent()
    }
}
