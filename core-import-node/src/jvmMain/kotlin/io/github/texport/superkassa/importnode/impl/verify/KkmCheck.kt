package io.github.texport.superkassa.importnode.impl.verify

import io.github.texport.superkassa.core.domain.api.model.common.CounterKeyFormats
import io.github.texport.superkassa.core.domain.api.model.common.CounterScopes
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftStatus
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.coredatabase.api.StoredCounter
import io.github.texport.superkassa.coredatabase.api.StoredDocument
import io.github.texport.superkassa.importnode.api.KkmImportReport
import io.github.texport.superkassa.importnode.impl.node.NodeData

/**
 * Сверка одной кассы: касса, смены, документы и их чеки, счётчики,
 * очередь, кассиры и ключи повтора — как их отдаёт порт хранилища кассы.
 *
 * Кассир ищется по хешу пина: так касса пускает его на смену, и совпавший
 * хеш значит, что прежний пин подойдёт.
 */
internal class KkmCheck(private val kkm: KkmInfo, private val data: NodeData, private val port: StoragePort) {

    private val id = kkm.id
    private val users = data.snapshot.users.filter { it.kkmId == id }
    private val shifts = data.snapshot.shifts.filter { it.kkmId == id }
    private val documents = data.snapshot.documents.filter { it.snapshot.cashboxId == id }
    private val counters = data.snapshot.counters.filter { it.kkmId == id }
    private val queue = data.snapshot.queue.filter { it.cashboxId == id }
    private val keys = data.snapshot.idempotencyKeys.filter { it.kkmId == id }
    private val openShift = shifts.singleOrNull { it.status == ShiftStatus.OPEN }

    fun verify(): KkmImportReport {
        same("cash register $id", port.findKkm(id), kkm)
        same("shifts of $id", port.listShifts(id, ALL, 0).toSet(), shifts.toSet())
        same("open shift of $id", port.findOpenShift(id), openShift)
        verifyDocuments()
        same("counters of $id", storedCounters(), counters.toSet())
        val lanes = queue.map { it.lane }.toSet()
        same("queue of $id", lanes.flatMap { port.listQueueTasksByCashbox(id, it, ALL, 0) }.toSet(), queue.toSet())
        same("cashiers of $id", port.listUsers(id).size, users.size)
        users.forEach { same("cashier ${it.id}", port.findUserByPin(id, it.pinHash)?.id, it.id) }
        keys.forEach { same("idempotency key of $id", port.findIdempotencyResponse(id, it.key), it.responseRef) }
        return report()
    }

    private fun storedCounters(): Set<StoredCounter> =
        port.listCounters(id).mapTo(mutableSetOf()) { StoredCounter(id, it.scope, it.shiftId, it.key, it.value) }

    private fun verifyDocuments() {
        val stored = port.listFiscalDocumentsByPeriod(id, 0, Long.MAX_VALUE, ALL, 0)
        same("documents of $id", stored.toSet(), documents.map(StoredDocument::snapshot).toSet())
        documents.forEach { document ->
            val receipt = document.receipt ?: return@forEach
            val restored = port.findFiscalDocumentWithReceiptPayload(document.snapshot.id)?.second
            same(
                "receipt of ${document.snapshot.id}",
                restored,
                receipt.toReceiptRequest().copy(idempotencyKey = receipt.idempotencyKey)
            )
        }
    }

    private fun report(): KkmImportReport {
        val x = openShift?.let { XReportCheck(port).compare(it, shiftCounters(it.id)) }
        return KkmImportReport(
            kkmId = id,
            documents = documents.size,
            lastDocumentNumber = documents.mapNotNull { it.snapshot.docNo }.maxOrNull(),
            lastPrintedDocumentNumber = documents.mapNotNull { it.snapshot.printedDocumentNumber }.maxOrNull(),
            shifts = shifts.size,
            openShiftNumber = openShift?.shiftNo,
            openShiftCashTiyn = openShift?.let(::cashOf),
            counters = counters.size,
            queueByStatus = queue.groupingBy { it.status }.eachCount(),
            users = users.size,
            idempotencyKeys = keys.size,
            xReportMatches = x?.stored,
            recalculationMatches = x?.recalculated
        )
    }

    /** Наличные открытой смены: из них касса продолжит считать ящик. */
    private fun cashOf(shift: ShiftInfo): Long? =
        port.loadCounters(id, CounterScopes.SHIFT, shift.id)[CounterKeyFormats.CASH_SUM]

    /** Счётчики смены так, как они лежали у узла. */
    private fun shiftCounters(shiftId: String): Map<String, Long> = counters
        .filter { it.scope == CounterScopes.SHIFT && it.shiftId == shiftId }
        .associate { it.key to it.value }

    private companion object {
        const val ALL = Int.MAX_VALUE
    }
}
