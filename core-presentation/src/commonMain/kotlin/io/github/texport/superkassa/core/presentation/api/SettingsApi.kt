package io.github.texport.superkassa.core.presentation.api

import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings

/**
 * Настройки ядра: чтение и правка по правилам ядра.
 *
 * Настройки не относятся ни к одной кассе и пина не спрашивают: доступ
 * к ним решает владелец процесса, в котором работает ядро.
 */
interface SettingsApi {

    /**
     * Действующие настройки: сохранённая запись с полями, которыми владеет
     * запуск, — ОФД и версией протокола.
     */
    fun getSettings(): CoreSettings

    /**
     * Сохраняет настройки целиком. Новые значения действуют со следующего запуска.
     *
     * @param settings новые настройки.
     * @return сохранённые настройки.
     * @throws io.github.texport.superkassa.core.domain.api.exception.SettingsFrozenException
     *   в режиме SERVER, при запрещённой правке и при смене версии протокола.
     */
    fun updateSettings(settings: CoreSettings): CoreSettings
}
