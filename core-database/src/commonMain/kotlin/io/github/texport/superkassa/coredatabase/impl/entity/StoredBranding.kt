package io.github.texport.superkassa.coredatabase.impl.entity

import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptBranding
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptLanguage
import io.github.texport.superkassa.core.domain.api.model.receipt.TicketAd
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Оформление чека в том виде, в каком его хранит узел в `branding_json`.
 *
 * Имена полей и умолчания те же, что у представления оформления узла:
 * строка, перенесённая из узла, читается здесь как есть, а записанная
 * здесь — узлом. Пустое поле означает умолчание, а не «стереть».
 */
@Serializable
internal data class StoredBranding(
    val language: String = ReceiptLanguage.MIXED.name,
    val headerLogoUrl: String? = null,
    val paperWidthMm: Int = DEFAULT_PAPER_WIDTH_MM,
    val themeColor: String = DEFAULT_THEME_COLOR,
    val beforeHeaderMsg: String? = null,
    val headerMsg: String? = null,
    val afterHeaderMsg: String? = null,
    val beforeItemsMsg: String? = null,
    val afterItemsMsg: String? = null,
    val beforeTotalsMsg: String? = null,
    val afterTotalsMsg: String? = null,
    val beforeQrMsg: String? = null,
    val footerMsg: String? = null,
    val useForceDarkTheme: Boolean = false,
    val customBackgroundColorHex: String? = null,
    val customCardTopBorderColorHex: String? = null,
    val ofdTicketAds: List<StoredTicketAd> = emptyList(),
    val printOfdTicketAds: Boolean = true
)

/** Рекламная строка ОФД на чеке. */
@Serializable
internal data class StoredTicketAd(
    val type: String = DEFAULT_AD_TYPE,
    val version: Long = 0L,
    val text: String
)

private const val DEFAULT_PAPER_WIDTH_MM = 80
private const val DEFAULT_THEME_COLOR = "indigo"
private const val DEFAULT_AD_TYPE = "TICKET_AD_OFD"

private val brandingJson = Json { ignoreUnknownKeys = true }

/** Строка оформления для колонки кассы. */
internal fun ReceiptBranding.toStoredJson(): String = brandingJson.encodeToString(
    StoredBranding.serializer(),
    StoredBranding(
        language.name, headerLogoUrl, paperWidthMm, themeColor, beforeHeaderMsg, headerMsg, afterHeaderMsg,
        beforeItemsMsg, afterItemsMsg, beforeTotalsMsg, afterTotalsMsg, beforeQrMsg, footerMsg,
        useForceDarkTheme, customBackgroundColorHex, customCardTopBorderColorHex,
        ofdTicketAds.map { StoredTicketAd(it.type, it.version, it.text) }, printOfdTicketAds
    )
)

/**
 * Оформление из колонки кассы.
 *
 * Пустая колонка — касса, заведённая до хранения оформления: у неё
 * оформление по умолчанию. Неразборная строка тоже даёт умолчание, как
 * у узла: оформление не фискально, и касса из-за него вставать не должна.
 */
internal fun brandingOf(json: String?): ReceiptBranding {
    if (json.isNullOrBlank()) return ReceiptBranding()
    val stored = try {
        brandingJson.decodeFromString(StoredBranding.serializer(), json)
    } catch (_: SerializationException) {
        return ReceiptBranding()
    } catch (_: IllegalArgumentException) {
        return ReceiptBranding()
    }
    return stored.toDomain()
}

/**
 * Оформление из строки без поблажек: неразборная строка или незнакомый
 * язык чека — [IllegalArgumentException], а не умолчание.
 */
internal fun strictBrandingOf(json: String): ReceiptBranding {
    val stored = brandingJson.decodeFromString(StoredBranding.serializer(), json)
    require(ReceiptLanguage.entries.any { it.name == stored.language }) { "Unknown receipt language" }
    return stored.toDomain()
}

private fun StoredBranding.toDomain() = ReceiptBranding(
    ReceiptLanguage.entries.firstOrNull { it.name == language } ?: ReceiptLanguage.MIXED,
    headerLogoUrl, paperWidthMm, themeColor, beforeHeaderMsg, headerMsg, afterHeaderMsg,
    beforeItemsMsg, afterItemsMsg, beforeTotalsMsg, afterTotalsMsg, beforeQrMsg, footerMsg,
    useForceDarkTheme, customBackgroundColorHex, customCardTopBorderColorHex,
    ofdTicketAds.map { TicketAd(it.type, it.version, it.text) }, printOfdTicketAds
)
