package kz.mybrain.superkassa.core.domain.usecase.kkm

import kz.mybrain.superkassa.core.domain.model.kkm.KkmInfo
import kz.mybrain.superkassa.core.domain.model.kkm.KkmMode
import kz.mybrain.superkassa.core.domain.model.kkm.KkmState
import kz.mybrain.superkassa.core.domain.port.ClockPort
import kz.mybrain.superkassa.core.domain.port.StoragePort

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
