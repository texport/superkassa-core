package kz.mybrain.superkassa.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ReceiptLanguage {
    RU, // Russian only
    KK, // Kazakh only
    MIXED // Bilingual (Russian and Kazakh)
}

@Serializable
data class ReceiptBranding(
    val language: ReceiptLanguage = ReceiptLanguage.MIXED,
    val headerLogoUrl: String? = null,   // Logo URL or Base64 data URI
    val headerHtml: String? = null,      // Custom HTML text at the very top of the receipt
    val footerHtml: String? = null,      // Custom HTML text at the very bottom of the receipt
    val customCss: String? = null        // Custom CSS overrides
)
