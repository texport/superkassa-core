package kz.mybrain.superkassa.core.data.ofd.builder

import java.util.zip.CRC32
import kotlinx.serialization.json.*
import kz.mybrain.superkassa.core.domain.model.zxreport.*

/**
 * Строитель запросов отчетов (X/Z-отчеты) и закрытия смены для ОФД.
 *
 * Предоставляет функции для создания JSON-структуры сменных отчетов и запроса на закрытие смены,
 * а также для расчета контрольной суммы CRC32 сформированного отчета.
 */
object OfdReportRequestBuilder {

    /**
     * Формирует полный JSON-запрос отчета (COMMAND_REPORT) для отправки в ОФД.
     *
     * @param ofdId идентификатор ОФД.
     * @param protocolVersion версия протокола взаимодействия с ОФД.
     * @param deviceId уникальный идентификатор устройства ККМ.
     * @param token токен сессии/авторизации.
     * @param reqNum порядковый номер отправляемого запроса.
     * @param reportType тип отчета (например, "REPORT_Z" или "REPORT_X").
     * @param zxReport входные сменные данные [ZxReportInput] для построения тела отчета.
     * @param serviceBlock сформированный ранее служебный JSON-блок (getRegInfo, offlinePeriod и т.д.).
     * @return Полный JSON-объект [JsonObject] запроса отчета.
     */
    fun buildReportRequest(
        ofdId: String,
        protocolVersion: String,
        deviceId: Long,
        token: Long,
        reqNum: Int,
        reportType: String,
        zxReport: ZxReportInput,
        serviceBlock: JsonObject
    ): JsonObject {
        val ofdIdNorm = ofdId.lowercase()
        return buildJsonObject {
            put("ofdId", JsonPrimitive(ofdIdNorm))
            put("protocolVersion", JsonPrimitive(protocolVersion))
            put("messageType", JsonPrimitive("REQUEST"))
            put("commandType", JsonPrimitive("COMMAND_REPORT"))
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
                    put("service", serviceBlock)
                    put(
                        "report",
                        buildJsonObject {
                            put("reportType", JsonPrimitive(reportType))
                            put("dateTime", OfdCommonRequestHelper.toDateTime(zxReport.dateTimeMillis))
                            put("isOffline", JsonPrimitive(false))
                            put(
                                "zxReport",
                                buildZxReportInternal(zxReport)
                            )
                        }
                    )
                }
            )
        }
    }

    /**
     * Формирует полный JSON-запрос закрытия смены (COMMAND_CLOSE_SHIFT) для отправки в ОФД.
     *
     * @param ofdId идентификатор ОФД.
     * @param protocolVersion версия протокола взаимодействия с ОФД.
     * @param deviceId уникальный идентификатор устройства ККМ.
     * @param token токен сессии/авторизации.
     * @param reqNum порядковый номер отправляемого запроса.
     * @param closeTimeMillis время закрытия смены в миллисекундах epoch.
     * @param frShiftNumber фискальный номер закрываемой смены.
     * @param zxReport JSON-объект отчета, подготовленный методом [buildZxReportInternal].
     * @param serviceBlock сформированный ранее служебный JSON-блок.
     * @return Полный JSON-объект [JsonObject] запроса закрытия смены.
     */
    fun buildCloseShiftRequest(
        ofdId: String,
        protocolVersion: String,
        deviceId: Long,
        token: Long,
        reqNum: Int,
        closeTimeMillis: Long,
        frShiftNumber: Int,
        zxReport: JsonObject,
        serviceBlock: JsonObject
    ): JsonObject {
        val ofdIdNorm = ofdId.lowercase()
        return buildJsonObject {
            put("ofdId", JsonPrimitive(ofdIdNorm))
            put("protocolVersion", JsonPrimitive(protocolVersion))
            put("messageType", JsonPrimitive("REQUEST"))
            put("commandType", JsonPrimitive("COMMAND_CLOSE_SHIFT"))
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
                    put("service", serviceBlock)
                    put(
                        "closeShift",
                        buildJsonObject {
                            put("closeTime", OfdCommonRequestHelper.toDateTime(closeTimeMillis))
                            put("isOffline", JsonPrimitive(false))
                            put("frShiftNumber", JsonPrimitive(frShiftNumber))
                            put("withdrawMoney", JsonPrimitive(false))
                            put(
                                "operator",
                                buildJsonObject {
                                    put("code", JsonPrimitive(1))
                                    put("name", JsonPrimitive("Оператор"))
                                }
                            )
                            put("zReport", zxReport)
                        }
                    )
                }
            )
        }
    }

    /**
     * Преобразует входную модель сменного отчета [ZxReportInput] во внутренний JSON-формат,
     * требуемый протоколом ОФД, и автоматически рассчитывает и добавляет контрольную сумму (checksum).
     *
     * @param zx входные сменные данные [ZxReportInput].
     * @return JSON-объект [JsonObject] с полями отчета и контрольной суммой.
     */
    fun buildZxReportInternal(zx: ZxReportInput): JsonObject {
        val base = buildJsonObject {
            put("dateTime", OfdCommonRequestHelper.toDateTime(zx.dateTimeMillis))
            put("openShiftTime", OfdCommonRequestHelper.toDateTime(zx.openShiftTimeMillis))
            zx.closeShiftTimeMillis?.let { put("closeShiftTime", OfdCommonRequestHelper.toDateTime(it)) }
            put("shiftNumber", JsonPrimitive(zx.shiftNumber))
            put("cashSum", OfdCommonRequestHelper.moneyObject(zx.cashSumBills, 0))
            put(
                "revenue",
                buildJsonObject {
                    put(
                        "sum",
                        OfdCommonRequestHelper.moneyObject(
                            kotlin.math.abs(zx.revenueBills),
                            kotlin.math.abs(zx.revenueCoins)
                        )
                    )
                    put("isNegative", JsonPrimitive(zx.revenueBills < 0 || zx.revenueCoins < 0))
                }
            )
            put(
                "startShiftNonNullableSums",
                buildJsonArray {
                    zx.startShiftNonNullableSums.forEach { (op, sum) ->
                        add(
                            buildJsonObject {
                                put("operation", JsonPrimitive(op))
                                put("sum", OfdCommonRequestHelper.moneyObject(sum, 0))
                            }
                        )
                    }
                }
            )
            put(
                "nonNullableSums",
                buildJsonArray {
                    zx.nonNullableSums.forEach { (op, sum) ->
                        add(
                            buildJsonObject {
                                put("operation", JsonPrimitive(op))
                                put("sum", OfdCommonRequestHelper.moneyObject(sum, 0))
                            }
                        )
                    }
                }
            )
            put(
                "operations",
                buildJsonArray {
                    zx.operations.forEach { op ->
                        add(
                            buildJsonObject {
                                put("operation", JsonPrimitive(op.operation))
                                put("count", JsonPrimitive(op.count.toInt()))
                                put("sum", OfdCommonRequestHelper.moneyObject(op.sumBills, 0))
                            }
                        )
                    }
                }
            )
            put(
                "sections",
                buildJsonArray {
                    zx.sections.forEach { section ->
                        add(
                            buildJsonObject {
                                put("sectionCode", JsonPrimitive(section.sectionCode))
                                put(
                                    "operations",
                                    buildJsonArray {
                                        section.operations.forEach { op ->
                                            add(
                                                buildJsonObject {
                                                    put("operation", JsonPrimitive(op.operation))
                                                    put("count", JsonPrimitive(op.count.toInt()))
                                                    put("sum", OfdCommonRequestHelper.moneyObject(op.sumBills, 0))
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
            )
            put(
                "discounts",
                buildJsonArray {
                    zx.discounts.forEach { op ->
                        add(
                            buildJsonObject {
                                put("operation", JsonPrimitive(op.operation))
                                put("count", JsonPrimitive(op.count.toInt()))
                                put("sum", OfdCommonRequestHelper.moneyObject(op.sumBills, 0))
                            }
                        )
                    }
                }
            )
            put(
                "markups",
                buildJsonArray {
                    zx.markups.forEach { op ->
                        add(
                            buildJsonObject {
                                put("operation", JsonPrimitive(op.operation))
                                put("count", JsonPrimitive(op.count.toInt()))
                                put("sum", OfdCommonRequestHelper.moneyObject(op.sumBills, 0))
                            }
                        )
                    }
                }
            )
            put(
                "totalResult",
                buildJsonArray {
                    zx.totalResult.forEach { op ->
                        add(
                            buildJsonObject {
                                put("operation", JsonPrimitive(op.operation))
                                put("count", JsonPrimitive(op.count.toInt()))
                                put("sum", OfdCommonRequestHelper.moneyObject(op.sumBills, 0))
                            }
                        )
                    }
                }
            )
            put(
                "ticketOperations",
                buildJsonArray {
                    zx.ticketOperations.forEach { t ->
                        add(
                            buildJsonObject {
                                put("operation", JsonPrimitive(t.operation))
                                put("ticketsTotalCount", JsonPrimitive(t.ticketsTotalCount.toInt()))
                                put("ticketsCount", JsonPrimitive(t.ticketsCount.toInt()))
                                put("ticketsSum", OfdCommonRequestHelper.moneyObject(t.ticketsSumBills, 0))
                                put(
                                    "payments",
                                    buildJsonArray {
                                        val grouped = t.payments.groupBy {
                                            if (it.payment == "PAYMENT_ELECTRONIC") "PAYMENT_CARD" else it.payment
                                        }
                                        grouped.forEach { (pay, list) ->
                                            val totalSum = list.sumOf { it.sumBills }
                                            val totalCount = list.sumOf { it.count }
                                            add(
                                                buildJsonObject {
                                                    put("payment", JsonPrimitive(pay))
                                                    put("sum", OfdCommonRequestHelper.moneyObject(totalSum, 0))
                                                    put("count", JsonPrimitive(totalCount.toInt()))
                                                }
                                            )
                                        }
                                    }
                                )
                                put("offlineCount", JsonPrimitive(t.offlineCount.toInt()))
                                put("discountSum", OfdCommonRequestHelper.moneyObject(t.discountSumBills, 0))
                                put("markupSum", OfdCommonRequestHelper.moneyObject(t.markupSumBills, 0))
                                put("changeSum", OfdCommonRequestHelper.moneyObject(t.changeSumBills, 0))
                            }
                        )
                    }
                }
            )
            put(
                "moneyPlacements",
                buildJsonArray {
                    zx.moneyPlacements.forEach { m ->
                        add(
                            buildJsonObject {
                                put("operation", JsonPrimitive(m.operation))
                                put("operationsTotalCount", JsonPrimitive(m.operationsTotalCount.toInt()))
                                put("operationsCount", JsonPrimitive(m.operationsCount.toInt()))
                                put("operationsSum", OfdCommonRequestHelper.moneyObject(m.operationsSumBills, 0))
                                put("offlineCount", JsonPrimitive(m.offlineCount.toInt()))
                            }
                        )
                    }
                }
            )
            put(
                "taxes",
                buildJsonArray {
                    zx.taxes.forEach { tax ->
                        add(
                            buildJsonObject {
                                put("taxType", JsonPrimitive(tax.taxType))
                                put("taxTypeCode", JsonPrimitive(tax.taxTypeCode))
                                put("percent", JsonPrimitive(tax.percent))
                                put(
                                    "operations",
                                    buildJsonArray {
                                        tax.operations.forEach { op ->
                                            add(
                                                buildJsonObject {
                                                    put("operation", JsonPrimitive(op.operation))
                                                    put(
                                                        "turnover",
                                                        OfdCommonRequestHelper.moneyObject(op.turnoverBills, 0)
                                                    )
                                                    put(
                                                        "turnoverWithoutTax",
                                                        OfdCommonRequestHelper.moneyObject(
                                                            op.turnoverWithoutTaxBills,
                                                            0
                                                        )
                                                    )
                                                    put(
                                                        "sum",
                                                        OfdCommonRequestHelper.moneyObject(op.taxSumBills, 0)
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
            )
        }

        val checksum = calculateZxReportChecksum(base)

        return JsonObject(
            base.toMutableMap().apply {
                put("checksum", JsonPrimitive(checksum))
            }
        )
    }

    /**
     * Вычисляет контрольную сумму CRC32 для JSON-представления сменного отчета.
     *
     * Контрольная сумма возвращается в виде шестнадцатеричной строки в верхнем регистре,
     * дополненной нулями слева до 8 символов.
     *
     * @param zxJson сформированный JSON-объект отчета.
     * @return 8-символьная шестнадцатеричная строка CRC32 в верхнем регистре.
     */
    private fun calculateZxReportChecksum(zxJson: JsonObject): String {
        val jsonString = buildJsonObject {
            put("zxReport", zxJson)
        }.toString()
        val crc32 = CRC32()
        val bytes = jsonString.toByteArray(Charsets.UTF_8)
        crc32.update(bytes, 0, bytes.size)
        return java.lang.Long.toHexString(crc32.value)
            .padStart(8, '0')
            .uppercase()
    }
}
