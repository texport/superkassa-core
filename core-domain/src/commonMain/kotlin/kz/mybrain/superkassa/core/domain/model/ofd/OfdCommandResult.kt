package kz.mybrain.superkassa.core.domain.model.ofd

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Результат выполнения фискальной команды в ОФД.
 *
 * @property status Статус выполнения запроса (OK, FAILED, TIMEOUT).
 * @property responseBin Бинарный ответ от ОФД.
 * @property responseJson Парсированный JSON-ответ от ОФД, если применимо.
 * @property responseToken Числовой токен, возвращенный ОФД.
 * @property responseReqNum Номер ответа ОФД.
 * @property resultCode Код результата обработки команды.
 * @property resultText Описание результата обработки от ОФД.
 * @property fiscalSign Фискальный признак документа.
 * @property autonomousSign Автономный фискальный признак документа.
 * @property errorMessage Описание ошибки на уровне сетевого обмена или обработки.
 * @property receiptUrl Ссылка на электронный чек на сервере ОФД.
 */
@Serializable
data class OfdCommandResult(
    val status: OfdCommandStatus,
    val responseBin: ByteArray? = null,
    val responseJson: JsonObject? = null,
    val responseToken: Long? = null,
    val responseReqNum: Int? = null,
    val resultCode: Int? = null,
    val resultText: String? = null,
    val fiscalSign: String? = null,
    val autonomousSign: String? = null,
    val errorMessage: String? = null,
    val receiptUrl: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is OfdCommandResult) return false

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
