package io.github.texport.superkassa.core.domain.impl.usecase.kkm

import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmMode
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmState
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.api.port.integration.inTransaction

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
    private val blockedState = KkmState.BLOCKED.name

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
            // Блокировку снимает ОФД ответом OK либо ввод верного токена,
            // а не поход в режим программирования и обратно. Пока состояние
            // восстанавливалось всегда активным, круг «войти — выйти» гасил
            // любую блокировку, включая снятие кассы с учёта.
            val restoredState = when {
                isDraft -> draftState
                kkm.blockReasonCode != null -> blockedState
                else -> registeredState
            }
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
