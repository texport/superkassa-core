package io.github.texport.superkassa.core.domain.impl.helper.zxreport

import io.github.texport.superkassa.core.domain.api.model.zxreport.TaxAggregate
import io.github.texport.superkassa.core.domain.api.model.zxreport.TaxOperationAggregate
import io.github.texport.superkassa.core.domain.api.model.common.CounterKeyFormats
import io.github.texport.superkassa.core.domain.api.model.common.format
import io.github.texport.superkassa.core.domain.api.model.common.VatGroup

/**
 * Объект-построитель блока налогов (taxes) для Zx-отчета.
 *
 * Агрегирует данные по налоговым ставкам и оборотам с разделением по типам операций
 * на основе счетчиков группы TAX_*:
 * - [CounterKeyFormats.TAX_TURNOVER]
 * - [CounterKeyFormats.TAX_TURNOVER_NO_TAX]
 * - [CounterKeyFormats.TAX_SUM]
 */
object ZxTaxBlockBuilder {
    private const val TAX_TYPE_ENUM_CODE = 100

    /**
     * Список основных кассовых операций для построения налоговых отчетов.
     */
    private val operations = listOf(
        "OPERATION_SELL",
        "OPERATION_SELL_RETURN",
        "OPERATION_BUY",
        "OPERATION_BUY_RETURN"
    )

    /**
     * Строит список налоговых агрегатов по всем группам НДС.
     *
     * Для каждой группы [VatGroup] (кроме NO_VAT) рассчитываются следующие показатели:
     * - type — числовой код типа налога;
     * - percent — налоговая ставка в тысячных долях процента;
     * - operations — список оборотов и сумм налога по каждому виду операций.
     *
     * @param counters Карта счетчиков смены.
     * @return Список объектов [TaxAggregate] для каждой налоговой группы.
     */
    fun resolveTaxes(counters: Map<String, Long>): List<TaxAggregate> {
        val result = mutableListOf<TaxAggregate>()

        for (group in VatGroup.entries) {
            // NO_VAT не считается налогом и не должен попадать в блок taxes ZXReport.
            if (group == VatGroup.NO_VAT) continue

            val type = TAX_TYPE_ENUM_CODE
            val typeCode = taxTypeCodeForGroup(group)
            val percent = percentForGroup(group)

            val ops = operations.map { op ->
                val turnover = counters[CounterKeyFormats.TAX_TURNOVER.format(group.name, op)] ?: 0L
                val sum = counters[CounterKeyFormats.TAX_SUM.format(group.name, op)] ?: 0L
                val turnoverNoTax =
                    counters[CounterKeyFormats.TAX_TURNOVER_NO_TAX.format(group.name, op)] ?: 0L

                TaxOperationAggregate(
                    operation = op,
                    turnoverBills = turnover,
                    turnoverWithoutTaxBills = turnoverNoTax,
                    taxSumBills = sum
                )
            }

            result += TaxAggregate(
                taxType = type,
                taxTypeCode = typeCode,
                percent = percent,
                operations = ops
            )
        }

        return result
    }

    /**
     * Возвращает строковый код типа налога для указанной группы НДС.
     */
    private fun taxTypeCodeForGroup(group: VatGroup): String = group.taxTypeCode

    /**
     * Возвращает процентную ставку НДС в тысячных долях процента.
     */
    private fun percentForGroup(group: VatGroup): Int = group.percentThousandths
}
