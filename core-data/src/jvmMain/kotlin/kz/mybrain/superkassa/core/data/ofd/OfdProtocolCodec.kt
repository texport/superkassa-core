package kz.mybrain.superkassa.core.data.ofd

import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kz.mybrain.ofdcodec.application.DefaultRegistry
import kz.mybrain.ofdcodec.application.OfdCodec
import kz.mybrain.ofdcodec.domain.model.OfdCodecException
import kz.mybrain.superkassa.core.data.exception.OfdProtocolException

/**
 * Сериализатор и декодировщик сообщений протокола ОФД через библиотеку `ofd-proto-codec`.
 *
 * Предоставляет методы для перевода структурированных JSON-запросов в бинарный формат
 * и обратно, а также форматирует ошибки, возвращаемые внешним кодеком.
 *
 * @property codec Экземпляр кодека [OfdCodec], инициализированный реестром по умолчанию.
 */
class OfdProtocolCodec(
    private val codec: OfdCodec = OfdCodec(DefaultRegistry.create())
) {
    /**
     * Кодирует JSON-элемент запроса в массив байтов для отправки по сети.
     *
     * @param json Данные запроса в формате [JsonElement].
     * @return Бинарный массив байтов закодированного сообщения.
     * @throws OfdProtocolException Если кодирование завершилось ошибкой или в ответе отсутствует поле `messageBase64`.
     */
    fun encode(json: JsonElement): ByteArray {
        val result = codec.encode(json)
        if (result.isFailure) {
            val formatted = formatOfdCodecErrors(result.exceptionOrNull())
            throw OfdProtocolException(formatted, result.exceptionOrNull())
        }
        val output = result.getOrNull()
            ?: throw OfdProtocolException("OFD encode returned null")
        val base64 = output["messageBase64"]?.jsonPrimitive?.content
            ?: throw OfdProtocolException("messageBase64 missing in OFD encode output")
        return try {
            Base64.getDecoder().decode(base64)
        } catch (e: IllegalArgumentException) {
            throw OfdProtocolException("Invalid Base64 format in OFD response: ${e.message}", e)
        }
    }

    /**
     * Декодирует бинарный массив байтов ответа ОФД в JSON-объект.
     *
     * @param bytes Массив байтов ответа от сервера ОФД.
     * @return Декодированный [JsonObject].
     * @throws OfdProtocolException Если декодирование завершилось с ошибкой.
     */
    fun decode(bytes: ByteArray): JsonObject {
        val result = codec.decode(bytes)
        if (result.isFailure) {
            val formatted = formatOfdCodecErrors(result.exceptionOrNull())
            throw OfdProtocolException(formatted, result.exceptionOrNull())
        }
        return result.getOrNull()
            ?: throw OfdProtocolException("OFD decode returned null")
    }

    /**
     * Преобразует возникшее в кодеке исключение в читаемое текстовое представление ошибок.
     *
     * Собирает сообщения об ошибках (на русском или английском языках) и дополняет их
     * метаданными (путь к свойству, код ошибки).
     *
     * @param ex Возникшее исключение.
     * @return Строка с описанием всех обнаруженных ошибок.
     */
    private fun formatOfdCodecErrors(ex: Throwable?): String {
        val errors = (ex as? OfdCodecException)?.errors ?: return ex?.message ?: "Unknown OFD error"
        return errors.joinToString("; ") { err ->
            val msg = err.messageRu.takeIf { it.isNotBlank() }
                ?: err.messageEn.takeIf { it.isNotBlank() }
                ?: "Unknown error"
            val pathStr = err.path.takeIf { it.isNotBlank() }
            val codeStr = err.code.takeIf { it.isNotBlank() }
            val extra = listOfNotNull(
                pathStr?.let { "путь: $it" },
                codeStr?.let { "код: $it" }
            )
            if (extra.isNotEmpty()) "$msg (${extra.joinToString(", ")})" else msg
        }
    }

    companion object {
        /**
         * Преобразует JSON-строку в объект [JsonElement].
         *
         * @param text Исходный JSON-текст.
         * @return Объект [JsonElement].
         */
        @Suppress("unused")
        fun parseJson(text: String): JsonElement = Json.parseToJsonElement(text)
    }
}
