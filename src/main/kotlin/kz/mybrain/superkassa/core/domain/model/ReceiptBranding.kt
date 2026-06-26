package kz.mybrain.superkassa.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ReceiptLanguage {
    RU, // Russian only
    KK, // Kazakh only
    MIXED // Bilingual (Russian and Kazakh)
}

@Serializable
enum class ReceiptLayoutType {
    TAPE_80MM,
    TAPE_58MM,
    FULLSCREEN
}

@Serializable
data class ReceiptBranding(
    val language: ReceiptLanguage = ReceiptLanguage.MIXED,
    val headerLogoUrl: String? = null, // Logo URL or Base64 data URI
    val paperWidthMm: Int = 80, // Paper roll width in mm (58 or 80)
    val themeColor: String = "indigo",
    val beforeHeaderMsg: String? = null, // Custom message at the very top of the receipt (above logo)
    val headerMsg: String? = null, // Custom message text at the very top of the receipt
    val afterHeaderMsg: String? = null, // Custom message after organization info, before title
    val beforeItemsMsg: String? = null, // Custom message below meta table, before items
    val afterItemsMsg: String? = null, // Custom message after items list, before payments
    val beforeTotalsMsg: String? = null, // Custom message after payments, before totals
    val afterTotalsMsg: String? = null, // Custom message after totals, before OFD details
    val beforeQrMsg: String? = null, // Custom message after OFD details, before QR code
    val footerMsg: String? = null, // Custom message text at the very bottom of the receipt
    val useForceDarkTheme: Boolean = false, // Force dark theme mode
    val customBackgroundColorHex: String? = null, // Optional custom background color hex
    val customCardTopBorderColorHex: String? = null // Optional custom card top border color hex
)
