package io.github.texport.superkassa.core.presentation.api.model.kkm

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

@Serializable
@Schema(description = "Настройки брендирования и оформления печатной формы чека (ответ сервера)")
data class ReceiptBrandingResponse(
    @Schema(description = "Язык печатной формы чека", example = "MIXED")
    val language: ReceiptLanguage = ReceiptLanguage.MIXED,
    @Schema(description = "URL изображения-логотипа в заголовке чека", example = "https://example.com/logo.png")
    val headerLogoUrl: String? = null,
    @Schema(description = "Ширина бумажной ленты чека в миллиметрах (например, 58 или 80)", example = "80")
    val paperWidthMm: Int = 80,
    @Schema(description = "Цветовая тема оформления чека (например, indigo, emerald, amber)", example = "indigo")
    val themeColor: String = "indigo",
    @Schema(description = "Информационное сообщение над самым началом чека", example = "Добро пожаловать!")
    val beforeHeaderMsg: String? = null,
    @Schema(description = "Текстовое сообщение внутри заголовка чека", example = "Магазин 'Продукты'")
    val headerMsg: String? = null,
    @Schema(description = "Информационное сообщение сразу под заголовком чека", example = "Режим работы: 24/7")
    val afterHeaderMsg: String? = null,
    @Schema(description = "Информационное сообщение перед списком позиций", example = "Позиции покупки:")
    val beforeItemsMsg: String? = null,
    @Schema(description = "Информационное сообщение сразу после списка позиций", example = "--- Конец списка ---")
    val afterItemsMsg: String? = null,
    @Schema(description = "Информационное сообщение перед блоком итоговых сумм", example = "Итого к оплате:")
    val beforeTotalsMsg: String? = null,
    @Schema(description = "Информационное сообщение под блоком итоговых сумм", example = "Скидки учтены")
    val afterTotalsMsg: String? = null,
    @Schema(description = "Информационное сообщение перед QR-кодом чека", example = "Отсканируйте для проверки:")
    val beforeQrMsg: String? = null,
    @Schema(description = "Текстовое сообщение в самом низу (подвале) чека", example = "Спасибо, что выбрали нас!")
    val footerMsg: String? = null,
    @Schema(description = "Принудительно использовать темную тему оформления в электронном виде", example = "false")
    val useForceDarkTheme: Boolean = false,
    @Schema(description = "Кастомный цвет фона чека в формате HEX-кода", example = "#FFFFFF")
    val customBackgroundColorHex: String? = null,
    @Schema(description = "Кастомный цвет верхней плашки-границы чека в формате HEX-кода", example = "#4F46E5")
    val customCardTopBorderColorHex: String? = null,
    @Schema(description = "Список рекламных текстов ОФД для вывода на чеке")
    val ofdTicketAds: List<String> = emptyList(),
    @Schema(description = "Флаг необходимости печати рекламных блоков ОФД на чеке", example = "true")
    val printOfdTicketAds: Boolean = true
)
