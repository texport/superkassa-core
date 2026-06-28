package kz.mybrain.superkassa.core.data.ofd

import kotlinx.serialization.json.JsonObject
import kz.mybrain.superkassa.core.data.ofd.builder.OfdReportRequestBuilder
import kz.mybrain.superkassa.core.data.ofd.builder.OfdServiceRequestBuilder
import kz.mybrain.superkassa.core.data.ofd.builder.OfdTicketRequestBuilder
import kz.mybrain.superkassa.core.domain.model.ofd.OfdServiceInfo
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptRequest
import kz.mybrain.superkassa.core.domain.model.zxreport.*

/**
 * Фабрика для сборки JSON-запросов к оператору фискальных данных (ОФД).
 *
 * Данный класс делегирует построение конкретных видов запросов специализированным
 * классам-построителям (билдерам), что позволяет обойти ограничения на максимальный
 * размер классов в байт-коде JVM и обеспечивает разделение ответственности.
 */
object OfdRequestFactory {

    /**
     * Формирует сервисный блок полезной нагрузки (`payload`) для запросов ОФД.
     *
     * @param serviceInfo Информация о сервисе ОФД.
     * @param registrationNumber Регистрационный номер ККМ.
     * @param factoryNumber Заводской номер ККМ.
     * @param systemId Идентификатор системы.
     * @param offlineBeginMillis Время начала автономного режима работы (в миллисекундах).
     * @param offlineEndMillis Время окончания автономного режима работы (в миллисекундах).
     * @return Построенный [JsonObject] сервисного блока.
     */
    fun buildServicePayload(
        serviceInfo: OfdServiceInfo,
        registrationNumber: String,
        factoryNumber: String,
        systemId: String,
        offlineBeginMillis: Long,
        offlineEndMillis: Long
    ): JsonObject = OfdServiceRequestBuilder.buildServicePayload(
        serviceInfo,
        registrationNumber,
        factoryNumber,
        systemId,
        offlineBeginMillis,
        offlineEndMillis
    )

    /**
     * Строит полный сервисный запрос к ОФД.
     *
     * @param ofdId Идентификатор ОФД.
     * @param protocolVersion Версия используемого протокола ОФД.
     * @param commandType Тип команды (например, регистрация, активация).
     * @param deviceId Идентификатор устройства в системе ОФД.
     * @param token Токен авторизации устройства.
     * @param reqNum Порядковый номер запроса ККМ.
     * @param offlineBeginMillis Время начала автономной работы ККМ.
     * @param offlineEndMillis Время окончания автономной работы ККМ.
     * @param registrationNumber Регистрационный номер ККМ.
     * @param factoryNumber Заводской номер ККМ.
     * @param systemId Системный идентификатор ККМ.
     * @param serviceInfo Дополнительные параметры сервиса ОФД.
     * @return Готовый к кодированию JSON-объект запроса.
     */
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

    /**
     * Строит запрос чека (фискального билета) для отправки в ОФД.
     *
     * @param ofdId Идентификатор ОФД.
     * @param protocolVersion Версия протокола ОФД.
     * @param deviceId Идентификатор устройства в системе ОФД.
     * @param token Токен авторизации устройства.
     * @param reqNum Порядковый номер запроса ККМ.
     * @param request Запрос на фискализацию чека, содержащий данные продажи/возврата.
     * @param serviceBlock Дополнительный сервисный блок информации (опционально).
     * @return JSON-объект запроса чека.
     */
    fun buildTicketRequest(
        ofdId: String,
        protocolVersion: String,
        deviceId: Long,
        token: Long,
        reqNum: Int,
        request: ReceiptRequest,
        serviceBlock: JsonObject? = null
    ): JsonObject = OfdTicketRequestBuilder.buildTicketRequest(
        ofdId,
        protocolVersion,
        deviceId,
        token,
        reqNum,
        request,
        serviceBlock
    )

    /**
     * Строит запрос операции внесения или изъятия наличных денежных средств (Money Placement).
     *
     * @param ofdId Идентификатор ОФД.
     * @param protocolVersion Версия протокола ОФД.
     * @param deviceId Идентификатор ККМ.
     * @param token Сессионный токен.
     * @param reqNum Номер запроса.
     * @param docType Тип операции ("CASH_IN" для внесения, "CASH_OUT" для изъятия).
     * @param amountBills Сумма операции в минимальных денежных единицах (тиын/копейки).
     * @param createdAtMillis Время создания документа ККМ в миллисекундах.
     * @param serviceBlock Сервисный блок ОФД.
     * @return JSON-объект запроса для операции с наличными.
     */
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

    /**
     * Строит запрос отчета (например, сменного X-отчета или периодического отчета) ККМ к ОФД.
     *
     * @param ofdId Идентификатор ОФД.
     * @param protocolVersion Версия протокола ОФД.
     * @param deviceId Идентификатор ККМ.
     * @param token Сессионный токен ККМ.
     * @param reqNum Номер запроса.
     * @param reportType Тип отчета.
     * @param zxReport Данные сменных итогов для формирования отчета.
     * @param serviceBlock Сервисный блок ОФД.
     * @return JSON-объект запроса отчета.
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
    ): JsonObject = OfdReportRequestBuilder.buildReportRequest(
        ofdId,
        protocolVersion,
        deviceId,
        token,
        reqNum,
        reportType,
        zxReport,
        serviceBlock
    )

    /**
     * Строит запрос закрытия смены (Z-отчет) с передачей итогов в ОФД.
     *
     * @param ofdId Идентификатор ОФД.
     * @param protocolVersion Версия протокола ОФД.
     * @param deviceId Идентификатор ККМ.
     * @param token Сессионный токен ККМ.
     * @param reqNum Номер запроса.
     * @param closeTimeMillis Время закрытия смены в миллисекундах.
     * @param frShiftNumber Номер смены фискального регистратора.
     * @param zxReport Сформированные показатели Z-отчета в виде JSON-объекта.
     * @param serviceBlock Сервисный блок ОФД.
     * @return JSON-объект запроса закрытия смены.
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
    ): JsonObject = OfdReportRequestBuilder.buildCloseShiftRequest(
        ofdId, protocolVersion, deviceId, token, reqNum, closeTimeMillis, frShiftNumber, zxReport, serviceBlock
    )

    /**
     * Внутренний метод для сборки JSON-структуры X/Z-отчета (ZxReport).
     *
     * @param zx Входные данные для Z/X-отчета ККМ.
     * @return JSON-объект структуры отчета.
     */
    fun buildZxReportInternal(zx: ZxReportInput): JsonObject =
        OfdReportRequestBuilder.buildZxReportInternal(zx)
}
