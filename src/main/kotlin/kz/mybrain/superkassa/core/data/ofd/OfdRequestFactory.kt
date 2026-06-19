package kz.mybrain.superkassa.core.data.ofd

import kz.mybrain.superkassa.core.application.zxreport.ZxReportBuilder
import kz.mybrain.superkassa.core.domain.model.OfdServiceInfo
import kz.mybrain.superkassa.core.domain.model.ReceiptRequest
import kotlinx.serialization.json.JsonObject

/**
 * Фабрика JSON-запросов к ОФД.
 * Делегирует построение конкретных запросов специализированным билдерам для соблюдения лимитов на размер классов.
 */
object OfdRequestFactory {

    fun buildServicePayload(
        serviceInfo: OfdServiceInfo,
        registrationNumber: String,
        factoryNumber: String,
        systemId: String,
        offlineBeginMillis: Long,
        offlineEndMillis: Long
    ): JsonObject = OfdServiceRequestBuilder.buildServicePayload(
        serviceInfo, registrationNumber, factoryNumber, systemId,
        offlineBeginMillis, offlineEndMillis
    )

    fun buildServiceRequest(
        ofdId: String,
        protocolVersion: String,
        commandType: String,
        deviceId: Long,
        token: Long,
        reqNum: Int,
        offlineBeginMillis: Long,
        offlineEndMillis: Long,
        registrationNumber: String,
        factoryNumber: String,
        systemId: String,
        serviceInfo: OfdServiceInfo
    ): JsonObject = OfdServiceRequestBuilder.buildServiceRequest(
        ofdId, protocolVersion, commandType, deviceId, token, reqNum,
        offlineBeginMillis, offlineEndMillis, registrationNumber, factoryNumber, systemId, serviceInfo
    )

    fun buildTicketRequest(
        ofdId: String,
        protocolVersion: String,
        deviceId: Long,
        token: Long,
        reqNum: Int,
        request: ReceiptRequest,
        serviceBlock: JsonObject? = null
    ): JsonObject = OfdTicketRequestBuilder.buildTicketRequest(
        ofdId, protocolVersion, deviceId, token, reqNum, request, serviceBlock
    )

    fun buildMoneyPlacementRequest(
        ofdId: String,
        protocolVersion: String,
        deviceId: Long,
        token: Long,
        reqNum: Int,
        docType: String,
        amountBills: Long,
        createdAtMillis: Long,
        serviceBlock: JsonObject
    ): JsonObject = OfdMoneyPlacementRequestBuilder.buildMoneyPlacementRequest(
        ofdId, protocolVersion, deviceId, token, reqNum, docType, amountBills, createdAtMillis, serviceBlock
    )

    fun buildReportRequest(
        ofdId: String,
        protocolVersion: String,
        deviceId: Long,
        token: Long,
        reqNum: Int,
        reportType: String,
        zxReport: ZxReportBuilder.ZxReportInput,
        serviceBlock: JsonObject
    ): JsonObject = OfdReportRequestBuilder.buildReportRequest(
        ofdId, protocolVersion, deviceId, token, reqNum, reportType, zxReport, serviceBlock
    )

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
    ): JsonObject = OfdReportRequestBuilder.buildCloseShiftRequest(
        ofdId, protocolVersion, deviceId, token, reqNum, closeTimeMillis, frShiftNumber, zxReport, serviceBlock
    )

    fun buildZxReportInternal(zx: ZxReportBuilder.ZxReportInput): JsonObject =
        OfdReportRequestBuilder.buildZxReportInternal(zx)
}
