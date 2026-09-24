package io.github.texport.superkassa.core.data.impl.ofd.builder

import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.common.UnitOfMeasurement
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptItem
import io.github.texport.superkassa.core.domain.api.model.receipt.TaxLine
import io.github.texport.superkassa.core.domain.api.model.receipt.TicketTaxResult
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Позиции чека в запросе к БФД (`TicketRequest.Item`).
 *
 * Позиция уходит суммой строки до своей скидки и наценки, а скидка
 * и наценка — элементами `ITEM_TYPE_DISCOUNT` и `ITEM_TYPE_MARKUP` сразу
 * за ней (у сторно — сторно скидки и наценки). Так их требует CPCR и так
 * БФД строит X/Z: отделы и операции без скидок, скидки и наценки
 * отдельными строками (`OperationCalculator.updateDiscounts`). Прежде
 * скидка вычиталась из суммы позиции: цена 1000, сумма 900, скидки нет,
 * и у БФД она пропадала из отчёта.
 */
internal object OfdTicketItemsJson {

    /**
     * Элементы чека по порядку позиций.
     *
     * @param taxes налог позиций и их скидок и наценок.
     * @param withCommodityType ставить ли тип предмета потребления у позиции с НТИН.
     */
    fun items(items: List<ReceiptItem>, taxes: TicketTaxResult, withCommodityType: Boolean): JsonArray =
        buildJsonArray {
            items.forEachIndexed { index, item ->
                add(commodity(item, taxes.itemTaxes.getOrNull(index), withCommodityType))
                val modifierTax = taxes.itemModifierTaxes.getOrNull(index)
                item.discount?.let { add(modifier(item, it, DISCOUNT, modifierTax)) }
                item.markup?.let { add(modifier(item, it, MARKUP, modifierTax)) }
            }
        }

    private fun commodity(item: ReceiptItem, tax: TaxLine?, withCommodityType: Boolean): JsonObject {
        val (type, field) = if (item.isStorno) STORNO_COMMODITY else COMMODITY
        return buildJsonObject {
            put("type", type)
            put(field, goods(item, tax, withCommodityType))
        }
    }

    private fun goods(item: ReceiptItem, tax: TaxLine?, withCommodityType: Boolean): JsonObject = buildJsonObject {
        put("name", item.name)
        put("sectionCode", item.sectionCode)
        put("quantity", item.quantity)
        put("price", money(item.price))
        put("sum", money(item.sumBeforeModifiers))
        put("measureUnitCode", item.measureUnitCode ?: UnitOfMeasurement.DEFAULT.code)
        item.barcode?.takeIf { it.isNotBlank() }?.let { put("barcode", it) }
        item.listExciseStamp?.takeIf { it.isNotEmpty() }?.let { stamps ->
            put("listExciseStamp", buildJsonArray { stamps.forEach { add(JsonPrimitive(it)) } })
        }
        item.ntin?.takeIf { it.isNotBlank() }?.let { ntin ->
            put("ntin", ntin)
            if (withCommodityType) put("commodityType", PRODUCT)
        }
        // Налог позиции; у сторно — свой, иначе БФД не вычтет его из налога чека.
        tax?.let { put("taxes", OfdTaxJson.taxes(listOf(it))) }
    }

    private fun modifier(item: ReceiptItem, sum: Money, kind: Kind, tax: TaxLine?): JsonObject = buildJsonObject {
        val (type, field) = if (item.isStorno) kind.storno else kind.plain
        put("type", type)
        put(
            field,
            buildJsonObject {
                put("name", kind.title)
                put("sum", money(sum))
                tax?.let { put("taxes", OfdTaxJson.taxes(listOf(it))) }
            }
        )
    }

    private fun money(value: Money): JsonObject = OfdCommonRequestHelper.moneyObject(value.bills, value.coins)

    /** Имя скидки — у позиции и на чек. */
    const val DISCOUNT_NAME: String = "Скидка"

    /** Имя наценки — у позиции и на чек. */
    const val MARKUP_NAME: String = "Наценка"

    /** Вид модификатора позиции: тип и поле элемента, у сторно — свои. */
    private class Kind(val title: String, val plain: Pair<String, String>, val storno: Pair<String, String>)

    private val COMMODITY = "ITEM_TYPE_COMMODITY" to "commodity"
    private val STORNO_COMMODITY = "ITEM_TYPE_STORNO_COMMODITY" to "stornoCommodity"
    private val DISCOUNT = Kind(
        DISCOUNT_NAME,
        "ITEM_TYPE_DISCOUNT" to "discount",
        "ITEM_TYPE_STORNO_DISCOUNT" to "stornoDiscount"
    )
    private val MARKUP = Kind(
        MARKUP_NAME,
        "ITEM_TYPE_MARKUP" to "markup",
        "ITEM_TYPE_STORNO_MARKUP" to "stornoMarkup"
    )

    /**
     * Тип предмета потребления у позиции с НТИН.
     *
     * НТИН выдаёт национальный каталог товаров: работ и услуг в нём нет,
     * поэтому позиция, пришедшая с НТИН, — всегда товар, и спрашивать
     * тип у кассира незачем.
     */
    private const val PRODUCT = "COMMODITY_TYPE_PRODUCT"
}
