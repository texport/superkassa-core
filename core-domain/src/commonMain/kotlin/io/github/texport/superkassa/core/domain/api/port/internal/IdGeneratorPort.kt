package io.github.texport.superkassa.core.domain.api.port.internal

/**
 * Порт генератора уникальных идентификаторов.
 * Используется для создания UUID документов ККМ, а также генерации заводских номеров касс.
 */
interface IdGeneratorPort {

    /**
     * Генерирует уникальный идентификатор сущности (например, UUID).
     *
     * @return строка с уникальным идентификатором.
     */
    fun nextId(): String

    /**
     * Генерирует заводской номер для ККМ при регистрации.
     *
     * @param prefix Префикс заводского номера кассы.
     * @return сгенерированный заводской номер.
     */
    fun generateFactoryNumber(prefix: String): String
}
