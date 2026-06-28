package kz.mybrain.superkassa.core.domain.port

import kz.mybrain.superkassa.core.domain.validation.OfdTagValidator

/**
 * Порт для работы с конфигурацией ОФД (провайдеры, окружения, endpoints).
 * Инкапсулирует доступ к реестру ОФД, изолируя логику приложения от деталей реализации
 * инфраструктурного и сетевого слоёв.
 */
interface OfdConfigPort {

    /**
     * Проверяет наличие провайдера, окружения и endpoint. Возвращает отформатированный тег ОФД.
     *
     * @param providerId идентификатор провайдера ОФД.
     * @param environmentId идентификатор окружения (например, dev, prod).
     * @return отформатированная строка тега (например, "PROVIDER:ENVIRONMENT").
     * @throws kz.mybrain.superkassa.core.domain.exception.ValidationException если комбинация невалидна.
     */
    fun validateAndFormatTag(providerId: String, environmentId: String): String =
        OfdTagValidator.validateAndFormatTag(providerId, environmentId)

    /**
     * Парсит тег ОФД вида "PROVIDER:ENVIRONMENT" в пару значений (providerId, environmentId).
     *
     * @param tag строка тега ОФД.
     * @return объект [Pair], содержащий идентификатор провайдера и идентификатор окружения.
     * @throws kz.mybrain.superkassa.core.domain.exception.ValidationException если формат тега некорректен.
     */
    fun parseTag(tag: String): Pair<String, String> =
        OfdTagValidator.parseTag(tag)

    /**
     * Проверяет, что существует конечная точка (endpoint) для данной пары providerId + environmentId.
     *
     * @param providerId идентификатор провайдера ОФД.
     * @param environmentId идентификатор окружения.
     * @return `true`, если endpoint зарегистрирован; `false` в противном случае.
     */
    fun hasEndpoint(providerId: String, environmentId: String): Boolean
}
