package io.github.texport.superkassa.core.domain.impl.usecase.queue

import io.github.texport.superkassa.core.domain.api.model.queue.OfflineQueueCommandRequest
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.api.port.internal.OfflineQueuePort

import io.github.texport.superkassa.core.domain.impl.logging.getLogger

class EnsureQueueConsistencyUseCase(
    private val storage: StoragePort,
    private val queue: OfflineQueuePort,
    private val clock: ClockPort? = null
) {
    private val logger = getLogger(EnsureQueueConsistencyUseCase::class)

    fun execute(kkmId: String): Int {
        logger.info("Executing queue consistency check for kkmId={}", kkmId)
        val activeTaskRefs = storage.listQueueTasksByCashbox(kkmId, "OFFLINE", limit = 1000)
            .filter { it.status == "PENDING" || it.status == "PROCESSING" || it.status == "FAILED" }
            .mapTo(HashSet()) { it.payloadRef }

        val pendingDocs = storage.listFiscalDocumentsByPeriod(
            kkmId = kkmId,
            fromInclusive = 0L,
            toExclusive = 4102444800000L,
            limit = 1000,
            offset = 0
        ).filter { (it.ofdStatus == "PENDING" || it.ofdStatus == "OFFLINE") && !activeTaskRefs.contains(it.id) }

        if (pendingDocs.isNotEmpty()) {
            logger.warn("Found {} pending documents not present in offline queue. Repairing queue...", pendingDocs.size)
        }

        var repaired = 0
        for (doc in pendingDocs) {
            val queueType = when (doc.docType) {
                "CHECK", "TICKET", "SALE", "RETURN", "BUY", "BUY_RETURN" -> "TICKET"
                "CLOSE_SHIFT", "SHIFT_CLOSE", "Z_REPORT", "REPORT_Z" -> "CLOSE_SHIFT"
                "CASH_IN", "CASH_OUT", "MONEY_PLACEMENT" -> "MONEY_PLACEMENT"
                "REPORT_X", "X_REPORT", "REPORT" -> "REPORT"
                else -> doc.docType
            }
            val ok = queue.enqueueOffline(
                OfflineQueueCommandRequest(
                    kkmId = kkmId,
                    type = queueType,
                    payloadRef = doc.id
                )
            )
            if (ok) repaired++
        }
        if (repaired > 0) {
            logger.info("Queue consistency check completed. Repaired {} items for kkmId={}", repaired, kkmId)
        } else {
            logger.debug("Queue consistency check completed. Queue is consistent for kkmId={}", kkmId)
        }
        return repaired
    }
}
