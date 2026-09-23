package io.github.texport.superkassa.embedded.impl.shift

import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.logging.getLogger
import io.github.texport.superkassa.core.presentation.api.SuperkassaApi
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
 * Автозакрытие смен по часам, пока касса открыта.
 *
 * Каждый заход спрашивает ядро о каждой кассе каталога: пора ли закрыть
 * её смену ([SuperkassaApi.autoCloseShift]). Когда пора и что делать
 * при отказе БФД, решает ядро; здесь только часы. Отказ одной кассы
 * не останавливает остальные и следующий заход.
 */
internal class ShiftAutoCloser(
    private val storage: StoragePort,
    private val api: SuperkassaApi,
    private val interval: Duration,
    dispatcher: CoroutineDispatcher
) {
    private val logger = getLogger(ShiftAutoCloser::class)
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var loop: Job? = null

    fun start() {
        loop = scope.launch {
            while (isActive) {
                closeDueOnce()
                delay(interval)
            }
        }
    }

    /** Ждёт конца текущего захода: база закрывается только после него. */
    fun stop() = runBlocking { loop?.cancelAndJoin() }

    /**
     * Один заход по всем кассам.
     *
     * @return сколько смен закрыто или отправлено на закрытие.
     */
    fun closeDueOnce(): Int {
        var closed = 0
        var offset = 0
        while (true) {
            val page = storage.listKkms(limit = PAGE, offset = offset)
            if (page.isEmpty()) return closed
            page.forEach { kkm -> if (closeDue(kkm.id)) closed++ }
            offset += PAGE
        }
    }

    private fun closeDue(kkmId: String): Boolean = try {
        api.autoCloseShift(kkmId) != null
    } catch (e: Exception) {
        logger.warn("Automatic shift close failed for cashbox {}: {}", kkmId, e::class.simpleName)
        false
    }

    private companion object {
        const val PAGE = 100
    }
}
