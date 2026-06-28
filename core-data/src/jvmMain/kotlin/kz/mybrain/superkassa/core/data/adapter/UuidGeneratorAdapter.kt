package kz.mybrain.superkassa.core.data.adapter

import java.util.UUID
import kz.mybrain.superkassa.core.domain.port.IdGeneratorPort

/**
 * Адаптер IdGeneratorPort на базе генератора UUID и случайных чисел.
 * Используется для создания уникальных идентификаторов и заводских номеров.
 */
object UuidGeneratorAdapter : IdGeneratorPort {
    /**
     * Генерирует уникальный случайный UUID версии 4.
     */
    override fun nextId(): String = UUID.randomUUID().toString()

    /**
     * Генерирует уникальный заводской номер ККМ согласно казахстанскому стандарту.
     * Формат: KZT + [две последние цифры текущего года] + [10 случайных символов в HEX].
     * Пример: KZT26C08F7E1C3F
     * @return Заводской номер кассы.
     */
    override fun generateFactoryNumber(): String {
        val year = java.time.Year.now().value % 100
        val random = java.security.SecureRandom()
        val bytes = ByteArray(8)
        random.nextBytes(bytes)
        val hex = bytes.joinToString("") { "%02X".format(it) }.take(10)
        return "KZT%02d%s".format(year, hex)
    }
}
