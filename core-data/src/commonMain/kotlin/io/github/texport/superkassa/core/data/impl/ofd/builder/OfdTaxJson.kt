package io.github.texport.superkassa.core.data.impl.ofd.builder

import io.github.texport.superkassa.core.domain.api.model.receipt.TaxLine
import io.github.texport.superkassa.core.domain.api.model.receipt.TicketTaxResult
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

/**
 * Налог в запросе чека (`TicketRequest.Tax`): вид налога, ставка
 * в тысячных процента, сумма и признак «в том числе».
 */
internal object OfdTaxJson {

    /** Налоги позиции, скидки или наценки. */
    fun taxes(lines: List<TaxLine>): JsonArray = buildJsonArray { lines.forEach { add(tax(it)) } }

    /**
     * Налог скидки или наценки на чек по ставкам либо `null`, если его нет.
     *
     * По нему БФД уменьшает налог чека на скидку и увеличивает на наценку
     * (`OperationCalculator.mergeTaxReportIntoReport`): без него скидка
     * снижала оборот у БФД, а налог оставался прежним.
     */
    fun modifierTaxes(
        result: TicketTaxResult
    ): JsonArray? = result.modifierTaxes.takeIf { it.isNotEmpty() }?.let(::taxes)

    private fun tax(line: TaxLine): JsonObject = buildJsonObject {
        put("taxType", JsonPrimitive(OfdCommonRequestHelper.taxTypeForGroup(line.vatGroup)))
        put("percent", JsonPrimitive(line.vatGroup.percentThousandths))
        put("sum", OfdCommonRequestHelper.moneyObject(line.taxSum.bills, line.taxSum.coins))
        put("isInTotalSum", JsonPrimitive(true))
    }
}
