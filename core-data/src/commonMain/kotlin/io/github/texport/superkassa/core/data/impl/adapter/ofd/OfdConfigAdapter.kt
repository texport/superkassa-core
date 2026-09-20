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
     * Проверяет, есть ли адрес для отправки команд указанному провайдеру ОФД в этом окружении.
     * @param providerId Строковый ID провайдера ОФД (например, "KAZAKHTELECOM").
     * @param environmentId Строковый ID окружения (например, "PROD").
     * @return true, если адрес известен; false в противном случае.
     */
    override fun hasEndpoint(providerId: String, environmentId: String): Boolean {
        val provider = OfdProvider.findProvider(providerId) ?: return false
        val environment = OfdEnvironment.findEnvironment(environmentId) ?: return false
        return provider.endpoint(environment) != null
    }
}
