package io.github.texport.superkassa.core.data.impl.adapter.ofd

import io.github.texport.superkassa.core.domain.api.model.ofd.OfdEnvironment
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdProvider
import io.github.texport.superkassa.core.domain.api.port.internal.OfdConfigPort

/**
 * Адаптер OfdConfigPort, использующий доменные модели OfdProvider и OfdEnvironment.
 * Настраивается и создается как Spring-бин в superkassa-server.
 */
internal class OfdConfigAdapter : OfdConfigPort {
    /**
     * Проверяет существование сетевого эндпоинта для указанного провайдера ОФД и окружения.
     * @param providerId Строковый ID провайдера ОФД (например, "KAZAKHTELECOM").
     * @param environmentId Строковый ID окружения (например, "PRODUCTION").
     * @return true, если эндпоинт найден и настроен; false в противном случае.
     */
    override fun hasEndpoint(providerId: String, environmentId: String): Boolean {
        // Ищем провайдера в перечислении доменных моделей
        val provider = OfdProvider.findProvider(providerId) ?: return false
        // Ищем окружение, игнорируя регистр букв
        val environment = OfdEnvironment.entries.firstOrNull {
            it.name.equals(environmentId, ignoreCase = true)
        } ?: return false
        // Проверяем наличие сопоставленного эндпоинта
        return provider.endpoints[environment] != null
    }
}
