package kz.mybrain.superkassa.core.domain.port

import kz.mybrain.superkassa.core.domain.model.settings.CoreSettings

/**
 * Порт репозитория настроек ядра системы (хранилище конфигурации).
 * Обеспечивает доступ к глобальным конфигурационным параметрам приложения и ККМ.
 */
@Suppress("unused")
interface CoreSettingsRepositoryPort {

    /**
     * Загружает текущие настройки ядра ККМ.
     *
     * @return объект настроек [CoreSettings] или `null`, если они ещё не были сохранены.
     */
    fun load(): CoreSettings?

    /**
     * Сохраняет переданные настройки ядра.
     *
     * @param settings объект настроек для сохранения.
     * @return `true`, если сохранение выполнено успешно; `false` в противном случае.
     */
    fun save(settings: CoreSettings): Boolean

    /**
     * Загружает настройки ядра, либо создаёт их со значениями по умолчанию, если они отсутствуют.
     *
     * @param defaults настройки по умолчанию, которые будут сохранены и возвращены в случае отсутствия данных.
     * @return существующие или вновь созданные настройки [CoreSettings].
     */
    fun loadOrCreate(defaults: CoreSettings): CoreSettings
}
