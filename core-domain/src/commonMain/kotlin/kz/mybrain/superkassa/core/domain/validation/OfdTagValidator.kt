package kz.mybrain.superkassa.core.domain.validation

import kz.mybrain.superkassa.core.domain.exception.ErrorMessages
import kz.mybrain.superkassa.core.domain.exception.ValidationException
import kz.mybrain.superkassa.core.domain.model.ofd.OfdEnvironment
import kz.mybrain.superkassa.core.domain.model.ofd.OfdProvider

/**
 * Валидатор и парсер тегов ОФД (Оператора Фискальных Данных).
 *
 * Относится к доменным бизнес-правилам и отвечает за проверку корректности
 * идентификаторов провайдеров и сред окружения ОФД, а также за сборку и разбор тегов ОФД.
 */
object OfdTagValidator {

    /**
     * Валидирует идентификатор провайдера и площадки ОФД, после чего формирует строковый тег.
     *
     * Строковый тег имеет формат `providerId:environmentName`.
     *
     * @param providerId Уникальный строковый идентификатор провайдера ОФД.
     * @param environmentId Идентификатор окружения ОФД (например, "PRD", "DEV").
     * @return Отформатированный строковый тег ОФД.
     * @throws ValidationException Если провайдер или среда окружения неизвестны, либо окружение не поддерживается провайдером.
     */
    fun validateAndFormatTag(providerId: String, environmentId: String): String {
        val provider = OfdProvider.findProvider(providerId)
            ?: throw ValidationException(
                ErrorMessages.ofdProviderUnknown(providerId),
                "OFD_PROVIDER_UNKNOWN"
            )
        val environment = OfdEnvironment.entries.firstOrNull { it.name.equals(environmentId, ignoreCase = true) }
            ?: throw ValidationException(
                ErrorMessages.ofdEnvironmentUnknown(environmentId),
                "OFD_ENVIRONMENT_UNKNOWN"
            )
        if (!provider.endpoints.containsKey(environment)) {
            throw ValidationException(
                ErrorMessages.ofdEnvironmentUnknown(environmentId),
                "OFD_ENVIRONMENT_UNKNOWN"
            )
        }
        return "${provider.id}:${environment.name}"
    }

    /**
     * Разбирает строковый тег ОФД на пару: идентификатор провайдера и имя окружения.
     *
     * Ожидается формат `providerId:environmentName`.
     *
     * @param tag Строковый тег ОФД для разбора.
     * @return Пара [Pair], где первое значение — ID провайдера, второе — имя окружения.
     * @throws ValidationException Если формат тега некорректен, либо провайдер/окружение неизвестны.
     */
    fun parseTag(tag: String): Pair<String, String> {
        val parts = tag.split(":")
        if (parts.size != 2) {
            throw ValidationException(
                ErrorMessages.ofdProviderTagInvalid(tag),
                "OFD_PROVIDER_TAG_INVALID"
            )
        }
        val provider = OfdProvider.findProvider(parts[0])
            ?: throw ValidationException(
                ErrorMessages.ofdProviderUnknown(parts[0]),
                "OFD_PROVIDER_UNKNOWN"
            )
        val environment = OfdEnvironment.entries.firstOrNull { it.name.equals(parts[1], ignoreCase = true) }
            ?: throw ValidationException(
                ErrorMessages.ofdEnvironmentUnknown(parts[1]),
                "OFD_ENVIRONMENT_UNKNOWN"
            )
        return provider.id to environment.name
    }
}
