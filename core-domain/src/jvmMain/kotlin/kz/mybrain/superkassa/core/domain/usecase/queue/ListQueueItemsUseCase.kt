package kz.mybrain.superkassa.core.domain.usecase.queue

import kz.mybrain.superkassa.core.domain.model.auth.UserRole
import kz.mybrain.superkassa.core.domain.usecase.auth.AuthorizeUserUseCase
import kz.mybrain.superkassa.offline_queue.domain.model.QueueLane
import kz.mybrain.superkassa.offline_queue.domain.port.QueueStoragePort

/**
 * Сценарий (Use Case) для получения списка элементов очереди отправки документов в ОФД для ККМ.
 *
 * Предоставляет администраторам возможность просматривать статус документов,
 * ожидающих отправки, отправленных или завершившихся ошибкой во внешней очереди (в частности, в офлайн-режиме).
 *
 * @property queueStorage Порт для работы с хранилищем очереди задач отправки документов.
 * @property authorizeUserUseCase Сценарий авторизации пользователей и проверки ролей/ККМ.
 */
class ListQueueItemsUseCase(
    private val queueStorage: QueueStoragePort,
    private val authorizeUserUseCase: AuthorizeUserUseCase
) {
    /**
     * Представление элемента очереди ОФД для отображения в интерфейсе.
     *
     * @property id Уникальный идентификатор задачи в очереди.
     * @property lane Направление/канал очереди (например, "OFFLINE").
     * @property type Тип команды или документа (например, "TICKET", "REPORT_Z").
     * @property status Текущий статус выполнения задачи (например, "PENDING", "FAILED", "SENT").
     * @property attempt Количество совершенных попыток отправки.
     * @property nextAttemptAt Время следующей запланированной попытки отправки в миллисекундах (timestamp).
     * @property lastError Текст последней ошибки, возникшей при попытке отправки.
     */
    data class QueueItemView(
        val id: String,
        val lane: String,
        val type: String,
        val status: String,
        val attempt: Int,
        val nextAttemptAt: Long?,
        val lastError: String?
    )

    /**
     * Выполняет сценарий получения списка задач из офлайн-очереди ОФД.
     *
     * Доступ к выполнению сценария имеет только пользователь с ролью [UserRole.ADMIN].
     *
     * @param kkmId Идентификатор кассового аппарата (ККМ).
     * @param pin ПИН-код администратора для авторизации.
     * @return Список объектов [QueueItemView], представляющих состояние задач в очереди.
     * @throws kz.mybrain.superkassa.core.domain.exception.NotFoundException Если ККМ не найдена.
     * @throws kz.mybrain.superkassa.core.domain.exception.ForbiddenException Если ПИН-код неверный или у пользователя нет прав администратора.
     */
    @Suppress("unused")
    fun execute(kkmId: String, pin: String): List<QueueItemView> {
        val kkm = authorizeUserUseCase.requireKkm(kkmId)
        authorizeUserUseCase.requireRole(kkm.id, pin, setOf(UserRole.ADMIN))

        val offline = queueStorage.listByCashbox(kkmId, QueueLane.OFFLINE, limit = 100, offset = 0)

        return offline.map {
            QueueItemView(
                id = it.id,
                lane = it.lane.name,
                type = it.type.name,
                status = it.status.name,
                attempt = it.attempt,
                nextAttemptAt = it.nextAttemptAt,
                lastError = it.lastError
            )
        }
    }
}
