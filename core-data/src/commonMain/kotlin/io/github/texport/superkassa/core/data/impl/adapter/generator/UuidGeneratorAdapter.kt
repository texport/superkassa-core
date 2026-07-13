package io.github.texport.superkassa.core.data.impl.adapter.generator

import io.github.texport.superkassa.core.domain.api.port.internal.IdGeneratorPort
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Адаптер IdGeneratorPort на базе генератора UUID и случайных чисел.
 * Используется для создания уникальных идентификаторов и заводских номеров.
 */
internal object UuidGeneratorAdapter : IdGeneratorPort {

    /**
     * Генерирует уникальный случайный UUID версии 4.
     */
    @OptIn(kotlin.uuid.ExperimentalUuidApi::class)
    override fun nextId(): String = kotlin.uuid.Uuid.random().toString()

    /**
     * Генерирует уникальный заводской номер ККМ согласно казахстанскому стандарту.
     * Формат: prefix + [две последние цифры текущего года] + [10 случайных символов в HEX].
     * Пример: KZT26C08F7E1C3F
     * @param prefix Префикс заводского номера кассы.
     * @return Заводской номер кассы.
     */
    override fun generateFactoryNumber(prefix: String): String {
        val nowMillis = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(nowMillis)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val year = localDateTime.year % 100
        val bytes = ByteArray(5)
        kotlin.random.Random.nextBytes(bytes)
        val hex = bytes.joinToString("") {
            val byteHex = (it.toInt() and 0xFF).toString(16).uppercase()
            if (byteHex.length == 1) "0$byteHex" else byteHex
        }
        val yearStr = if (year < 10) "0$year" else year.toString()
        return prefix + yearStr + hex
    }
}
