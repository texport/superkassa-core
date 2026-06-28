package kz.mybrain.superkassa.core.data.ofd

import java.util.Base64
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Утилиты для извлечения фискальных данных из ответа ОФД.
 */
object OfdResponseUtils {

    /**
     * Извлекает URL-адрес фискального чека из ответа ОФД.
     *
     * Поиск выполняется по приоритету в следующих полях:
     * - `payload.ticket.qrCodeBase64` (Base64-кодированный URL-адрес).
     * - `payload.ticket.qr_code` (прямой URL-адрес).
     *
     * @param responseJson JSON-объект ответа ОФД.
     * @return URL-адрес чека или null, если он не найден или некорректен.
     */
    fun extractReceiptUrl(responseJson: JsonObject?): String? {
        if (responseJson == null) return null
        val payload = responseJson["payload"]?.jsonObject ?: return null
        val ticket = payload["ticket"]?.jsonObject ?: return null

        val qrCodeBase64 = ticket["qrCodeBase64"]?.jsonPrimitive?.content
        if (qrCodeBase64 != null) {
            return try {
                String(Base64.getDecoder().decode(qrCodeBase64), Charsets.UTF_8)
            } catch (_: Exception) {
                null
            }
        }

        return ticket["qr_code"]?.jsonPrimitive?.content
    }

    /**
     * Извлекает фискальный признак документа из ответа ОФД.
     *
     * Поиск фискального признака выполняется в следующих структурах (в порядке приоритета):
     * 1. В билете чека (`payload.ticket.fiscalSign`).
     * 2. В номере чека (`payload.ticket.ticket_number`).
     * 3. В Z/X-отчете (`payload.report.zxReport.fiscalSignature`).
     *
     * @param responseJson JSON-объект ответа ОФД.
     * @return Строка с фискальным признаком или null, если он не найден.
     */
    fun extractFiscalSign(responseJson: JsonObject?): String? {
        if (responseJson == null) return null
        val payload = responseJson["payload"]?.jsonObject ?: return null

        val ticket = payload["ticket"]?.jsonObject
        if (ticket != null) {
            val fiscalSign = ticket["fiscalSign"]?.jsonPrimitive?.content
            if (fiscalSign != null) return fiscalSign

            val ticketNumber = ticket["ticket_number"]?.jsonPrimitive?.content
            if (ticketNumber != null) return ticketNumber
        }

        val report = payload["report"]?.jsonObject
        val zxReport = report?.get("zxReport")?.jsonObject
        if (zxReport != null) {
            val fiscalSignature = zxReport["fiscalSignature"]?.jsonPrimitive?.content
            if (fiscalSignature != null) return fiscalSignature
        }

        return null
    }
}
