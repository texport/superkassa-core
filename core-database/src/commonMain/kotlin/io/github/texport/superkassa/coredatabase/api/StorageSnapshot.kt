package io.github.texport.superkassa.coredatabase.api

import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.queue.QueueTask
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptStoredPayload
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo

/**
 * Всё, что касса хранит, одним набором — для переноса в пустую базу.
 *
 * Записи ложатся как есть: номера, типы и суммы документов, счётчики
 * и ключи повтора не пересчитываются. Порт хранилища для этого не годится:
 * он заводит документ заново — со своим номером и статусом «ждёт отправки».
 */
data class StorageSnapshot(
    val kkms: List<KkmInfo>,
    val users: List<StoredUser>,
    val shifts: List<ShiftInfo>,
    val documents: List<StoredDocument>,
    val counters: List<StoredCounter>,
    val queue: List<QueueTask>,
    val idempotencyKeys: List<StoredIdempotencyKey>
)

/** Кассир кассы; пин известен только хешем. */
data class StoredUser(
    val kkmId: String,
    val id: String,
    val name: String,
    val role: UserRole,
    val pinHash: String,
    val createdAt: Long
)

/** Фискальный документ и чек, из которого он собран; у отчётов и внесений чека нет. */
data class StoredDocument(
    val snapshot: FiscalDocumentSnapshot,
    val receipt: ReceiptStoredPayload?
)

/** Значение счётчика кассы: общего или сменного. */
data class StoredCounter(
    val kkmId: String,
    val scope: String,
    val shiftId: String?,
    val key: String,
    val value: Long
)

/** Ключ повтора фискальной операции; без [responseRef] операция не завершена. */
data class StoredIdempotencyKey(
    val kkmId: String,
    val key: String,
    val operation: String,
    val responseRef: String?,
    val createdAt: Long
)
