package kz.mybrain.superkassa.core.data.ofd

import kz.mybrain.superkassa.core.domain.model.*
import kz.mybrain.superkassa.core.domain.tax.TaxCalculationService
import kotlinx.serialization.json.*

object OfdTicketRequestBuilder {

    fun buildTicketRequest(
        ofdId: String,
        protocolVersion: String,
        deviceId: Long,
        token: Long,
        reqNum: Int,
        request: ReceiptRequest,
        serviceBlock: JsonObject? = null
    ): JsonObject {
        val now = OfdCommonRequestHelper.toDateTime(System.currentTimeMillis())
        val operationCode = when (request.operation) {
            ReceiptOperationType.SELL -> "OPERATION_SELL"
            ReceiptOperationType.SELL_RETURN -> "OPERATION_SELL_RETURN"
            ReceiptOperationType.BUY -> "OPERATION_BUY"
            ReceiptOperationType.BUY_RETURN -> "OPERATION_BUY_RETURN"
        }
        return buildJsonObject {
            put("ofdId", JsonPrimitive(ofdId))
            put("protocolVersion", JsonPrimitive(protocolVersion))
            put("messageType", JsonPrimitive("REQUEST"))
            put("commandType", JsonPrimitive("COMMAND_TICKET"))
            put(
                "header",
                buildJsonObject {
                    put("deviceId", JsonPrimitive(deviceId))
                    put("token", JsonPrimitive(token))
                    put("reqNum", JsonPrimitive(reqNum))
                }
            )
            put(
                "payload",
                buildJsonObject {
                    serviceBlock?.let { put("service", it) }
                    put(
                        "ticket",
                        buildJsonObject {
                            put("operation", JsonPrimitive(operationCode))
                            put("dateTime", now)
                            put(
                                "operator",
                                buildJsonObject {
                                    put("code", JsonPrimitive(1))
                                }
                            )

                            // Суммы скидки/наценки (для items и amounts)
                            val totalItemDiscount =
                                OfdCommonRequestHelper.sumMoney(request.items.mapNotNull { it.discount })
                            val totalItemMarkup =
                                OfdCommonRequestHelper.sumMoney(request.items.mapNotNull { it.markup })
                            val discountMoney = request.discount ?: totalItemDiscount
                            val markupMoney = request.markup ?: totalItemMarkup

                            put(
                                "items",
                                buildJsonArray {
                                    val taxService = TaxCalculationService()

                                    request.items.forEach { item ->
                                        val (itemType, itemField) = if (item.isStorno) {
                                            "ITEM_TYPE_STORNO_COMMODITY" to "stornoCommodity"
                                        } else {
                                            "ITEM_TYPE_COMMODITY" to "commodity"
                                        }
                                        add(
                                            buildJsonObject {
                                                put("type", JsonPrimitive(itemType))
                                                put(
                                                    itemField,
                                                    buildJsonObject {
                                                        put("name", JsonPrimitive(item.name))
                                                        put("sectionCode", JsonPrimitive(item.sectionCode))
                                                        put("quantity", JsonPrimitive(item.quantity * 1000L))
                                                        put(
                                                            "price",
                                                            OfdCommonRequestHelper.moneyObject(item.price.bills, item.price.coins)
                                                        )
                                                        put(
                                                            "sum",
                                                            OfdCommonRequestHelper.moneyObject(item.sum.bills, item.sum.coins)
                                                        )
                                                        put(
                                                            "measureUnitCode",
                                                            JsonPrimitive(item.measureUnitCode ?: UnitOfMeasurement.DEFAULT.code)
                                                        )
                                                        item.barcode?.takeIf { it.isNotBlank() }?.let { barcode ->
                                                            put("barcode", JsonPrimitive(barcode))
                                                        }
                                                        item.listExciseStamp?.takeIf { it.isNotEmpty() }?.let { stamps ->
                                                            put(
                                                                "listExciseStamp",
                                                                buildJsonArray { stamps.forEach { add(JsonPrimitive(it)) } }
                                                            )
                                                        }
                                                        item.ntin?.takeIf { it.isNotBlank() }?.let { ntin ->
                                                            put("ntin", JsonPrimitive(ntin))
                                                        }

                                                        // Налог на уровне позиции (commodity.taxes[])
                                                        val itemVatGroup =
                                                            when (request.taxRegime) {
                                                                TaxRegime.NO_VAT ->
                                                                    VatGroup.NO_VAT

                                                                TaxRegime.VAT_PAYER,
                                                                TaxRegime.MIXED ->
                                                                    item.vatGroup ?: (request.defaultVatGroup ?: VatGroup.NO_VAT)
                                                            }

                                                        val taxResultForItem =
                                                            taxService.calculateTicketTaxes(
                                                                items = listOf(item),
                                                                taxRegime = request.taxRegime,
                                                                defaultVatGroup = request.defaultVatGroup ?: VatGroup.NO_VAT,
                                                                overrideVatGroup = itemVatGroup
                                                            )

                                                        if (taxResultForItem.ticketTaxes.isNotEmpty()) {
                                                            put(
                                                                "taxes",
                                                                buildJsonArray {
                                                                    taxResultForItem.ticketTaxes.forEach { line ->
                                                                        add(
                                                                            buildJsonObject {
                                                                                put(
                                                                                    "taxType",
                                                                                    JsonPrimitive(
                                                                                        OfdCommonRequestHelper.taxTypeForGroup(
                                                                                            line.vatGroup
                                                                                        )
                                                                                    )
                                                                                )
                                                                                put(
                                                                                    "percent",
                                                                                    JsonPrimitive(line.percent)
                                                                                )
                                                                                put(
                                                                                    "sum",
                                                                                    OfdCommonRequestHelper.moneyObject(
                                                                                        line.taxSum.bills,
                                                                                        line.taxSum.coins
                                                                                    )
                                                                                )
                                                                                put(
                                                                                    "isInTotalSum",
                                                                                    JsonPrimitive(true)
                                                                                )
                                                                            }
                                                                        )
                                                                    }
                                                                }
                                                            )
                                                        }
                                                    }
                                                )
                                            }
                                        )
                                    }

                                    // Отдельные позиции скидки/наценки по протоколу
                                    when (request.operation) {
                                        ReceiptOperationType.SELL_RETURN,
                                        ReceiptOperationType.BUY_RETURN -> {
                                            discountMoney?.takeIf { m -> m.bills != 0L || m.coins != 0 }?.let { m ->
                                                add(
                                                    buildJsonObject {
                                                        put("type", JsonPrimitive("ITEM_TYPE_STORNO_DISCOUNT"))
                                                        put(
                                                            "stornoDiscount",
                                                            buildJsonObject {
                                                                put("name", JsonPrimitive("Скидка"))
                                                                put("sum", OfdCommonRequestHelper.moneyObject(m.bills, m.coins))
                                                            }
                                                        )
                                                    }
                                                )
                                            }
                                            markupMoney?.takeIf { m -> m.bills != 0L || m.coins != 0 }?.let { m ->
                                                add(
                                                    buildJsonObject {
                                                        put("type", JsonPrimitive("ITEM_TYPE_STORNO_MARKUP"))
                                                        put(
                                                            "stornoMarkup",
                                                            buildJsonObject {
                                                                put("name", JsonPrimitive("Наценка"))
                                                                put("sum", OfdCommonRequestHelper.moneyObject(m.bills, m.coins))
                                                            }
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                        ReceiptOperationType.SELL,
                                        ReceiptOperationType.BUY -> {
                                            discountMoney?.takeIf { m -> m.bills != 0L || m.coins != 0 }?.let { m ->
                                                add(
                                                    buildJsonObject {
                                                        put("type", JsonPrimitive("ITEM_TYPE_DISCOUNT"))
                                                        put(
                                                            "discount",
                                                            buildJsonObject {
                                                                put("name", JsonPrimitive("Скидка"))
                                                                put("sum", OfdCommonRequestHelper.moneyObject(m.bills, m.coins))
                                                            }
                                                        )
                                                    }
                                                )
                                            }
                                            markupMoney?.takeIf { m -> m.bills != 0L || m.coins != 0 }?.let { m ->
                                                add(
                                                    buildJsonObject {
                                                        put("type", JsonPrimitive("ITEM_TYPE_MARKUP"))
                                                        put(
                                                            "markup",
                                                            buildJsonObject {
                                                                put("name", JsonPrimitive("Наценка"))
                                                                put("sum", OfdCommonRequestHelper.moneyObject(m.bills, m.coins))
                                                            }
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                            put(
                                "payments",
                                buildJsonArray {
                                    val groupedPayments = request.payments.groupBy {
                                        when (it.type) {
                                            PaymentType.CASH -> "PAYMENT_CASH"
                                            PaymentType.CARD -> "PAYMENT_CARD"
                                            PaymentType.ELECTRONIC -> "PAYMENT_CARD" // Map ELECTRONIC to CARD for OFD
                                            PaymentType.MOBILE -> "PAYMENT_MOBILE"
                                        }
                                    }
                                    groupedPayments.forEach { (payType, paymentList) ->
                                        var totalBills = paymentList.sumOf { it.sum.bills }
                                        var totalCoins = paymentList.sumOf { it.sum.coins }
                                        if (totalCoins >= 100) {
                                            totalBills += totalCoins / 100
                                            totalCoins %= 100
                                        }
                                        add(
                                            buildJsonObject {
                                                put("type", JsonPrimitive(payType))
                                                put("sum", OfdCommonRequestHelper.moneyObject(totalBills, totalCoins.toInt()))
                                            }
                                        )
                                    }
                                }
                            )
                            put(
                                "amounts",
                                buildJsonObject {
                                    put(
                                        "total",
                                        OfdCommonRequestHelper.moneyObject(
                                            request.total.bills,
                                            request.total.coins
                                        )
                                    )
                                    val taken = request.taken ?: request.total
                                    put(
                                        "taken",
                                        OfdCommonRequestHelper.moneyObject(
                                            taken.bills,
                                            taken.coins
                                        )
                                    )
                                    val change = request.change ?: Money(0, 0)
                                    put(
                                        "change",
                                        OfdCommonRequestHelper.moneyObject(
                                            change.bills,
                                            change.coins
                                        )
                                    )

                                    discountMoney?.let { m ->
                                        put(
                                            "discount",
                                            OfdCommonRequestHelper.moneyObject(m.bills, m.coins)
                                        )
                                    }
                                    markupMoney?.let { m ->
                                        put(
                                            "markup",
                                            OfdCommonRequestHelper.moneyObject(m.bills, m.coins)
                                        )
                                    }
                                }
                            )

                            val parent = request.parentTicket
                            if (parent != null &&
                                (request.operation == ReceiptOperationType.SELL_RETURN ||
                                 request.operation == ReceiptOperationType.BUY_RETURN)
                            ) {
                                put(
                                    "parentTicket",
                                    buildJsonObject {
                                        put(
                                            "parentTicketNumber",
                                            JsonPrimitive(parent.parentTicketNumber)
                                        )
                                        put(
                                            "parentTicketDateTime",
                                            OfdCommonRequestHelper.toDateTime(parent.parentTicketDateTimeMillis)
                                        )
                                        put("kgdKkmId", JsonPrimitive(parent.kgdKkmId))
                                        put(
                                            "parentTicketTotal",
                                            OfdCommonRequestHelper.moneyObject(
                                                parent.parentTicketTotal.bills,
                                                parent.parentTicketTotal.coins
                                            )
                                        )
                                        put(
                                            "parentTicketIsOffline",
                                            JsonPrimitive(parent.parentTicketIsOffline)
                                        )
                                    }
                                )
                            }
                        }
                    )
                }
            )
        }
    }
}
