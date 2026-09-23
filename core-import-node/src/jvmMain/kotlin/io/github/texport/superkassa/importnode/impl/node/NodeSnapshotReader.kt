package io.github.texport.superkassa.importnode.impl.node

import io.github.texport.superkassa.coredatabase.api.StorageSnapshot
import io.github.texport.superkassa.importnode.api.NodeImportException

/**
 * Что прочитано у узла: записи для базы кассы и то, чему в ней места нет.
 *
 * @property notTransferred непустые таблицы и колонки узла, которые не переносятся: имя → число строк.
 */
internal class NodeData(
    val snapshot: StorageSnapshot,
    val notTransferred: Map<String, Long>
)

/**
 * Читает базу узла целиком.
 *
 * Таблицы, которых у кассы нет, делятся на две группы. Журнал, сообщения
 * ОФД, старая очередь и исходящие события несут фискальные следы, и если
 * в них что-то есть — перенос отказывает: молча бросить их нельзя. Нынешний
 * узел в них не пишет. Справочник товаров, старые операторы, журнал ошибок
 * и аренды блокировок фискального не несут и называются в отчёте.
 */
internal object NodeSnapshotReader {

    private val FISCAL_TABLES = listOf("fiscal_journal", "ofd_message", "offline_queue", "outbox_event")
    private val OTHER_TABLES = listOf("nomenclature_item", "kkm_operator", "error_log", "cashbox_lock", "queue_lock")
    private const val OFD_ADDRESS = "cashbox.ofd_host"

    fun read(db: NodeDatabase): NodeData {
        requireNoFiscalLeftovers(db)
        val kkms = db.rows(KkmRows.SQL, KkmRows::kkm)
        val documents = DocumentRows(kkms.associateBy { it.id })
        val snapshot = StorageSnapshot(
            kkms = kkms,
            users = db.rows(LedgerRows.USERS, LedgerRows::user),
            shifts = db.rows(LedgerRows.SHIFTS, LedgerRows::shift),
            documents = db.rows(DocumentRows.SQL, documents::document),
            counters = db.rows(LedgerRows.COUNTERS, LedgerRows::counter),
            queue = db.rows(LedgerRows.QUEUE, LedgerRows::queueTask),
            idempotencyKeys = db.rows(LedgerRows.IDEMPOTENCY, LedgerRows::idempotencyKey)
        )
        return NodeData(snapshot, notTransferred(db))
    }

    private fun requireNoFiscalLeftovers(db: NodeDatabase) {
        val leftovers = FISCAL_TABLES.associateWith(db::count).filterValues { it > 0 }
        if (leftovers.isNotEmpty()) {
            throw NodeImportException("Node tables $leftovers hold records the cash register database has no place for")
        }
    }

    /** Адрес ОФД кассы узел больше не читает, но старые записи могут его хранить. */
    private fun notTransferred(db: NodeDatabase): Map<String, Long> {
        val tables = OTHER_TABLES.associateWith(db::count)
        val ofdAddress = db.rows(
            "SELECT COUNT(*) AS n FROM cashbox WHERE ofd_host IS NOT NULL"
        ) { it.long("n") }.single()
        return (tables + (OFD_ADDRESS to ofdAddress)).filterValues { it > 0 }
    }
}
