package io.github.texport.superkassa.core.domain.impl.usecase.settings

import io.github.texport.superkassa.core.domain.api.exception.SettingsFrozenException
import io.github.texport.superkassa.core.domain.api.model.settings.CoreMode
import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import io.github.texport.superkassa.core.domain.api.model.settings.StorageSettings
import io.github.texport.superkassa.core.domain.api.port.integration.CoreSettingsRepositoryPort
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** Правила изменения настроек: те же, что у узла в `server-settings`. */
class SettingsUseCasesTest {

    private class Repository(var stored: CoreSettings?) : CoreSettingsRepositoryPort {
        var saves = 0
        override fun load(): CoreSettings? = stored
        override fun save(settings: CoreSettings): Boolean {
            saves++
            stored = settings
            return true
        }
        override fun loadOrCreate(defaults: CoreSettings): CoreSettings = stored ?: defaults.also { save(it) }
    }

    private val desktop = CoreSettings(
        mode = CoreMode.DESKTOP,
        storage = StorageSettings(engine = "SQLITE", jdbcUrl = "jdbc:sqlite:build/core.db"),
        allowChanges = true,
        ofdProtocolVersion = "203",
        ofdProviderId = "KAZAKHTELECOM"
    )

    private fun useCases(stored: CoreSettings?, deployment: CoreSettings = desktop): Pair<Repository, UpdateSettingsUseCase> {
        val repository = Repository(stored)
        return repository to UpdateSettingsUseCase(GetSettingsUseCase(repository, deployment), repository)
    }

    @Test
    fun `на рабочем месте с разрешённой правкой настройки сохраняются`() {
        val (repository, update) = useCases(desktop)
        val next = desktop.copy(ofdTimeoutSeconds = 9)

        assertEquals(next, update.execute(next))
        assertEquals(next, repository.stored)
    }

    @Test
    fun `запрет правки отказывает и ничего не сохраняет`() {
        val (repository, update) = useCases(desktop.copy(allowChanges = false))

        val refused = assertFailsWith<SettingsFrozenException> { update.execute(desktop) }

        assertContains(refused.message.orEmpty(), "Изменение настроек заморожено")
        assertEquals(0, repository.saves)
    }

    @Test
    fun `в режиме SERVER правка через API запрещена`() {
        val server = desktop.copy(mode = CoreMode.SERVER)
        val (repository, update) = useCases(server)

        val refused = assertFailsWith<SettingsFrozenException> { update.execute(server) }

        assertContains(refused.message.orEmpty(), "Настройки не могут быть изменены через API в режиме SERVER")
        assertEquals(0, repository.saves)
    }

    /** Правка версии протокола раньше принималась и молча не действовала. */
    @Test
    fun `версия протокола, заданная запуском, не меняется`() {
        val (repository, update) = useCases(desktop, deployment = desktop.copy(ofdProtocolVersion = "204"))

        val refused = assertFailsWith<SettingsFrozenException> { update.execute(desktop.copy(ofdProtocolVersion = "203")) }

        assertContains(refused.message.orEmpty(), "204")
        assertEquals(0, repository.saves)
    }

    /** Запрет, сохранённый владельцем, действует со следующего же вызова, без перезапуска. */
    @Test
    fun `сохранённый запрет правки действует сразу`() {
        val (_, update) = useCases(desktop)

        update.execute(desktop.copy(allowChanges = false))

        assertFailsWith<SettingsFrozenException> { update.execute(desktop) }
    }

    /**
     * Ответ называет ОФД и версию, на которых касса разговаривает.
     *
     * Сохранённая запись говорила 204, запуск — 203: экран диагностики
     * показывал «Протокол ОФД 204» над обменом по 2.0.3.
     */
    @Test
    fun `настройки отвечают полями запуска поверх сохранённой записи`() {
        val repository = Repository(desktop.copy(ofdProtocolVersion = "204", ofdProviderId = "BFD"))

        val settings = GetSettingsUseCase(repository, desktop).execute()

        assertEquals("203", settings.ofdProtocolVersion)
        assertEquals("KAZAKHTELECOM", settings.ofdProviderId)
    }

    @Test
    fun `первое чтение сохраняет умолчания запуска`() {
        val repository = Repository(null)

        assertEquals(desktop, GetSettingsUseCase(repository, desktop).execute())
        assertEquals(desktop, repository.stored)
        assertNull(Repository(null).stored)
    }
}
