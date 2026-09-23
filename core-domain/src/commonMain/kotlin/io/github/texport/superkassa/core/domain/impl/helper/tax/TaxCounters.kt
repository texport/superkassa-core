package io.github.texport.superkassa.core.domain.impl.helper.tax

import io.github.texport.superkassa.core.domain.api.model.common.CounterKeyFormats
import io.github.texport.superkassa.core.domain.api.model.common.format
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest

/**
 * Приращения налоговых счётчиков смены от одного чека.
 *
 * Оборот по налогу — сумма с НДС, как у БФД в Z (`TaxOperation.turnover`),
 * а оборот без налога — он же за вычетом НДС. Прежде в оборот шла сумма
 * без налога, и Z в БФД занижал облагаемый оборот на сумму НДС.
 *
 * Правило одно для счёта по мере чеков и для пересчёта смены заново.
 *
 * @param request сохранённый чек.
 * @param operationKey код операции, например `OPERATION_SELL`.
 * @return пары «ключ счётчика — приращение в тиынах».
 */
internal fun taxCounterDeltas(request: ReceiptRequest, operationKey: String): List<Pair<String, Long>> =
    TaxCalculator().calculate(request).ticketTaxes.flatMap { line ->
        val group = line.vatGroup.name
        val tax = line.taxSum.tiyn()
        val withoutTax = line.taxBase.tiyn()
        listOf(
            CounterKeyFormats.TAX_TURNOVER.format(group, operationKey) to withoutTax + tax,
            CounterKeyFormats.TAX_SUM.format(group, operationKey) to tax,
            CounterKeyFormats.TAX_TURNOVER_NO_TAX.format(group, operationKey) to withoutTax
        )
    }
