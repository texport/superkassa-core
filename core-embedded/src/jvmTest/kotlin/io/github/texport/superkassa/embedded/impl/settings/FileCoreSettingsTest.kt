package io.github.texport.superkassa.embedded.impl.settings

import io.github.texport.superkassa.core.domain.api.model.settings.CoreMode
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Файл настроек, записанный прежним ядром, читается новым.
 *
 * Прежние настройки несли пины по умолчанию для администратора и кассира.
 * Пинов по умолчанию больше нет, а файлы с этими полями ещё лежат
 * в каталогах данных приложений: касса с таким файлом должна подняться,
 * а поля — просто не читаться и при следующей записи исчезнуть.
 */
class FileCoreSettingsTest {
    private val dir: File = createTempDirectory("core-settings-").toFile()
    private val file = File(dir, "core-settings.json")

    @AfterTest
    fun cleanUp() {
        dir.deleteRecursively()
    }

    @Test
    fun `файл с прежними пинами по умолчанию читается, а пины не переносятся`() {
        file.writeText(
            """
            {
                "mode": "DESKTOP",
                "storage": { "engine": "SQLITE", "jdbcUrl": "jdbc:sqlite:data/core.db" },
                "allowChanges": true,
                "ofdProtocolVersion": "204",
                "defaultAdminPin": "4821",
                "defaultCashierPin": "4821"
            }
            """.trimIndent()
        )
        val settings = FileCoreSettings(file.path)

        val loaded = checkNotNull(settings.load())
        settings.save(loaded)

        assertEquals(CoreMode.DESKTOP, loaded.mode)
        assertEquals("204", loaded.ofdProtocolVersion)
        assertFalse("defaultAdminPin" in file.readText() || "defaultCashierPin" in file.readText(), file.readText())
    }
}
