package io.github.texport.superkassa.core.domain.impl.usecase.delivery

import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryFailure
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryOutcome
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryRetryPolicy
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryTask
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryTaskStatus
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.integration.DeliveryPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.logging.getLogger
import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.string.api.TrilingualMessage

/**
 * Отправка задач доставки чека: фоном — все, чей срок наступил,
 * по требованию кассира — задачи одного документа.
 *
 * Задача сначала занимается в хранилище и только потом уходит в канал:
 * фон и ручной повтор, взявшиеся за неё одновременно, не отправят чек
 * покупателю дважды. Отказ канала — причина в задаче и повтор по
 * [policy]; окончательный отказ и исчерпанные попытки — итог «не удалось».
 *
 * Вызовы блокирующие: ждут ответа канала.
 */
class SendDeliveryTasksUseCase(
    private val storage: StoragePort,
    private val delivery: DeliveryPort,
    private val requests: DeliveryRequests,
    private val clock: ClockPort,
    private val policy: DeliveryRetryPolicy
) {
    private val logger = getLogger(SendDeliveryTasksUseCase::class)

    /**
     * Отправляет не больше [limit] задач, срок которых наступил.
     *
     * @return сколько задач отправлено этим заходом, с успехом или отказом.
     * @throws UnsupportedOperationException если хранилище задач не держит.
     */
    fun sendDue(limit: Int): Int =
        storage.dueDeliveryTasks(clock.now(), limit).count { send(it) != null }

    /**
     * Отправляет задачи документа [documentId], срок которых наступил.
     *
     * @return все задачи документа после отправки.
     * @throws UnsupportedOperationException если хранилище задач не держит.
     */
    fun sendDocument(documentId: String): List<DeliveryTask> {
        storage.deliveryTasksOf(documentId).filter { it.status == DeliveryTaskStatus.PENDING }.forEach { send(it) }
        return storage.deliveryTasksOf(documentId)
    }

    /** Занимает задачу и отправляет; `null` — задачу занял другой отправитель или её срок не наступил. */
    private fun send(task: DeliveryTask): DeliveryTask? {
        val now = clock.now()
        if (!storage.claimDeliveryTask(task.id, now, now + policy.lease.inWholeMilliseconds)) return null
        val claimed = task.copy(attempts = task.attempts + 1)
        val settled = settle(claimed, attempt(claimed), clock.now())
        storage.saveDeliveryTask(settled)
        // Строка задачи несёт документ, канал, попытку и код — но не получателя.
        logger.info("Receipt delivery {}: {}", settled.status, settled)
        return settled
    }

    private fun attempt(task: DeliveryTask): DeliveryOutcome {
        val prepared = runCatching { requests.prepare(task) }.getOrElse { failure ->
            return failedBefore(PREPARATION_FAILED, CoreStrings.deliveryPreparationFailed(task.channel), failure)
        }
        return when (prepared) {
            is DeliveryRequests.Prepared.Refused -> prepared.outcome
            is DeliveryRequests.Prepared.Ready -> runCatching { delivery.send(prepared.request) }.getOrElse { failure ->
                val reason = failure::class.simpleName.orEmpty()
                failedBefore(CHANNEL_FAILED, CoreStrings.deliveryChannelFailed(task.channel, reason), failure)
            }
        }
    }

    /** Сбой по дороге к каналу: причина — только род ошибки, её текст может нести адрес покупателя. */
    private fun failedBefore(code: String, message: TrilingualMessage, failure: Throwable): DeliveryOutcome {
        logger.warn("Receipt delivery attempt broke: code={}, reason={}", code, failure::class.simpleName)
        return DeliveryOutcome.failed(DeliveryFailure(code, message), retryable = true)
    }

    private fun settle(task: DeliveryTask, outcome: DeliveryOutcome, now: Long): DeliveryTask = when {
        outcome.delivered -> task.copy(status = DeliveryTaskStatus.DELIVERED, failure = null, updatedAt = now)
        outcome.retryable && task.attempts < policy.attempts -> task.copy(
            status = DeliveryTaskStatus.PENDING,
            nextAttemptAt = now + policy.pauseAfter(task.attempts).inWholeMilliseconds,
            failure = outcome.failure,
            updatedAt = now
        )
        else -> task.copy(status = DeliveryTaskStatus.FAILED, failure = outcome.failure, updatedAt = now)
    }

    /** Коды сбоев по дороге к каналу. */
    companion object {
        /** Чек не удалось нарисовать или перевести в вид канала. */
        const val PREPARATION_FAILED: String = "DELIVERY_PREPARATION_FAILED"

        /** Канал упал, не ответив итогом. */
        const val CHANNEL_FAILED: String = "DELIVERY_CHANNEL_FAILED"
    }
}
