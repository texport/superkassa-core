package kz.mybrain.superkassa.core.domain.usecase.shift

import kz.mybrain.superkassa.core.domain.exception.ConflictException
import kz.mybrain.superkassa.core.domain.exception.ErrorMessages
import kz.mybrain.superkassa.core.domain.exception.ValidationException
import kz.mybrain.superkassa.core.domain.model.auth.UserRole
import kz.mybrain.superkassa.core.domain.model.common.CounterKeyFormats
import kz.mybrain.superkassa.core.domain.model.common.format
import kz.mybrain.superkassa.core.domain.model.common.CounterScopes
import kz.mybrain.superkassa.core.domain.model.kkm.KkmInfo
import kz.mybrain.superkassa.core.domain.model.kkm.KkmState
import kz.mybrain.superkassa.core.domain.model.shift.ShiftInfo
import kz.mybrain.superkassa.core.domain.model.shift.ShiftStatus
import kz.mybrain.superkassa.core.domain.port.ClockPort
import kz.mybrain.superkassa.core.domain.port.IdGeneratorPort
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.core.domain.usecase.auth.AuthorizeUserUseCase

/**
 * Сценарий (Use Case) открытия смены контрольно-кассовой машины (ККМ).
 *
 * Данный класс инкапсулирует бизнес-логику открытия новой смены на ККМ:
 * 1. Проверяет существование ККМ и то, что она не находится в режиме программирования.
 * 2. Проверяет полномочия пользователя (открывать смену может только Администратор).
 * 3. Проверяет отсутствие уже открытой смены на данной ККМ.
 * 4. Вычисляет порядковый номер новой смены на основе истории локальных смен или счетчика ККМ.
 * 5. Создает запись о новой смене.
 * 6. Инициализирует сменные счетчики на основе накопленных глобальных счетчиков (например, по операциям продаж, возвратов и т.д.).
 *
 * @property storage Порт для доступа к хранилищу данных ККМ, смен и счетчиков.
 * @property idGenerator Порт для генерации уникальных идентификаторов.
 * @property clock Порт для получения системного времени.
 * @property authorizeUser Сценарий авторизации пользователей по PIN-коду.
 */
class OpenShiftUseCase(
    private val storage: StoragePort,
    private val idGenerator: IdGeneratorPort,
    private val clock: ClockPort,
    private val authorizeUser: AuthorizeUserUseCase
) {
    /**
     * Выполняет процедуру открытия смены.
     *
     * @param kkmId Уникальный идентификатор ККМ.
     * @param pin PIN-код администратора для авторизации.
     * @return [ShiftInfo] Данные об открытой смене.
     * @throws ValidationException если ККМ не найдена или находится в режиме программирования.
     * @throws ConflictException если на ККМ уже есть открытая смена.
     */
    fun execute(kkmId: String, pin: String): ShiftInfo {
        return storage.inTransaction {
            // Ищем ККМ в базе данных с блокировкой
            val kkm = storage.findKkmForUpdate(kkmId) ?: throw ValidationException(ErrorMessages.kkmNotFound(), "KKM_NOT_FOUND")
            
            // ККМ не должна находиться в режиме программирования
            requireNotProgramming(kkm)
            
            // Только Администратор имеет право открывать смену
            authorizeUser.execute(kkm.id, pin, setOf(UserRole.ADMIN))
            
            val now = clock.now()
            val shiftId = idGenerator.nextId()
            
            // Проверяем, не открыта ли уже смена на этой ККМ
            val existingShift = storage.findOpenShift(kkmId)
            if (existingShift != null) {
                throw ConflictException(
                    trilingualMessage = ErrorMessages.shiftAlreadyOpen(),
                    code = "SHIFT_ALREADY_OPEN"
                )
            }
            
            // Запрашиваем последнюю локально сохраненную смену для вычисления номера новой смены
            val lastLocalShiftNo = storage.listShifts(kkmId, limit = 1, offset = 0)
                .firstOrNull()
                ?.shiftNo

            // Вычисляем номер новой смены: инкрементируем последний номер смены
            val shiftNo = if (lastLocalShiftNo == null) {
                if (kkm.lastShiftNo == 1) {
                    1L
                } else {
                    (kkm.lastShiftNo?.toLong() ?: 0L) + 1L
                }
            } else {
                lastLocalShiftNo + 1L
            }
            
            val shift = ShiftInfo(
                id = shiftId,
                kkmId = kkmId,
                shiftNo = shiftNo,
                status = ShiftStatus.OPEN,
                openedAt = now
            )
            
            // Сохраняем открытую смену в базу данных
            storage.createShift(shift)
            
            // Обновляем информацию о ККМ, устанавливая номер последней смены
            storage.updateKkm(kkm.copy(updatedAt = now, lastShiftNo = shiftNo.toInt()))
 
            // Переносим накопительные итоги глобальных счетчиков в начало новой смены
            val globalCounters = storage.loadCounters(kkmId, CounterScopes.GLOBAL, null)
            val operations = listOf(
                "OPERATION_SELL",
                "OPERATION_SELL_RETURN",
                "OPERATION_BUY",
                "OPERATION_BUY_RETURN"
            )
            operations.forEach { op ->
                val globalKey = CounterKeyFormats.NON_NULLABLE_SUM.format(op)
                val startValue = globalCounters[globalKey] ?: 0L
                if (startValue != 0L) {
                    // Устанавливаем стартовое значение счетчика на начало смены
                    val startShiftKey = CounterKeyFormats.START_SHIFT_NON_NULLABLE_SUM.format(op)
                    storage.upsertCounter(kkmId, CounterScopes.SHIFT, shiftId, startShiftKey, startValue)

                    // Инициализируем текущее значение сменного счетчика
                    val shiftNonNullableKey = CounterKeyFormats.NON_NULLABLE_SUM.format(op)
                    storage.upsertCounter(kkmId, CounterScopes.SHIFT, shiftId, shiftNonNullableKey, startValue)
                }
            }

            shift
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
                trilingualMessage = ErrorMessages.kkmInProgramming(),
                code = "KKM_IN_PROGRAMMING"
            )
        }
    }
}
