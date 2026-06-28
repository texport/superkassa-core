package kz.mybrain.superkassa.core.domain.model.receipt

import kotlinx.serialization.Serializable

/**
 * Настройки брендирования, визуального оформления и ширины ленты чека.
 *
 * @property language Язык чека (казахский, русский или смешанный).
 * @property headerLogoUrl URL-ссылка на логотип в заголовке чека.
 * @property paperWidthMm Ширина чековой ленты в миллиметрах (например, 80 или 58).
 * @property themeColor Имя цветовой темы оформления чека.
 * @property beforeHeaderMsg Сообщение перед заголовком чека.
 * @property headerMsg Сообщение в заголовке чека.
 * @property afterHeaderMsg Сообщение после заголовка чека.
 * @property beforeItemsMsg Сообщение перед списком товаров/услуг.
 * @property afterItemsMsg Сообщение после списка товаров/услуг.
 * @property beforeTotalsMsg Сообщение перед итоговыми суммами.
 * @property afterTotalsMsg Сообщение после итоговых сумм.
 * @property beforeQrMsg Сообщение перед QR-кодом чека.
 * @property footerMsg Сообщение в подвале чека.
 * @property useForceDarkTheme Флаг принудительного использования темной темы чека.
 * @property customBackgroundColorHex Произвольный фоновый цвет в формате HEX.
 * @property customCardTopBorderColorHex Произвольный цвет верхней границы карточки чека в формате HEX.
 */
@Serializable
data class ReceiptBranding(
    val language: ReceiptLanguage = ReceiptLanguage.MIXED,
    val headerLogoUrl: String? = null,
    val paperWidthMm: Int = 80,
    val themeColor: String = "indigo",
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
    val customCardTopBorderColorHex: String? = null
)
