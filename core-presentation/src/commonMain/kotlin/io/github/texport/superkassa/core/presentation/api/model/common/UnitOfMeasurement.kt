package io.github.texport.superkassa.core.presentation.api.model.common

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Единицы измерения номенклатурных позиций чека, классификатор ИС ЭСФ.
 */
@Serializable
@Schema(description = "Единица измерения")
enum class UnitOfMeasurement {
    /** Штука */
    @Schema(description = "Штука")
    PIECE,

    /** Килограмм */
    @Schema(description = "Килограмм")
    KILOGRAM,

    /** Услуга */
    @Schema(description = "Услуга")
    SERVICE,

    /** Метр */
    @Schema(description = "Метр")
    METER,

    /** Литр */
    @Schema(description = "Литр")
    LITER,

    /** Погонный метр */
    @Schema(description = "Погонный метр")
    LINEAR_METER,

    /** Тонна */
    @Schema(description = "Тонна")
    TON,

    /** Час */
    @Schema(description = "Час")
    HOUR,

    /** Сутки */
    @Schema(description = "Сутки")
    DAY,

    /** Неделя */
    @Schema(description = "Неделя")
    WEEK,

    /** Месяц */
    @Schema(description = "Месяц")
    MONTH,

    /** Миллиметр */
    @Schema(description = "Миллиметр")
    MILLIMETER,

    /** Сантиметр */
    @Schema(description = "Сантиметр")
    CENTIMETER,

    /** Дециметр */
    @Schema(description = "Дециметр")
    DECIMETER,

    /** Единица */
    @Schema(description = "Единица")
    UNIT,

    /** Километр */
    @Schema(description = "Километр")
    KILOMETER,

    /** Гектограмм */
    @Schema(description = "Гектограмм")
    HECTOGRAM,

    /** Миллиграмм */
    @Schema(description = "Миллиграмм")
    MILLIGRAM,

    /** Метрический карат */
    @Schema(description = "Метрический карат")
    METRIC_CARAT,

    /** Грамм */
    @Schema(description = "Грамм")
    GRAM,

    /** Микрограмм */
    @Schema(description = "Микрограмм")
    MICROGRAM,

    /** Кубический миллиметр */
    @Schema(description = "Кубический миллиметр")
    CUBIC_MILLIMETER,

    /** Миллилитр */
    @Schema(description = "Миллилитр")
    MILLILITER,

    /** Квадратный метр */
    @Schema(description = "Квадратный метр")
    SQUARE_METER,

    /** Гектар */
    @Schema(description = "Гектар")
    HECTARE,

    /** Квадратный километр */
    @Schema(description = "Квадратный километр")
    SQUARE_KILOMETER,

    /** Лист */
    @Schema(description = "Лист")
    SHEET,

    /** Пачка */
    @Schema(description = "Пачка")
    PACK,

    /** Рулон */
    @Schema(description = "Рулон")
    ROLL,

    /** Упаковка */
    @Schema(description = "Упаковка")
    PACKAGE,

    /** Бутылка */
    @Schema(description = "Бутылка")
    BOTTLE,

    /** Работа */
    @Schema(description = "Работа")
    WORK,

    /** Кубический метр */
    @Schema(description = "Кубический метр")
    CUBIC_METER,

    /** Неизвестно */
    @Schema(description = "Неизвестно")
    UNKNOWN
}
