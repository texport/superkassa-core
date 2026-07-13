package io.github.texport.superkassa.core.domain.impl.usecase.queue

import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase

/**
 * Сценарий (Use Case) для получения списка элементов очереди отправки документов в ОФД для ККМ.
 *
 * Предоставляет администраторам возможность просматривать статус документов,
 * ожидающих отправки, отправленных или завершившихся ошибкой во внешней очереди (в частности, в офлайн-режиме).
 *
 * @property storage Порт для доступа к хранилищу данных ККМ.
 * @property authorizeUserUseCase Сценарий авторизации пользователей и проверки ролей/ККМ.
 */
class ListQueueItemsUseCase(
    private val storage: StoragePort,
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
        val lastError: String?,
        val errorRu: String? = null,
        val errorKk: String? = null,
        val errorEn: String? = null
    )

    private fun parseCompactError(compactError: String?): Triple<String?, String?, String?> {
        if (compactError == null) return Triple(null, null, null)
        val regex = Regex("""^RU:\s*(.*?)\s*\|\s*KK:\s*(.*?)\s*\|\s*EN:\s*(.*?)$""")
        val match = regex.find(compactError)
        return if (match != null) {
            Triple(match.groupValues[1].trim(), match.groupValues[2].trim(), match.groupValues[3].trim())
        } else {
            Triple(compactError, compactError, compactError)
        }
    }

    /**
     * Выполняет сценарий получения списка задач из офлайн-очереди ОФД.
     *
     * Доступ к выполнению сценария имеет только пользователь с ролью [UserRole.ADMIN].
     *
     * @param kkmId Идентификатор кассового аппарата (ККМ).
     * @param pin ПИН-код администратора для авторизации.
     * @return Список объектов [QueueItemView], представляющих состояние задач в очереди.
     * @throws io.github.texport.superkassa.core.domain.api.exception.NotFoundException Если ККМ не найдена.
     * @throws io.github.texport.superkassa.core.domain.api.exception.ForbiddenException Если ПИН-код неверный или у пользователя нет прав администратора.
     */
    fun execute(kkmId: String, pin: String): List<QueueItemView> {
        val kkm = authorizeUserUseCase.requireKkm(kkmId)
        authorizeUserUseCase.requireRole(kkm.id, pin, setOf(UserRole.ADMIN))

        val offline = storage.listQueueTasksByCashbox(kkmId, "OFFLINE", limit = 100, offset = 0)

        return offline.map {
            val (ru, kk, en) = parseCompactError(it.lastError)
            QueueItemView(
                id = it.id,
                lane = it.lane,
                type = it.type,
                status = it.status,
                attempt = it.attempt,
                nextAttemptAt = it.nextAttemptAt,
                lastError = it.lastError,
                errorRu = ru,
                errorKk = kk,
                errorEn = en
            )
        }
    }
}
