package io.github.texport.superkassa.core.domain.impl.usecase.receipt

import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.common.UnitOfMeasurement
import io.github.texport.superkassa.core.domain.api.model.common.VatGroup
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptItem
import io.github.texport.superkassa.core.string.api.CoreStrings

/**
 * Позиция чека из того, что прислал кассир.
 *
 * Сумма строки — [Money.lineSum], то есть цена × количество к ближайшему
 * тиыну; скидка и наценка позиции берутся от неё и вычитаются или
 * прибавляются уже в тиынах.
 *
 * @throws ValidationException если код единицы измерения неизвестен.
 * @throws IllegalArgumentException если ставка НДС неизвестна.
 */
internal fun receiptItemOf(dto: CreateReceiptCommand.ItemInput): ReceiptItem {
    val baseTiyn = Money.lineSum(dto.price, dto.quantity).tiyn()
    val discountTiyn = partOf(baseTiyn, dto.discountPercent, dto.discountSum)
    val markupTiyn = partOf(baseTiyn, dto.markupPercent, dto.markupSum)
    return ReceiptItem(
        name = dto.name,
        nameKk = dto.nameKk?.takeIf { it.isNotBlank() },
        sectionCode = SECTION_CODE,
        quantity = dto.quantity.scaled(QUANTITY_SCALE),
        price = Money.fromTenge(dto.price),
        sum = Money.fromTiyn((baseTiyn - discountTiyn + markupTiyn).coerceAtLeast(0L)),
        barcode = dto.barcode?.takeIf { it.isNotBlank() },
        vatGroup = dto.vatGroup?.let { vatGroupOf(it, "vatGroup") },
        discount = discountTiyn.takeIf { it > 0 }?.let { Money.fromTiyn(it) },
        markup = markupTiyn.takeIf { it > 0 }?.let { Money.fromTiyn(it) },
        measureUnitCode = dto.measureUnitCode?.takeIf { it.isNotBlank() }?.let { measureUnitOf(it) },
        listExciseStamp = dto.listExciseStamp?.takeIf { it.isNotEmpty() },
        ntin = dto.ntin?.takeIf { it.isNotBlank() },
        isStorno = dto.isStorno
    )
}

/**
 * Ставка НДС по имени.
 *
 * @param field имя поля для текста отказа.
 * @throws IllegalArgumentException если такой ставки нет.
 */
internal fun vatGroupOf(value: String, field: String): VatGroup = try {
    VatGroup.valueOf(value)
} catch (_: IllegalArgumentException) {
    throw IllegalArgumentException("Invalid $field: $value. Valid: " + VatGroup.entries.joinToString { it.name })
}

private fun measureUnitOf(raw: String): String = try {
    UnitOfMeasurement.fromCode(raw).code
} catch (_: IllegalArgumentException) {
    throw ValidationException(CoreStrings.measureUnitCodeInvalid(raw), "MEASURE_UNIT_CODE_INVALID")
}

/**
 * Скидка или наценка в тиынах: процентом от базы либо готовой суммой.
 *
 * Процент считается от целых тиынов и округляется один раз к ближайшему.
 * Правило одно и для позиции, и для чека, поэтому и место одно.
 *
 * @param baseTiyn база, от которой берётся процент.
 * @param percent доля в процентах либо `null`.
 * @param sum готовая сумма в тенге либо `null`.
 * @return сумма скидки или наценки в тиынах; ноль, если не задана ни одна.
 */
internal fun partOf(baseTiyn: Long, percent: Decimal?, sum: Decimal?): Long = when {
    percent != null -> Decimal.roundedDiv(baseTiyn * percent.unscaled, HUNDRED_PERCENT * pow10(percent.scale))
    sum != null -> sum.scaled(TIYN_SCALE)
    else -> 0L
}

private fun pow10(power: Int): Long {
    var result = 1L
    repeat(power) { result *= 10 }
    return result
}

/** Секция позиции: касса работает одной секцией. */
private const val SECTION_CODE = "001"

/** Знаков после запятой у тенге. */
private const val TIYN_SCALE: Int = 2

/** Количество хранится в тысячных долях единицы. */
private const val QUANTITY_SCALE: Int = 3

/** Сто процентов. */
private const val HUNDRED_PERCENT: Long = 100
