package io.github.texport.superkassa.embedded

import io.github.texport.superkassa.core.domain.api.exception.SettingsFrozenException
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Настройки ядра в приложении: чтение, правка по правилам ядра, файл каталога данных. */
class SettingsTest {
    private val dir: File = createTempDirectory("kassa-settings-").toFile()

    @AfterTest
    fun cleanUp() {
        dir.deleteRecursively()
    }

    @Test
    fun `правка сохраняется в каталоге и переживает перезапуск`() {
        TestCashRegister.open(dir).use { kassa ->
            val current = kassa.settings.getSettings()
            kassa.settings.updateSettings(current.copy(ofdTimeoutSeconds = 9))
        }

        TestCashRegister.open(dir).use { kassa ->
            assertEquals(9, kassa.settings.getSettings().ofdTimeoutSeconds)
        }
    }

    @Test
    fun `ОФД и версия протокола — те, с которыми касса поднята`() {
        TestCashRegister.open(dir).use { kassa ->
            val settings = kassa.settings.getSettings()

            assertEquals("KAZAKHTELECOM", settings.ofdProviderId)
            assertEquals("203", settings.ofdProtocolVersion)
            assertFailsWith<SettingsFrozenException> {
                kassa.settings.updateSettings(settings.copy(ofdProtocolVersion = "204"))
            }
        }
    }

    @Test
    fun `запрет правки, сохранённый владельцем, действует сразу`() {
        TestCashRegister.open(dir).use { kassa ->
            val current = kassa.settings.getSettings()
            kassa.settings.updateSettings(current.copy(allowChanges = false))

            assertFailsWith<SettingsFrozenException> { kassa.settings.updateSettings(current) }
        }
    }
}
