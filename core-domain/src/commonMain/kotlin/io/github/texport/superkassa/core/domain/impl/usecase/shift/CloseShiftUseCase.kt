package io.github.texport.superkassa.core.domain.impl.usecase.shift

import io.github.texport.superkassa.core.domain.api.exception.ConflictException
import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryStatus
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmState
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandStatus
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.api.model.queue.OfflineQueueCommandRequest
import io.github.texport.superkassa.core.domain.api.model.report.ReportResult
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftStatus
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.internal.IdGeneratorPort
import io.github.texport.superkassa.core.domain.api.port.internal.OfflineQueuePort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.ofd.SendFiscalCommandUseCase

/**
 * Сценарий (Use Case) закрытия смены контрольно-кассовой машины (ККМ) и генерации Z-отчета.
 *
 * Данный класс инкапсулирует бизнес-логику закрытия смены:
 * 1. Проверяет существование ККМ и то, что она не находится в режиме программирования.
 * 2. Авторизует пользователя с ролью Администратора или Кассира.
 * 3. Находит текущую открытую смену.
 * 4. Генерирует уникальный идентификатор документа.
 * 5. В зависимости от наличия офлайн-очереди:
 *    - Добавляет команду в очередь (если ККМ работает в офлайн-режиме).
 *    - Отправляет команду закрытия смены напрямую в ОФД (если связь доступна).
 * 6. Регистрирует факт закрытия смены в локальном хранилище.
 *
 * @property storage Порт для доступа к хранилищу данных ККМ, смен и счетчиков.
 * @property queue Порт для работы с автономной/офлайн очередью команд ККМ.
 * @property sendFiscalCommandUseCase Сценарий отправки фискальных команд в ОФД.
 * @property idGenerator Порт для генерации уникальных идентификаторов.
 * @property clock Порт для получения системного времени.
 * @property authorizeUser Сценарий авторизации пользователей по PIN-коду.
 */
class CloseShiftUseCase(
    private val storage: StoragePort,
    private val queue: OfflineQueuePort,
    private val sendFiscalCommandUseCase: SendFiscalCommandUseCase,
    private val idGenerator: IdGeneratorPort,
    private val clock: ClockPort,
    private val authorizeUser: AuthorizeUserUseCase
) {
    /**
     * Выполняет процедуру закрытия смены.
     *
     * @param kkmId Уникальный идентификатор ККМ.
     * @param pin PIN-код пользователя для проверки прав доступа.
     * @return [ReportResult] Результат закрытия смены и отправки Z-отчета.
     * @throws ValidationException если ККМ не найдена или находится в режиме программирования.
     * @throws ConflictException если смена на ККМ не открыта.
     */
    fun execute(kkmId: String, pin: String): ReportResult {
        return storage.inTransaction {
            // Ищем ККМ в базе данных с блокировкой, если не найдена — выбрасываем исключение
            val kkm = storage.findKkmForUpdate(kkmId) ?: throw ValidationException(CoreStrings.kkmNotFound(), "KKM_NOT_FOUND")

            // ККМ не должна находиться в режиме программирования/настройки
            requireNotProgramming(kkm)

            // Проверяем права пользователя: закрывать смену могут только Администратор или Кассир
            authorizeUser.execute(kkm.id, pin, setOf(UserRole.ADMIN, UserRole.CASHIER))

            // Проверяем, есть ли на данной ККМ открытая смена
            val shift = storage.findOpenShift(kkmId)
                ?: throw ConflictException(
                    CoreStrings.shiftNotOpen(),
                    "SHIFT_NOT_OPEN"
                )

            // Генерируем ID для фискального документа закрытия смены
            val documentId = idGenerator.nextId()
            val now = clock.now()

            // Проверяем, нужно ли отправлять документ через офлайн-очередь (например, если нет связи)
            val hasQueue = !queue.canSendDirectly(kkmId)
            val (deliveryStatus, deliveryError) =
                if (hasQueue) {
                    // Если связь отсутствует или есть очередь, ставим команду закрытия смены в офлайн-очередь
                    val command = OfflineQueueCommandRequest(
                        kkmId = kkmId,
                        type = OfdCommandType.CLOSE_SHIFT.value,
                        payloadRef = documentId
                    )
                    queue.enqueueOffline(command)
                    DeliveryStatus.OFFLINE_QUEUED to null
                } else {
                    // Если связь есть, отправляем команду закрытия смены напрямую в ОФД
                    val result = sendFiscalCommandUseCase.execute(kkmId, OfdCommandType.CLOSE_SHIFT, documentId)
                    when (result.status) {
                        OfdCommandStatus.OK -> DeliveryStatus.ONLINE_OK to null
                        OfdCommandStatus.TIMEOUT -> DeliveryStatus.OFFLINE_QUEUED to result.errorMessage
                        OfdCommandStatus.FAILED -> DeliveryStatus.ONLINE_ERROR to result.errorMessage
                    }
                }

            // Фиксируем закрытие смены в локальной базе данных
            storage.closeShift(shift.id, ShiftStatus.CLOSED, now, documentId)

            // Возвращаем результат генерации Z-отчета и его отправки
            ReportResult(
                documentId = documentId,
                deliveryStatus = deliveryStatus,
                deliveryError = deliveryError
            )
        }
    }

    /**
     * Проверяет, что ККМ не находится в режиме программирования.
     *
     * @param kkm Информация о ККМ.
     * @throws ValidationException если ККМ находится в режиме программирования.
     */
    private fun requireNotProgramming(kkm: KkmInfo) {
        if (kkm.state == KkmState.PROGRAMMING.name) {
            throw ValidationException(
                trilingualMessage = CoreStrings.kkmInProgramming(),
                code = "KKM_IN_PROGRAMMING"
            )
        }
    }
}
