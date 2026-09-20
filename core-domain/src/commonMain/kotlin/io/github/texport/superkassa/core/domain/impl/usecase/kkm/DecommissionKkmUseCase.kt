package io.github.texport.superkassa.core.domain.impl.usecase.kkm

import io.github.texport.superkassa.core.domain.api.exception.ConflictException
import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.domain.api.exception.NotFoundException
import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmState
import io.github.texport.superkassa.core.domain.api.port.internal.OfflineQueuePort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort

import io.github.texport.superkassa.core.domain.impl.logging.getLogger

/**
 * Сценарий удаления (вывода из эксплуатации) ККМ.
 *
 * Удаление ККМ разрешено только при выполнении следующих условий:
 * 1. Касса находится в состоянии [KkmState.PROGRAMMING].
 * 2. Роль пользователя — [UserRole.ADMIN].
 * 3. Смена на кассе полностью закрыта (нет открытых смен).
 * 4. Касса не находится в автономном режиме.
 * 5. Очередь автономных команд/документов пуста и может отправлять запросы напрямую.
 *
 * @property storage Порт для доступа к хранилищу данных ККМ.
 * @property queue Порт для проверки и управления очередью команд ККМ.
 */
class DecommissionKkmUseCase(
    private val storage: StoragePort,
    private val queue: OfflineQueuePort
) {
    private val logger = getLogger(DecommissionKkmUseCase::class)

    /**
     * Проверяет возможность вывода кассы из эксплуатации без её удаления.
     *
     * @param kkm Информация о проверяемой ККМ.
     * @throws ValidationException Если касса находится не в режиме программирования.
     * @throws ConflictException Если открыта смена, идет автономный режим или очередь не пуста.
     */
    fun validateCanDelete(kkm: KkmInfo) {
        logger.info(
            "DecommissionKkmUseCase.validateCanDelete: validating deletion criteria for kkmId='{}', state='{}'",
            kkm.id,
            kkm.state
        )
        // 1. Проверка: касса должна быть в режиме программирования
        if (kkm.state != KkmState.PROGRAMMING.name) {
            logger.warn(
                "DecommissionKkmUseCase.validateCanDelete FAILED: state='{}' != PROGRAMMING for kkmId='{}'",
                kkm.state,
                kkm.id
            )
            throw ValidationException(
                CoreStrings.kkmDeleteRequiresProgramming(),
                "KKM_DELETE_REQUIRES_PROGRAMMING"
            )
        }
        // 2. Проверка: касса не должна быть в автономном режиме
        if (kkm.autonomousSince != null) {
            logger.warn(
                "DecommissionKkmUseCase.validateCanDelete FAILED: kkmId='{}' is in autonomous mode since {}",
                kkm.id,
                kkm.autonomousSince
            )
            throw ConflictException(
                CoreStrings.kkmDeleteAutonomousNotAllowed(),
                "KKM_DELETE_AUTONOMOUS_NOT_ALLOWED"
            )
        }
        // 3. Проверка: смена должна быть закрыта
        val openShift = storage.findOpenShift(kkm.id)
        if (openShift != null) {
            logger.warn("DecommissionKkmUseCase.validateCanDelete FAILED: kkmId='{}' has active open shift", kkm.id)
            throw ConflictException(CoreStrings.kkmDeleteShiftOpen(), "KKM_DELETE_SHIFT_OPEN")
        }
        // 4. Проверка: очередь не должна содержать неотправленных в ОФД документов
        if (!queue.canSendDirectly(kkm.id)) {
            logger.warn(
                "DecommissionKkmUseCase.validateCanDelete FAILED: kkmId='{}' has non-empty offline queue",
                kkm.id
            )
            throw ConflictException(
                CoreStrings.kkmDeleteQueueNotEmpty(),
                "KKM_DELETE_QUEUE_NOT_EMPTY"
            )
        }
        logger.info("DecommissionKkmUseCase.validateCanDelete PASSED for kkmId='{}'", kkm.id)
    }

    /**
     * Выполняет процедуру вывода кассы из эксплуатации (полное удаление).
     *
     * @param kkm Информация об удаляемой ККМ.
     * @return `true`, если касса была успешно удалена.
     * @throws ValidationException Если касса находится не в режиме программирования.
     * @throws ConflictException Если открыта смена или очередь команд не пуста.
     * @throws NotFoundException Если удаляемая касса не найдена в хранилище.
     */
    fun execute(kkm: KkmInfo): Boolean {
        logger.info("DecommissionKkmUseCase.execute: start decommissioning for kkmId='{}'", kkm.id)
        validateCanDelete(kkm)
        // Выполняем полное удаление из хранилища данных
        val deleted = storage.deleteKkmCompletely(kkm.id)
        if (!deleted) {
            logger.error(
                "DecommissionKkmUseCase.execute ERROR: deleteKkmCompletely returned false for kkmId='${kkm.id}'"
            )
            throw NotFoundException(trilingualMessage = CoreStrings.kkmNotFound(), code = "KKM_NOT_FOUND")
        }
        // Очищаем очередь команд для удаленной кассы
        queue.deleteQueuedCommands(kkm.id)
        logger.info("DecommissionKkmUseCase.execute SUCCESS: decommissioned kkmId='{}'", kkm.id)
        return true
    }
}
