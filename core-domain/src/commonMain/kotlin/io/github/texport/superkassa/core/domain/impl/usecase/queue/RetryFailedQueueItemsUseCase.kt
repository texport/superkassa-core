package io.github.texport.superkassa.core.domain.impl.usecase.queue

import io.github.texport.superkassa.core.domain.api.exception.ConflictException
import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmMode
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmState
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase

/**
 * Сценарий (Use Case) повторной отправки неудавшихся задач из локальной очереди ОФД.
 *
 * Переводит все задачи в очереди со статусом FAILED обратно в статус PENDING,
 * чтобы инициировать повторный цикл их обработки и отправки.
 * Для выполнения этой операции ККМ должна находиться в режиме программирования ([KkmMode.PROGRAMMING])
 * и смена кассы должна быть закрыта.
 *
 * @property storage Порт для доступа к хранилищу данных ККМ и смен.
 * @property authorizeUserUseCase Сценарий авторизации пользователей и проверки ролей/ККМ.
 */
class RetryFailedQueueItemsUseCase(
    private val storage: StoragePort,
    private val authorizeUserUseCase: AuthorizeUserUseCase
) {
    /**
     * Выполняет сценарий повтора отправки для всех зависших/ошибочных задач в офлайн-очереди ККМ.
     *
     * Требует авторизации с ролью [io.github.texport.superkassa.core.domain.api.model.auth.UserRole.ADMIN].
     *
     * @param kkmId Идентификатор кассового аппарата (ККМ).
     * @param pin ПИН-код администратора для авторизации.
     * @return Количество успешно обновленных задач в очереди.
     * @throws ValidationException Если ККМ находится не в режиме программирования.
     * @throws ConflictException Если на ККМ открыта смена.
     * @throws io.github.texport.superkassa.core.domain.api.exception.NotFoundException Если ККМ не найдена.
     */
    fun execute(kkmId: String, pin: String): Int {
        val kkm = authorizeUserUseCase.requireKkm(kkmId)
        authorizeUserUseCase.requireRole(
            kkmId,
            pin,
            setOf(io.github.texport.superkassa.core.domain.api.model.auth.UserRole.ADMIN)
        )

        if (kkm.mode != KkmMode.PROGRAMMING.name || kkm.state != KkmState.PROGRAMMING.name) {
            throw ValidationException(
                CoreStrings.kkmSettingsRequiresProgramming(),
                "KKM_NOT_IN_PROGRAMMING_FOR_QUEUE"
            )
        }

        val openShift = storage.findOpenShift(kkmId)
        if (openShift != null) {
            throw ConflictException(
                CoreStrings.kkmDeleteShiftOpen(),
                "QUEUE_MANAGEMENT_SHIFT_OPEN"
            )
        }

        return storage.inTransaction {
            val offline = storage.listQueueTasksByCashbox(kkmId, "OFFLINE", limit = 100, offset = 0)
            val failed = offline.filter { it.status == "FAILED" }
            if (failed.isEmpty()) return@inTransaction 0
            var updated = 0
            failed.forEach { cmd ->
                val ok = storage.updateQueueTaskStatus(
                    id = cmd.id,
                    status = "PENDING",
                    attempt = cmd.attempt,
                    lastError = null,
                    nextAttemptAt = null
                )
                if (ok) updated++
            }
            updated
        }
    }
}
