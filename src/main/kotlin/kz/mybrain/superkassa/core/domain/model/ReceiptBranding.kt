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
    val customCss: String? = null,        // Custom CSS overrides
    val paperWidthMm: Int = 80,          // Paper roll width in mm (58 or 80)
    val beforeHeaderHtml: String? = null, // HTML at the very top of the receipt (above logo)
    val afterHeaderHtml: String? = null,  // HTML after organization info, before title
    val beforeItemsHtml: String? = null,  // HTML below meta table, before items
    val afterItemsHtml: String? = null,   // HTML after items list, before payments
    val beforeTotalsHtml: String? = null, // HTML after payments, before totals
    val afterTotalsHtml: String? = null,  // HTML after totals, before OFD details
    val beforeQrHtml: String? = null,     // HTML after OFD details, before QR code
    val themeColor: String = "indigo"
)
