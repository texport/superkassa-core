@file:Suppress("MagicNumber", "VariableNaming", "ComplexCondition")

package io.github.texport.superkassa.core.data.impl.adapter

import io.github.texport.superkassa.core.domain.api.port.internal.IdGeneratorPort
import kotlin.random.Random

/**
 * КМП-генератор уникальных идентификаторов по умолчанию.
 *
 * Генерирует криптографически уникальные UUID v4 и заводские номера ККМ
 * на основе кроссплатформенного генератора псевдослучайных чисел [Random].
 */
internal class DefaultIdGeneratorAdapter : IdGeneratorPort {

    /**
     * Генерирует следующий случайный UUID v4 в формате `xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx`.
     *
     * @return строка с уникальным идентификатором.
     */
    override fun nextId(): String {
        return generateUuidV4()
    }

    /**
     * Генерирует уникальный заводской номер ККМ с заданным префиксом.
     *
     * @param prefix префикс (например "SWK", "KASSA").
     * @return строка заводского номера (например "SWK-482019").
     */
    override fun generateFactoryNumber(prefix: String): String {
        val randomDigits = (100000..999999).random()
        return "$prefix-$randomDigits"
    }

    private fun generateUuidV4(): String {
        val bytes = ByteArray(16)
        Random.nextBytes(bytes)
        bytes[6] = (bytes[6].toInt() and 0x0f or 0x40).toByte()
        bytes[8] = (bytes[8].toInt() and 0x3f or 0x80).toByte()

        fun Byte.toHex(): String = (toInt() and 0xff).toString(16).padStart(2, '0')

        val sb = StringBuilder()
        for (i in 0 until 16) {
            if (i == 4 || i == 6 || i == 8 || i == 10) {
                sb.append('-')
            }
            sb.append(bytes[i].toHex())
        }
        return sb.toString()
    }
}
