package io.github.texport.superkassa.embedded.impl.delivery

import io.github.texport.superkassa.core.domain.impl.logging.getLogger
import io.github.texport.superkassa.core.presentation.api.DeliveryApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration

/**
 * Досылка чеков покупателям, пока касса открыта.
 *
 * Идёт своей корутиной, отдельно от чеков: медленный провайдер держит
 * только её. Замка писателя кассы она не берёт — БФД не трогает, а
 * задачу, за которую взялись одновременно фон и ручной повтор, отдаёт
 * одному из них хранилище.
 *
 * Задачи, оставшиеся от прошлого запуска, досылаются первым же заходом.
 */
internal class DeliverySender(
    private val delivery: DeliveryApi,
    private val interval: Duration,
    private val batchSize: Int,
    dispatcher: CoroutineDispatcher
) {
    private val logger = getLogger(DeliverySender::class)
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var loop: Job? = null

    fun start() {
        loop = scope.launch {
            while (isActive) {
                sendOnce()
                delay(interval)
            }
        }
    }

    /** Ждёт конца текущего захода: база закрывается только после него. */
    fun stop() = runBlocking { loop?.cancelAndJoin() }

    /**
     * Досылает всё, чей срок наступил, заходами по [batchSize].
     *
     * @return сколько задач отправлено, с успехом или отказом.
     */
    fun sendOnce(): Int {
        var sent = 0
        while (true) {
            val batch = runCatching { delivery.sendDueDeliveries(batchSize) }.getOrElse { failure ->
                logger.warn("Receipt delivery pass failed: {}", failure::class.simpleName)
                0
            }
            sent += batch
            if (batch < batchSize) return sent
        }
    }
}
