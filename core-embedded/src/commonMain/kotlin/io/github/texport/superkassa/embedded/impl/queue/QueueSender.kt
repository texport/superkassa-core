package io.github.texport.superkassa.embedded.impl.queue

import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.api.port.integration.inTransaction
import io.github.texport.superkassa.core.domain.impl.logging.getLogger
import io.github.texport.superkassa.core.presentation.api.OfflineQueueApi
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
 * Досылка автономной очереди в ОФД, пока касса открыта.
 *
 * Заход обходит все кассы каталога страницами, а не первые сто. Досылка
 * одной кассы идёт под тем же замком писателя, что и её чеки: документ
 * из очереди и новый чек не уходят в ОФД одновременно и не делят номер
 * запроса.
 */
internal class QueueSender(
    private val storage: StoragePort,
    private val queue: OfflineQueueApi,
    private val interval: Duration,
    private val batchSize: Int,
    dispatcher: CoroutineDispatcher
) {
    private val logger = getLogger(QueueSender::class)
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
     * Один заход по всем кассам; отказ одной кассы не останавливает остальные.
     *
     * @return сколько документов обработано: отправленных и тех, чья попытка сорвалась.
     */
    fun sendOnce(): Int {
        var sent = 0
        var offset = 0
        while (true) {
            val page = storage.listKkms(limit = PAGE, offset = offset)
            if (page.isEmpty()) return sent
            page.forEach { kkm -> sent += sendKkm(kkm.id) }
            offset += PAGE
        }
    }

    private fun sendKkm(kkmId: String): Int = try {
        val processed = storage.inTransaction { queue.processOfflineBatch(kkmId, batchSize) }
        if (processed > 0) logger.info("Offline queue processed {} documents for cashbox {}", processed, kkmId)
        processed
    } catch (e: Exception) {
        logger.warn("Offline queue pass failed for cashbox {}: {}", kkmId, e::class.simpleName)
        0
    }

    private companion object {
        const val PAGE = 100
    }
}
