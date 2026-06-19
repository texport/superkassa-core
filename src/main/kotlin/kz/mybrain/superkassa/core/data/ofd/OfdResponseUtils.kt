package kz.mybrain.superkassa.core.data.ofd

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64

/**
 * Утилиты для извлечения данных из ответа ОФД.
 */
object OfdResponseUtils {

    /**
     * Извлекает URL чека из ответа ОФД.
     * Ответ может содержать payload.ticket.qrCodeBase64 (Base64-кодированный URL)
     * или payload.ticket.qr_code.
     */
    fun extractReceiptUrl(responseJson: JsonObject?): String? {
        if (responseJson == null) return null
        val payload = responseJson["payload"]?.jsonObject ?: return null
        val ticket = payload["ticket"]?.jsonObject ?: return null
        
        val qrCodeBase64 = ticket["qrCodeBase64"]?.jsonPrimitive?.content
        if (qrCodeBase64 != null) {
            return try {
                String(Base64.getDecoder().decode(qrCodeBase64), Charsets.UTF_8)
            } catch (e: Exception) {
                null
            }
        }
        
        return ticket["qr_code"]?.jsonPrimitive?.content
    }

    /**
     * Извлекает фискальный признак из ответа ОФД.
     * Ищет в payload.ticket.fiscalSign, payload.report.zxReport.fiscalSignature или payload.ticket.ticket_number.
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
