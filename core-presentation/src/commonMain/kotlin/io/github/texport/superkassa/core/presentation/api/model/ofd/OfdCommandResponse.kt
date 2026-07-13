package io.github.texport.superkassa.core.presentation.api.model.ofd

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Результат выполнения фискальной команды в ОФД.
 */
@Serializable
@Schema(description = "Результат фискальной команды ОФД")
data class OfdCommandResponse(
    @Schema(description = "Статус выполнения запроса", example = "OK") val status: OfdCommandStatus,
    @Schema(description = "Бинарный ответ от ОФД", hidden = true) val responseBin: ByteArray? = null,
    @Schema(description = "Парсированный JSON-ответ от ОФД", hidden = true) val responseJson: kotlinx.serialization.json.JsonObject? = null,
    @Schema(description = "Числовой токен, возвращенный ОФД", example = "10529") val responseToken: Long? = null,
    @Schema(description = "Номер ответа ОФД", example = "202") val responseReqNum: Int? = null,
    @Schema(description = "Код результата обработки команды", example = "0") val resultCode: Int? = null,
    @Schema(description = "Описание результата обработки от ОФД", example = "OK") val resultText: String? = null,
    @Schema(description = "Фискальный признак документа", example = "3810283") val fiscalSign: String? = null,
    @Schema(description = "Автономный фискальный признак документа", example = "2837192") val autonomousSign: String? = null,
    @Schema(description = "Описание ошибки", example = "Timeout waiting for OFD response") val errorMessage: String? = null,
    @Schema(description = "Ссылка на электронный чек на сервере ОФД", example = "http://ofd.example.com/receipt/123") val receiptUrl: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is OfdCommandResponse) return false

        if (status != other.status) return false
        if (responseBin != null) {
            if (other.responseBin == null) return false
            if (!responseBin.contentEquals(other.responseBin)) return false
        } else if (other.responseBin != null) return false
        if (responseJson != other.responseJson) return false
        if (responseToken != other.responseToken) return false
        if (responseReqNum != other.responseReqNum) return false
        if (resultCode != other.resultCode) return false
        if (resultText != other.resultText) return false
        if (fiscalSign != other.fiscalSign) return false
        if (autonomousSign != other.autonomousSign) return false
        if (errorMessage != other.errorMessage) return false
        if (receiptUrl != other.receiptUrl) return false

        return true
    }

    override fun hashCode(): Int {
        var result = status.hashCode()
        result = 31 * result + (responseBin?.contentHashCode() ?: 0)
        result = 31 * result + (responseJson?.hashCode() ?: 0)
        result = 31 * result + (responseToken?.hashCode() ?: 0)
        result = 31 * result + (responseReqNum ?: 0)
        result = 31 * result + (resultCode ?: 0)
        result = 31 * result + (resultText?.hashCode() ?: 0)
        result = 31 * result + (fiscalSign?.hashCode() ?: 0)
        result = 31 * result + (autonomousSign?.hashCode() ?: 0)
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        result = 31 * result + (receiptUrl?.hashCode() ?: 0)
        return result
    }
}
