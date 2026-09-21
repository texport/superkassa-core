package io.github.texport.superkassa.core.domain.impl.usecase.shift

import io.github.texport.superkassa.core.domain.impl.helper.common.assignPrintedDocumentNumber
import io.github.texport.superkassa.core.domain.api.exception.ConflictException
import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryStatus
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmState
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandStatus
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.api.model.queue.OfflineQueueCommandRequest
import io.github.texport.superkassa.core.domain.api.model.report.ReportResult
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftStatus
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.internal.IdGeneratorPort
import io.github.texport.superkassa.core.domain.api.port.internal.OfflineQueuePort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.api.port.integration.inTransaction
import io.github.texport.superkassa.core.domain.api.model.common.CounterKeyFormats
import io.github.texport.superkassa.core.domain.api.model.common.CounterScopes
import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.ofd.SendFiscalCommandUseCase
import io.github.texport.superkassa.core.domain.impl.logging.getLogger

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
    private val logger = getLogger(CloseShiftUseCase::class)

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
        logger.debug("Начало процедуры закрытия смены для ККМ: {}", kkmId)
        return storage.inTransaction {
            // Ищем ККМ в базе данных с блокировкой, если не найдена — выбрасываем исключение
            val kkm = storage.findKkmForUpdate(kkmId) ?: throw ValidationException(CoreStrings.kkmNotFound(), "KKM_NOT_FOUND")
            logger.debug("ККМ найдена. Состояние: {}", kkm.state)

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
            logger.debug("Найдена открытая смена: {}", shift.id)

            // Генерируем ID для фискального документа закрытия смены
            val documentId = idGenerator.nextId()
            logger.debug("Сгенерирован documentId для закрытия смены: {}", documentId)
            val now = clock.now()

            // Сохраняем фискальный документ закрытия смены (Z-отчет)
            storage.saveShiftDocument(kkmId, "SHIFT_CLOSE", documentId, shift.id, now)
            assignPrintedDocumentNumber(storage, kkmId, documentId)

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
                    storage.updateReceiptStatus(
                        documentId = documentId,
                        fiscalSign = null,
                        autonomousSign = now.toString(),
                        ofdStatus = "PENDING",
                        deliveredAt = null,
                        isAutonomous = true
                    )
                    DeliveryStatus.OFFLINE_QUEUED to null
                } else {
                    // Если связь есть, отправляем команду закрытия смены напрямую в ОФД
                    val result = sendFiscalCommandUseCase.execute(kkmId, OfdCommandType.CLOSE_SHIFT, documentId)
                    logger.debug("Результат отправки CLOSE_SHIFT в ОФД: {}", result.status)
                    val (status, ofdStatusText) = when (result.status) {
                        OfdCommandStatus.OK -> DeliveryStatus.ONLINE_OK to "SENT"
                        OfdCommandStatus.TIMEOUT -> DeliveryStatus.OFFLINE_QUEUED to "PENDING"
                        OfdCommandStatus.FAILED -> DeliveryStatus.ONLINE_ERROR to "FAILED"
                    }

                    storage.updateReceiptStatus(
                        documentId = documentId,
                        fiscalSign = result.fiscalSign,
                        autonomousSign = result.autonomousSign,
                        ofdStatus = ofdStatusText,
                        deliveredAt = if (result.status == OfdCommandStatus.OK) now else null,
                        isAutonomous = (result.status == OfdCommandStatus.TIMEOUT)
                    )

                    if (result.status == OfdCommandStatus.TIMEOUT) {
                        val command = OfflineQueueCommandRequest(
                            kkmId = kkmId,
                            type = OfdCommandType.CLOSE_SHIFT.value,
                            payloadRef = documentId
                        )
                        queue.enqueueOffline(command)
                    }

                    status to result.errorMessage
                }

            // Выполняем автоизъятие, если оно включено и в кассе есть наличные
            val globalCounters = storage.loadCounters(kkmId, CounterScopes.GLOBAL, null)
            val currentCash = globalCounters[CounterKeyFormats.CASH_SUM] ?: 0L
            logger.debug("Текущая сумма наличных: {}. Автоизъятие включено: {}", currentCash, kkm.autoCashout)
            if (kkm.autoCashout && currentCash > 0L) {
                logger.debug("Выполняется автоизъятие на сумму {}", currentCash)
                val cashOutDocId = idGenerator.nextId()
                storage.saveCashOperation(
                    kkmId = kkmId,
                    type = "CASH_OUT",
                    // cash.sum хранится в тиынах, а Money(bills, coins) первым
                    // берёт тенге: Money(currentCash, 0) записывал бы автоизъятие
                    // в сто раз больше — 9 720 ₸ ящика ушли бы документом
                    // на 972 000 ₸ и в ОФД.
                    amount = Money.fromTiyn(currentCash),
                    documentId = cashOutDocId,
                    shiftId = shift.id,
                    createdAt = now
                )
                storage.upsertCounter(kkmId, CounterScopes.GLOBAL, null, CounterKeyFormats.CASH_SUM, 0L)
                storage.upsertCounter(kkmId, CounterScopes.SHIFT, shift.id, CounterKeyFormats.CASH_SUM, 0L)
                // Документ уже записан изъятием выше: второй раз тем же
                // номером база его не принимает — закрытие смены падало
                // на ограничении первичного ключа, как только автоизъятие
                // становилось включаемым.
                assignPrintedDocumentNumber(storage, kkmId, cashOutDocId)

                deliverAutoCashout(kkmId, cashOutDocId, now)
            }

            // Фиксируем закрытие смены в локальной базе данных
            storage.closeShift(shift.id, ShiftStatus.CLOSED, now, documentId)
            logger.debug("Смена {} успешно закрыта в БД", shift.id)

            // Возвращаем результат генерации Z-отчета и его отправки
            ReportResult(
                documentId = documentId,
                deliveryStatus = deliveryStatus,
                deliveryError = deliveryError
            ).also { logger.debug("Процедура закрытия смены завершена с результатом: {}", it) }
        }
    }

    /**
     * Отправляет автоизъятие и записывает, чем дело кончилось.
     *
     * Состояние документа ставится по ответу так же, как у самого
     * Z-отчёта: прежде отправку только записывали в журнал, и документ
     * оставался «ожидает отправки» даже после того, как ОФД его принял —
     * в журнале кассира он висел так навсегда.
     *
     * Отказ отправки закрытие смены не отменяет: смена уже закрыта,
     * а изъятие доедет из очереди. Но и молчать о нём нельзя — отказ
     * виден кассиру состоянием документа.
     */
    private fun deliverAutoCashout(kkmId: String, documentId: String, now: Long) {
        val queued = OfflineQueueCommandRequest(
            kkmId = kkmId,
            type = OfdCommandType.MONEY_PLACEMENT.value,
            payloadRef = documentId
        )
        if (!queue.canSendDirectly(kkmId)) {
            queue.enqueueOffline(queued)
            return
        }
        val result = runCatching {
            sendFiscalCommandUseCase.execute(kkmId, OfdCommandType.MONEY_PLACEMENT, documentId)
        }.getOrElse { failure ->
            logger.error("Автоизъятие не ушло в ОФД: ${failure.message}")
            OfdCommandResult(status = OfdCommandStatus.TIMEOUT)
        }
        val delivered = result.status == OfdCommandStatus.OK
        storage.updateReceiptStatus(
            documentId = documentId,
            fiscalSign = result.fiscalSign,
            autonomousSign = result.autonomousSign,
            ofdStatus = statusOf(result.status),
            ofdErrorCode = result.resultCode?.takeIf { !delivered },
            deliveredAt = if (delivered) now else null,
            isAutonomous = result.status == OfdCommandStatus.TIMEOUT,
            ofdErrorText = result.resultText?.takeIf { !delivered && it.isNotBlank() }
        )
        if (result.status == OfdCommandStatus.TIMEOUT) {
            queue.enqueueOffline(queued)
        }
    }

    /** Состояние доставки словами журнала. */
    private fun statusOf(status: OfdCommandStatus): String = when (status) {
        OfdCommandStatus.OK -> "SENT"
        OfdCommandStatus.TIMEOUT -> "PENDING"
        OfdCommandStatus.FAILED -> "FAILED"
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
