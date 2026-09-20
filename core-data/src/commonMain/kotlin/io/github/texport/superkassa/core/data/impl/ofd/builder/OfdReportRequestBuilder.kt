package io.github.texport.superkassa.core.data.impl.ofd.builder

import io.github.texport.superkassa.core.data.impl.util.Crc32
import io.github.texport.superkassa.core.domain.api.model.zxreport.*
import kotlinx.serialization.json.*

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
        serviceBlock: JsonObject,
        isOffline: Boolean = false,
        printedDocumentNumber: Long? = null
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
                            put("isOffline", JsonPrimitive(isOffline))
                            printedDocumentNumber?.let { put("printedDocumentNumber", JsonPrimitive(it)) }
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
        serviceBlock: JsonObject,
        isOffline: Boolean = false,
        printedDocumentNumber: Long? = null
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
                            put("isOffline", JsonPrimitive(isOffline))
                            printedDocumentNumber?.let { put("printedDocumentNumber", JsonPrimitive(it)) }
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
            // Остаток ящика уходит в ОФД целиком: раньше дробная часть
            // прибивалась нулём, и Z-отчёт расходился с настоящими деньгами.
            put("cashSum", OfdCommonRequestHelper.moneyFromTiyn(zx.cashSumTiyn))
            put(
                "revenue",
                buildJsonObject {
                    // Выручка уходит по модулю, знак отдельным признаком:
                    // так требует протокол.
                    val revenue = if (zx.revenueTiyn < 0) -zx.revenueTiyn else zx.revenueTiyn
                    put("sum", OfdCommonRequestHelper.moneyFromTiyn(revenue))
                    put("isNegative", JsonPrimitive(zx.revenueTiyn < 0))
                }
            )
            put(
                "startShiftNonNullableSums",
                buildJsonArray {
                    zx.startShiftNonNullableSums.forEach { (op, sum) ->
                        add(
                            buildJsonObject {
                                put("operation", JsonPrimitive(op))
                                put("sum", OfdCommonRequestHelper.moneyFromTiyn(sum))
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
                                put("sum", OfdCommonRequestHelper.moneyFromTiyn(sum))
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
                                put("sum", OfdCommonRequestHelper.moneyFromTiyn(op.sumTiyn))
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
                                                    put("sum", OfdCommonRequestHelper.moneyFromTiyn(op.sumTiyn))
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
                                put("sum", OfdCommonRequestHelper.moneyFromTiyn(op.sumTiyn))
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
                                put("sum", OfdCommonRequestHelper.moneyFromTiyn(op.sumTiyn))
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
                                put("sum", OfdCommonRequestHelper.moneyFromTiyn(op.sumTiyn))
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
                                put("ticketsSum", OfdCommonRequestHelper.moneyFromTiyn(t.ticketsSumTiyn))
                                put(
                                    "payments",
                                    buildJsonArray {
                                        val grouped = t.payments.groupBy {
                                            if (it.payment == "PAYMENT_ELECTRONIC") "PAYMENT_CARD" else it.payment
                                        }
                                        grouped.forEach { (pay, list) ->
                                            val totalSum = list.sumOf { it.sumTiyn }
                                            val totalCount = list.sumOf { it.count }
                                            add(
                                                buildJsonObject {
                                                    put("payment", JsonPrimitive(pay))
                                                    put("sum", OfdCommonRequestHelper.moneyFromTiyn(totalSum))
                                                    put("count", JsonPrimitive(totalCount.toInt()))
                                                }
                                            )
                                        }
                                    }
                                )
                                put("offlineCount", JsonPrimitive(t.offlineCount.toInt()))
                                put("discountSum", OfdCommonRequestHelper.moneyFromTiyn(t.discountSumTiyn))
                                put("markupSum", OfdCommonRequestHelper.moneyFromTiyn(t.markupSumTiyn))
                                put("changeSum", OfdCommonRequestHelper.moneyFromTiyn(t.changeSumTiyn))
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
                                put("operationsSum", OfdCommonRequestHelper.moneyFromTiyn(m.operationsSumTiyn))
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
                                                        OfdCommonRequestHelper.moneyFromTiyn(op.turnoverTiyn)
                                                    )
                                                    put(
                                                        "turnoverWithoutTax",
                                                        OfdCommonRequestHelper.moneyFromTiyn(op.turnoverWithoutTaxTiyn)
                                                    )
                                                    put(
                                                        "sum",
                                                        OfdCommonRequestHelper.moneyFromTiyn(op.taxSumTiyn)
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
        val bytes = jsonString.encodeToByteArray()
        val crcValue = Crc32.calculate(bytes)
        return crcValue.toString(16)
            .padStart(8, '0')
            .uppercase()
    }
}
