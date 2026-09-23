package io.github.texport.superkassa.embedded.impl.settings

import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import io.github.texport.superkassa.core.domain.api.port.integration.CoreSettingsRepositoryPort
import io.github.texport.superkassa.embedded.impl.storage.LocalFiles
import kotlinx.serialization.json.Json

/**
 * Настройки ядра в файле каталога данных.
 *
 * Испорченный файл не подменяется умолчаниями: в нём ОФД и сроки кассы,
 * и молча начать с других значит работать не с тем ОФД. Разбор падает
 * с причиной, и касса не поднимается.
 *
 * @param path путь к файлу настроек.
 */
internal class FileCoreSettings(private val path: String) : CoreSettingsRepositoryPort {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    override fun load(): CoreSettings? =
        LocalFiles.readText(path)?.let { json.decodeFromString(CoreSettings.serializer(), it) }

    override fun save(settings: CoreSettings): Boolean {
        LocalFiles.writeText(path, json.encodeToString(CoreSettings.serializer(), settings))
        return true
    }

    override fun loadOrCreate(defaults: CoreSettings): CoreSettings =
        load() ?: defaults.also { save(it) }
}
