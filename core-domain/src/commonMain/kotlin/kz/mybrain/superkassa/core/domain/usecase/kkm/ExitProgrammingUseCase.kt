package kz.mybrain.superkassa.core.domain.usecase.kkm

import kz.mybrain.superkassa.core.domain.model.kkm.KkmInfo
import kz.mybrain.superkassa.core.domain.model.kkm.KkmMode
import kz.mybrain.superkassa.core.domain.model.kkm.KkmState
import kz.mybrain.superkassa.core.domain.port.ClockPort
import kz.mybrain.superkassa.core.domain.port.StoragePort

/**
 * Сценарий выхода ККМ из режима программирования.
 *
 * Восстанавливает рабочий режим и состояние ККМ в зависимости от того,
 * является ли касса зарегистрированной или находится в статусе черновика.
 *
 * @property storage Порт для доступа к хранилищу данных.
 * @property clock Порт для работы с системным временем.
 */
class ExitProgrammingUseCase(
    private val storage: StoragePort,
    private val clock: ClockPort
) {
    private val draftMode = KkmMode.REGISTRATION.name
    private val draftState = KkmState.IDLE.name
    private val registeredMode = KkmMode.REGISTRATION.name
    private val registeredState = KkmState.ACTIVE.name

    /**
     * Выводит ККМ из режима программирования в рамках транзакции.
     *
     * Если регистрационный номер отсутствует ([KkmInfo.registrationNumber] пуст),
     * касса переводится в режим черновика ([KkmMode.REGISTRATION], [KkmState.IDLE]).
     * Иначе возвращается стандартный активный режим ([KkmMode.REGISTRATION], [KkmState.ACTIVE]).
     *
     * @param kkm Информация о ККМ для вывода из режима программирования.
     * @return Обновленная информация о ККМ с восстановленным рабочим состоянием.
     */
    fun execute(kkm: KkmInfo): KkmInfo {
        return storage.inTransaction {
            val isDraft = kkm.registrationNumber.isNullOrBlank()
            val restoredMode = if (isDraft) draftMode else registeredMode
            val restoredState = if (isDraft) draftState else registeredState
            val updated = kkm.copy(
                updatedAt = clock.now(),
                mode = restoredMode,
                state = restoredState
            )
            storage.updateKkm(updated)
            updated
        }
    }
}
