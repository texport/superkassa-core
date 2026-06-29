package kz.mybrain.superkassa.core.domain.usecase.ofd

import kz.mybrain.superkassa.core.domain.model.common.CounterScopes
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandResult
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandStatus
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandType
import kz.mybrain.superkassa.core.domain.model.shift.ShiftInfo
import kz.mybrain.superkassa.core.domain.model.shift.ShiftStatus
import kz.mybrain.superkassa.core.domain.port.ClockPort
import kz.mybrain.superkassa.core.domain.port.IdGeneratorPort
import kz.mybrain.superkassa.core.domain.port.OfflineQueuePort
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.core.domain.usecase.auth.AuthorizeUserUseCase
import kz.mybrain.superkassa.core.domain.exception.ValidationException
import kz.mybrain.superkassa.core.domain.helper.KkmCommonHelper
import kz.mybrain.superkassa.core.domain.helper.OfdInfoCountersSnapshotParser

/**
 * Сценарий (Use Case) синхронизации счетчиков ККМ с данными ОФД.
 *
 * Выполняет запрос INFO из ОФД для получения актуального слепка счетчиков и состояния смен
 * на стороне Оператора Фискальных Данных, после чего обновляет локальную базу данных (счетчики,
 * состояние и номера смен ККМ) для устранения рассинхронизации.
 *
 * @property storage Порт для доступа к локальному хранилищу данных.
 * @property queue Порт для работы с офлайн-очередью команд ККМ.
 * @property clock Порт для работы с системным временем.
 * @property idGenerator Порт для генерации уникальных идентификаторов.
 * @property authorizeUserUseCase Сценарий проверки прав доступа и состояния ККМ.
 * @property kkmCommonHelper Вспомогательный класс общего функционала работы с ККМ.
 */
@Suppress("unused", "DuplicatedCode")
class SyncOfdCountersUseCase(
    private val storage: StoragePort,
    private val queue: OfflineQueuePort,
    private val clock: ClockPort,
    private val idGenerator: IdGeneratorPort,
    private val authorizeUserUseCase: AuthorizeUserUseCase,
    private val kkmCommonHelper: KkmCommonHelper
) {
    /**
     * Выполняет синхронизацию счетчиков с ОФД.
     *
     * @param kkmId Уникальный идентификатор ККМ.
     * @param pin PIN-код администратора для авторизации.
     * @return [OfdCommandResult] Результат выполнения команды ОФД.
     * @throws ValidationException при ошибках валидации ККМ, пин-кода или наличии неотправленных чеков в очереди.
     */
    fun execute(kkmId: String, pin: String): OfdCommandResult {
        // Проверяем, разрешена ли синхронизация (ККМ должна существовать, а в офлайн-очереди не должно быть документов)
        val kkm = kkmCommonHelper.requireSyncAllowed(
            kkmId = kkmId,
            pin = pin,
            allowOpenShift = true,
            authorization = authorizeUserUseCase,
            queue = queue
        )

        // Отправляем запрос информации в ОФД для получения текущего состояния и счетчиков
        val result = kkmCommonHelper.sendOfdCommand(
            kkm = kkm,
            commandType = OfdCommandType.INFO,
            payloadRef = idGenerator.nextId()
        )

        // Если ответ от ОФД успешно получен, обновляем локальную базу данных
        if (result.status == OfdCommandStatus.OK && result.responseJson != null) {
            storage.inTransaction {
                // Парсим полученный JSON ответа ОФД
                val snapshot = OfdInfoCountersSnapshotParser.parse(result.responseJson)
                val now = clock.now()

                // Обновляем локальные глобальные счетчики
                snapshot.globalCounters.forEach { (key, value) ->
                    storage.upsertCounter(kkmId, CounterScopes.GLOBAL, null, key, value)
                }

                // Определяем номер смены (из ОФД или последний локальный)
                val shiftNo = snapshot.shiftNumber ?: kkm.lastShiftNo ?: 0

                if (snapshot.isOpenShift) {
                    // Если в ОФД смена открыта, синхронизируем локальное состояние открытой смены
                    var localOpenShift = storage.findOpenShift(kkmId)
                    if (localOpenShift == null) {
                        // Если локально смена считалась закрытой, открываем новую смену на основе данных ОФД
                        val shiftId = idGenerator.nextId()
                        val newShift = ShiftInfo(
                            id = shiftId,
                            kkmId = kkmId,
                            shiftNo = shiftNo.toLong(),
                            status = ShiftStatus.OPEN,
                            openedAt = snapshot.openShiftTimeMillis ?: now,
                            closedAt = null
                        )
                        storage.createShift(newShift)
                        localOpenShift = newShift
                    }

                    // Записываем сменные счетчики, полученные от ОФД
                    val currentShiftId = localOpenShift.id
                    snapshot.shiftCounters.forEach { (key, value) ->
                        storage.upsertCounter(kkmId, CounterScopes.SHIFT, currentShiftId, key, value)
                    }
                } else {
                    // Если в ОФД смена закрыта, но локально числится открытой — закрываем локальную смену
                    val localOpenShift = storage.findOpenShift(kkmId)
                    if (localOpenShift != null) {
                        storage.closeShift(
                            shiftId = localOpenShift.id,
                            closedAt = snapshot.closeShiftTimeMillis ?: now,
                            status = ShiftStatus.CLOSED,
                            closeDocumentId = null
                        )
                    }
                }

                // Сбрасываем флаг автономного режима и обновляем номер последней смены у ККМ
                val freshKkm = storage.findKkmForUpdate(kkmId)
                if (freshKkm != null) {
                    storage.updateKkm(
                        freshKkm.copy(
                            updatedAt = now,
                            lastShiftNo = shiftNo,
                            autonomousSince = null
                        )
                    )
                }
            }
        }
        return result
    }
}
