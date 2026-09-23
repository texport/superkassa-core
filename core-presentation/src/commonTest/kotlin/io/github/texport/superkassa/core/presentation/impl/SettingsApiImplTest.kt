package io.github.texport.superkassa.core.presentation.impl

import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import io.github.texport.superkassa.core.domain.impl.usecase.settings.GetSettingsUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.settings.UpdateSettingsUseCase
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertSame

class SettingsApiImplTest {

    private val settings = mockk<CoreSettings>()
    private val getSettings = mockk<GetSettingsUseCase>()
    private val updateSettings = mockk<UpdateSettingsUseCase>()
    private val api = SettingsApiImpl(getSettings, updateSettings)

    @Test
    fun `settings are read and saved through the domain use cases`() {
        every { getSettings.execute() } returns settings
        every { updateSettings.execute(settings) } returns settings

        assertSame(settings, api.getSettings())
        assertSame(settings, api.updateSettings(settings))
    }
}
