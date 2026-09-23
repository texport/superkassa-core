package io.github.texport.superkassa.testing.api.kassa

import io.github.texport.superkassa.core.presentation.api.model.kkm.VatGroup

/**
 * Какой заводится касса.
 *
 * Пинов по умолчанию нет: администратор и кассир входят теми пинами,
 * которые задала проверка.
 *
 * @property adminPin пин администратора, 4–10 символов.
 * @property cashierPin пин кассира, 4–10 символов, отличный от пина администратора.
 * @property vat налоговый режим кассы.
 * @property name название кассы в списке входа; `null` — без названия.
 * @property cashierName имя кассира.
 * @property systemId номер кассы в БФД; `null` — следующий свободный на стенде.
 */
data class KassaSetup(
    val adminPin: String,
    val cashierPin: String,
    val vat: VatMode = VatMode.NotPayer,
    val name: String? = null,
    val cashierName: String = "Нурлан",
    val systemId: Long? = null
)

/** Налоговый режим кассы. */
sealed interface VatMode {
    /** Ставка, которой облагается позиция без своей ставки. */
    val defaultVat: VatGroup

    /** Не плательщик НДС. */
    data object NotPayer : VatMode {
        override val defaultVat: VatGroup = VatGroup.NO_VAT
    }

    /** Плательщик НДС: позиция без своей ставки облагается ставкой [defaultVat]. */
    data class Payer(override val defaultVat: VatGroup) : VatMode

    /** Смешанный режим: налог как у плательщика, ставка печатается у каждой позиции. */
    data class Mixed(override val defaultVat: VatGroup) : VatMode
}
