package io.github.texport.superkassa.core.data.impl.ofd.builder

import io.github.texport.superkassa.core.domain.impl.helper.tax.TaxCalculator
import io.github.texport.superkassa.core.domain.api.model.common.*
import io.github.texport.superkassa.core.domain.api.model.receipt.PaymentType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import kotlinx.serialization.json.*

/**
 * Строитель запросов чеков (билетов) для ОФД.
 *
 * Формирует JSON-структуру фискального чека (продажа, покупка, возврат продажи, возврат покупки)
 * в соответствии с требованиями протокола ОФД. Поддерживает расчет налогов, скидок, наценок
 * и информацию о родительском чеке для операций возврата.
 */
object OfdTicketRequestBuilder {

    /**
     * Создает полный JSON-объект фискального чека (COMMAND_TICKET) для передачи в ОФД.
     *
     * @param ofdId идентификатор ОФД.
     * @param protocolVersion версия протокола взаимодействия с ОФД.
     * @param deviceId уникальный идентификатор ККМ в ОФД.
     * @param token токен сессии/авторизации.
     * @param reqNum порядковый номер отправляемого запроса.
     * @param request запрос на регистрацию чека [ReceiptRequest] с деталями транзакции.
     * @param serviceBlock сформированный ранее служебный JSON-блок (опционально).
     * @param dateTimeMillis время оформления чека; без него — время сборки запроса.
     * @return JSON-объект [JsonObject] сформированного фискального чека.
     */
    fun buildTicketRequest(
        ofdId: String,
        protocolVersion: String,
        deviceId: Long,
        token: Long,
        reqNum: Int,
        request: ReceiptRequest,
        serviceBlock: JsonObject? = null,
        frShiftNumber: Int? = null,
        offlineTicketNumber: Int? = null,
        printedDocumentNumber: Long? = null,
        dateTimeMillis: Long? = null
    ): JsonObject {
        // Время чека — время его оформления: досланный наутро вчерашний чек
        // уходил с утренним временем.
        val now = OfdCommonRequestHelper.toDateTime(
            dateTimeMillis ?: kotlin.time.Clock.System.now().toEpochMilliseconds()
        )
        val operationCode = when (request.operation) {
            ReceiptOperationType.SELL -> "OPERATION_SELL"
            ReceiptOperationType.SELL_RETURN -> "OPERATION_SELL_RETURN"
            ReceiptOperationType.BUY -> "OPERATION_BUY"
            ReceiptOperationType.BUY_RETURN -> "OPERATION_BUY_RETURN"
        }
        // Налог позиций и скидки или наценки на чек — тем же расчётом,
        // что печатается на чеке и копится в счётчиках смены.
        val taxes = TaxCalculator().calculate(request)
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
                            // Номер смены ведёт касса: без него сервер продолжит свой счёт
                            // и документы лягут не в ту смену.
                            frShiftNumber?.let { put("frShiftNumber", JsonPrimitive(it)) }
                            // Отраслевые реквизиты: вид отрасли и ровно один подблок под него.
                            request.domain?.let { d ->
                                put(
                                    "domain",
                                    buildJsonObject {
                                        put("type", JsonPrimitive(d.type.name))
                                        d.services?.let {
                                            put(
                                                "services",
                                                buildJsonObject {
                                                    put(
                                                        "accountNumber",
                                                        JsonPrimitive(it.accountNumber)
                                                    )
                                                }
                                            )
                                        }
                                        d.gasOil?.let { g ->
                                            put(
                                                "gasoil",
                                                buildJsonObject {
                                                    g.correctionNumber?.let {
                                                        put(
                                                            "correctionNumber",
                                                            JsonPrimitive(it)
                                                        )
                                                    }
                                                    g.correctionSum?.let {
                                                        put(
                                                            "correctionSum",
                                                            OfdCommonRequestHelper.moneyObject(it.bills, it.coins)
                                                        )
                                                    }
                                                    g.cardNumber?.let { put("cardNumber", JsonPrimitive(it)) }
                                                }
                                            )
                                        }
                                        d.taxi?.let { t ->
                                            put(
                                                "taxi",
                                                buildJsonObject {
                                                    put("carNumber", JsonPrimitive(t.carNumber))
                                                    put("isOrder", JsonPrimitive(t.isOrder))
                                                    put(
                                                        "currentFee",
                                                        OfdCommonRequestHelper.moneyObject(
                                                            t.currentFee.bills,
                                                            t.currentFee.coins
                                                        )
                                                    )
                                                }
                                            )
                                        }
                                        d.parking?.let { pk ->
                                            put(
                                                "parking",
                                                buildJsonObject {
                                                    put(
                                                        "beginTime",
                                                        OfdCommonRequestHelper.toDateTime(pk.beginTimeMillis)
                                                    )
                                                    put("endTime", OfdCommonRequestHelper.toDateTime(pk.endTimeMillis))
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                            // Признак автономного чека — сам номер: поля is_offline у чека нет,
                            // и без номера сервер считает документ обычным сетевым.
                            offlineTicketNumber?.let { put("offlineTicketNumber", JsonPrimitive(it)) }
                            printedDocumentNumber?.let { put("printedDocumentNumber", JsonPrimitive(it)) }
                            put("operation", JsonPrimitive(operationCode))
                            put("dateTime", now)
                            put(
                                "operator",
                                buildJsonObject {
                                    put("code", JsonPrimitive(1))
                                }
                            )

                            // Суммы скидки/наценки (для items и amounts)
                            val discountMoney = request.discount
                            val markupMoney = request.markup

                            put(
                                "items",
                                OfdTicketItemsJson.items(request.items, taxes, commodityTypeExpected(protocolVersion))
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
                                            PaymentType.CREDIT -> "PAYMENT_CREDIT"
                                            PaymentType.TARE -> "PAYMENT_TARE"
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
                                                put(
                                                    "sum",
                                                    OfdCommonRequestHelper.moneyObject(totalBills, totalCoins)
                                                )
                                            }
                                        )
                                    }
                                }
                            )
                            // НДС на весь чек — налогом самого чека: позиции, скидка
                            // и наценка налогов тогда не несут (CPCR, TicketRequest.taxes).
                            taxes.receiptTaxes.takeIf { it.isNotEmpty() }?.let { put("taxes", OfdTaxJson.taxes(it)) }
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
                                            buildJsonObject {
                                                put("name", JsonPrimitive(OfdTicketItemsJson.DISCOUNT_NAME))
                                                put("sum", OfdCommonRequestHelper.moneyObject(m.bills, m.coins))
                                                OfdTaxJson.modifierTaxes(taxes)?.let { put("taxes", it) }
                                            }
                                        )
                                    }
                                    markupMoney?.let { m ->
                                        put(
                                            "markup",
                                            buildJsonObject {
                                                put("name", JsonPrimitive(OfdTicketItemsJson.MARKUP_NAME))
                                                put("sum", OfdCommonRequestHelper.moneyObject(m.bills, m.coins))
                                                OfdTaxJson.modifierTaxes(taxes)?.let { put("taxes", it) }
                                            }
                                        )
                                    }
                                }
                            )

                            // БИН/ИИН покупателя: касса его спрашивает, печатает
                            // на чеке и до сих пор не отправляла — фискальный
                            // документ уходил без покупателя, хотя на бумаге он
                            // стоял. Реквизит живёт в расширениях чека с версии
                            // 2.0.1, то есть во всех обслуживаемых версиях.
                            request.customerBin?.takeIf { it.isNotBlank() }?.let { bin ->
                                put(
                                    "extensionOptions",
                                    buildJsonObject { put("customerIinOrBin", JsonPrimitive(bin)) }
                                )
                            }

                            val parent = request.parentTicket
                            if (parent != null &&
                                (
                                    request.operation == ReceiptOperationType.SELL_RETURN ||
                                        request.operation == ReceiptOperationType.BUY_RETURN
                                    )
                            ) {
                                put(
                                    "parentTicket",
                                    buildJsonObject {
                                        put(
                                            "parentTicketNumber",
                                            JsonPrimitive(parent.parentTicketNumber.toString())
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

    /**
     * Обязателен ли тип предмета потребления у позиции с НТИН.
     *
     * Реквизит введён версией 2.0.4 и с неё обязателен для товаров
     * из национального каталога: ОФД на позицию с НТИН без него
     * отвечает кодом 15, а по нему касса уходит в блокировку. В более
     * ранних версиях поля в протоколе нет, и посылать его нельзя —
     * там ОФД отвергнет сам реквизит.
     */
    private fun commodityTypeExpected(protocolVersion: String): Boolean =
        (protocolVersion.filter { it.isDigit() }.toIntOrNull() ?: 0) >= COMMODITY_TYPE_SINCE

    /** Версия протокола, с которой тип предмета потребления обязателен. */
    private const val COMMODITY_TYPE_SINCE = 204
}
