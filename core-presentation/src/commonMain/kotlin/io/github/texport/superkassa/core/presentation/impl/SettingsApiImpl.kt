package io.github.texport.superkassa.core.presentation.impl

import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import io.github.texport.superkassa.core.domain.impl.usecase.settings.GetSettingsUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.settings.UpdateSettingsUseCase
import io.github.texport.superkassa.core.presentation.api.SettingsApi

/** Настройки ядра через сценарии домена. */
class SettingsApiImpl(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase
) : SettingsApi {
    override fun getSettings(): CoreSettings = getSettingsUseCase.execute()

    override fun updateSettings(settings: CoreSettings): CoreSettings = updateSettingsUseCase.execute(settings)
}
