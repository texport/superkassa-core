package io.github.texport.superkassa.core.domain.impl.usecase.kkm

import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmMode
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmState
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort

/**
 * Сценарий входа ККМ в режим программирования.
 *
 * Режим программирования позволяет изменять критически важные настройки кассы,
 * такие как реквизиты ОФД, параметры налогообложения и брендирования чеков.
 * Во время этого режима обычные кассовые операции приостанавливаются.
 *
 * @property storage Порт для доступа к хранилищу данных.
 * @property clock Порт для работы с системным временем.
 */
class EnterProgrammingUseCase(
    private val storage: StoragePort,
    private val clock: ClockPort
) {
    /**
     * Переводит ККМ в режим программирования в рамках транзакции.
     *
     * Обновляет режим ([KkmInfo.mode]) и состояние ([KkmInfo.state]) ККМ на [KkmMode.PROGRAMMING].
     *
     * @param kkm Информация о ККМ для перевода в режим программирования.
     * @return Обновленная информация о ККМ с установленным режимом программирования.
     */
    fun execute(kkm: KkmInfo): KkmInfo {
        return storage.inTransaction {
            val updated = kkm.copy(
                updatedAt = clock.now(),
                mode = KkmMode.PROGRAMMING.name,
                state = KkmState.PROGRAMMING.name
            )
            storage.updateKkm(updated)
            updated
        }
    }
}
