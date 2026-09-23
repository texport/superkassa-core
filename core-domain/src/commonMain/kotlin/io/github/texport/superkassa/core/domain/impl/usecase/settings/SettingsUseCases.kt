package io.github.texport.superkassa.core.domain.impl.usecase.settings

import io.github.texport.superkassa.core.domain.api.exception.SettingsFrozenException
import io.github.texport.superkassa.core.domain.api.model.settings.CoreMode
import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import io.github.texport.superkassa.core.domain.api.model.settings.withDeploymentOwned
import io.github.texport.superkassa.core.domain.api.port.integration.CoreSettingsRepositoryPort
import io.github.texport.superkassa.core.string.api.CoreStrings

/**
 * Текущие настройки ядра — те же, по которым касса работает.
 *
 * Сохранённая запись перекрывается полями запуска: иначе ответ называл бы
 * ОФД и версию протокола, на которых касса не разговаривает.
 *
 * @property repository хранилище настроек.
 * @property deployment настройки запуска: умолчания для первого запуска
 *   и владелец ОФД и версии протокола.
 */
class GetSettingsUseCase(
    private val repository: CoreSettingsRepositoryPort,
    private val deployment: CoreSettings
) {
    /** Настройки хранилища с полями запуска; при первом обращении сохраняются умолчания. */
    fun execute(): CoreSettings = repository.loadOrCreate(deployment).withDeploymentOwned(deployment)
}

/**
 * Изменение настроек ядра.
 *
 * Правила проверяются по действующим настройкам, прочитанным заново,
 * а не по снимку на запуске: запрет правки, сохранённый владельцем,
 * действует со следующего же вызова. Новые значения вступают в силу
 * при следующем запуске кассы.
 *
 * @property current действующие настройки.
 * @property repository хранилище настроек.
 */
class UpdateSettingsUseCase(
    private val current: GetSettingsUseCase,
    private val repository: CoreSettingsRepositoryPort
) {
    /**
     * Проверяет, что правка разрешена, и сохраняет новые настройки.
     *
     * Отказ, если касса работает в режиме SERVER, если владелец запретил
     * правку и если новая запись меняет версию протокола: её задаёт запуск,
     * и правка молча не подействовала бы.
     *
     * @param newSettings новые настройки целиком.
     * @return сохранённые настройки.
     * @throws SettingsFrozenException если правка запрещена.
     * @throws IllegalStateException если хранилище не сохранило запись.
     */
    fun execute(newSettings: CoreSettings): CoreSettings {
        val settings = current.execute()
        if (settings.mode == CoreMode.SERVER) throw SettingsFrozenException(CoreStrings.settingsFrozenServerMode())
        if (!settings.allowChanges) throw SettingsFrozenException(CoreStrings.settingsFrozen())
        if (newSettings.ofdProtocolVersion != settings.ofdProtocolVersion) {
            throw SettingsFrozenException(CoreStrings.ofdProtocolVersionFixedAtStartup(settings.ofdProtocolVersion))
        }
        check(repository.save(newSettings)) { "Core settings were not saved" }
        return newSettings
    }
}
