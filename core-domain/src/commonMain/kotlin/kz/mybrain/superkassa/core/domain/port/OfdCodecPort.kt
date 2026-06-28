package kz.mybrain.superkassa.core.domain.port

import kotlinx.serialization.json.JsonObject

/**
 * Порт кодека для сериализации и десериализации пакетов ОФД.
 */
interface OfdCodecPort {
    /**
     * Преобразует JsonObject запроса в бинарное представление протокола.
     */
    fun encode(json: JsonObject): ByteArray

    /**
     * Преобразует бинарный ответ протокола в JsonObject.
     */
    fun decode(bytes: ByteArray): JsonObject
}
