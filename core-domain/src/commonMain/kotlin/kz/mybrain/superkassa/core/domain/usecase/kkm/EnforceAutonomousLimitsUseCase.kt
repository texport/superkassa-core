package kz.mybrain.superkassa.core.domain.usecase.kkm

import kz.mybrain.superkassa.core.domain.exception.ConflictException
import kz.mybrain.superkassa.core.domain.exception.ErrorMessages
import kz.mybrain.superkassa.core.domain.model.kkm.KkmInfo
import kz.mybrain.superkassa.core.domain.model.kkm.KkmState
import kz.mybrain.superkassa.core.domain.port.ClockPort
import kz.mybrain.superkassa.core.domain.port.OfflineQueuePort
import kz.mybrain.superkassa.core.domain.port.StoragePort

/**
 * Сценарий проверки и обеспечения соблюдения лимитов автономной работы ККМ.
 *
 * Автономный режим активируется, когда в очереди появляются неотправленные документы (пропала связь с ОФД).
 * По закону или бизнес-правилам, автономная работа ограничена во времени (по умолчанию 72 часа).
 * Если этот лимит превышен, касса переводится в заблокированное состояние [KkmState.BLOCKED].
 * При восстановлении связи (очередь становится пустой) блокировка снимается автоматически.
 *
 * @property storage Порт для доступа к хранилищу данных ККМ.
 * @property queue Порт для проверки наличия документов в очереди.
 * @property clock Порт для работы с системным временем.
 * @property maxAutonomousDurationMs Максимально допустимая длительность автономной работы в миллисекундах.
 */
class EnforceAutonomousLimitsUseCase(
    private val storage: StoragePort,
    private val queue: OfflineQueuePort,
    private val clock: ClockPort,
    private val maxAutonomousDurationMs: Long = DEFAULT_MAX_AUTONOMOUS_DURATION_MS
) {
    companion object {
        /**
         * Дефолтный максимальный лимит автономной работы: 72 часа.
         */
        private const val DEFAULT_MAX_AUTONOMOUS_DURATION_MS = 72L * 60L * 60L * 1000L
    }

    /**
     * Выполняет проверку состояния автономного режима ККМ и обновляет его при необходимости.
     *
     * - Если очередь не пуста и автономный режим еще не был зафиксирован — устанавливается дата начала автономной работы.
     * - Если очередь пуста, но ККМ числилась в автономном режиме — автономный режим сбрасывается.
     * - Если лимит автономной работы превышен — касса блокируется [KkmState.BLOCKED] и выбрасывается исключение.
     * - Если касса была заблокирована, но очередь опустела — статус возвращается в [KkmState.ACTIVE].
     *
     * @param kkm Текущая информация о ККМ для проверки.
     * @throws ConflictException Если ККМ превысила лимит автономной работы.
     */
    fun execute(kkm: KkmInfo) {
        val now = clock.now()
        val hasQueue = !queue.canSendDirectly(kkm.id)
        val autonomousSince = kkm.autonomousSince

        // 1. Активация автономного режима при появлении неотправленных документов
        if (autonomousSince == null && hasQueue) {
            storage.updateKkm(kkm.copy(updatedAt = now, autonomousSince = now))
            return
        }

        // 2. Сброс автономного режима, если очередь пуста, и касса не заблокирована
        if (autonomousSince != null && !hasQueue && kkm.state != KkmState.BLOCKED.name) {
            storage.updateKkm(kkm.copy(updatedAt = now, autonomousSince = null))
            return
        }

        // 3. Блокировка кассы при превышении лимита автономной работы
        if (autonomousSince != null && now - autonomousSince > maxAutonomousDurationMs) {
            if (kkm.state != KkmState.BLOCKED.name) {
                storage.updateKkm(kkm.copy(updatedAt = now, state = KkmState.BLOCKED.name))
            }
            throw ConflictException(ErrorMessages.kkmAutonomousTooLong(), "KKM_AUTONOMOUS_TOO_LONG")
        }

        // 4. Разблокировка кассы при восстановлении связи и пустой очереди
        if (kkm.state == KkmState.BLOCKED.name) {
            if (hasQueue) {
                throw ConflictException(ErrorMessages.kkmAutonomousTooLong(), "KKM_AUTONOMOUS_TOO_LONG")
            }
            storage.updateKkm(
                kkm.copy(
                    updatedAt = now,
                    state = KkmState.ACTIVE.name,
                    autonomousSince = null
                )
            )
        }
    }
}
