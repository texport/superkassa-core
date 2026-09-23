package io.github.texport.superkassa.core.domain.impl.usecase.receipt

import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.model.common.TaxRegime
import io.github.texport.superkassa.core.domain.api.model.common.VatGroup
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptItem
import io.github.texport.superkassa.core.string.api.CoreStrings

/**
 * НДС чека: позиции и ставка на весь чек.
 *
 * @property items позиции; в чеке с НДС по позициям у позиции своя ставка
 * либо никакой — тогда её облагает ставка кассы.
 * @property receiptVat ставка на весь чек либо `null`, если НДС по позициям.
 */
internal data class VatScope(val items: List<ReceiptItem>, val receiptVat: VatGroup?)

/**
 * НДС в чеке задаётся одним способом: на весь чек или по позициям.
 *
 * Правило то же, что у скидки: уровень один на чек. CPCR принимает налоги
 * либо у позиций, либо у самого чека (отказ «Taxes mix error»), и касса
 * не выбирает за кассира, какой из двух способов он имел в виду.
 *
 * @param receiptVat ставка на весь чек, присланная вызывающим, либо `null`.
 * @param items позиции чека.
 * @throws ValidationException если заданы оба способа.
 */
internal fun requireOneVatScope(receiptVat: VatGroup?, items: List<ReceiptItem>) {
    if (receiptVat == null || items.none { it.vatGroup != null }) return
    throw ValidationException(CoreStrings.receiptVatScopesConflict(), "RECEIPT_VAT_SCOPES_CONFLICT")
}

/**
 * Проверяет, что ставки чека допускает налоговый режим кассы.
 *
 * Неплательщик НДС налог не выделяет: при режиме NO_VAT расчёт налога
 * даёт пустой список, и ставка позиции никуда не уходит. Принять такую
 * ставку — значит показать кассиру и покупателю налог, которого в чеке
 * ОФД нет. Отказ громкий и называет ставку, чтобы кассир понял, что
 * исправлять.
 *
 * @param kkm касса, от имени которой оформляется чек.
 * @param items позиции чека.
 * @param receiptVat ставка на весь чек, присланная вызывающим, либо `null`.
 * @throws ValidationException если режим кассы ставку не допускает.
 */
internal fun requireVatAllowedByRegime(kkm: KkmInfo, items: List<ReceiptItem>, receiptVat: VatGroup?) {
    if (kkm.taxRegime != TaxRegime.NO_VAT) return
    val offending = items.firstNotNullOfOrNull { it.vatGroup?.takeIf { group -> group != VatGroup.NO_VAT } }
        ?: receiptVat?.takeIf { it != VatGroup.NO_VAT }
        ?: return
    throw ValidationException(CoreStrings.receiptVatNotAllowed(offending.name), "RECEIPT_VAT_NOT_ALLOWED")
}
