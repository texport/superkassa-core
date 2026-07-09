package io.github.texport.superkassa.receiptrenderer.impl.renderer.component.common

internal object StatusBlockComponent {
    fun render(
        fiscalBadge: String,
        ofdBadge: String,
        errorReasonHtml: String
    ): String {
        return """
            <div class="status-chips-container">
                $fiscalBadge
                $ofdBadge
            </div>
            $errorReasonHtml
        """.trimIndent()
    }
}
