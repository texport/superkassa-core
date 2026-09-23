package io.github.texport.superkassa.core.string.impl

import io.github.texport.superkassa.core.string.api.TrilingualMessage

/** Отказы по границам сумм, количеств и процентов в чеке и кассовой операции. */
internal object AmountStrings {
    fun valueOutOfRange(field: String, bounds: String): TrilingualMessage = TrilingualMessage(
        ru = "Значение «$field» вне допустимых границ: $bounds.",
        kk = "«$field» мәні рұқсат етілген шектен тыс: $bounds.",
        en = "Value of '$field' is out of range: $bounds."
    )

    fun cashSumTooSmall(): TrilingualMessage = TrilingualMessage(
        ru = "Сумма операции должна быть не меньше 0,01 ₸.",
        kk = "Операция сомасы кемінде 0,01 ₸ болуы керек.",
        en = "Operation amount must be at least 0.01 ₸."
    )
}
