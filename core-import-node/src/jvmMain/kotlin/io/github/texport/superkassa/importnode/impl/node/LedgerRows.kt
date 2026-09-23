package io.github.texport.superkassa.importnode.impl.node

import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.model.queue.QueueTask
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftStatus
import io.github.texport.superkassa.coredatabase.api.StoredCounter
import io.github.texport.superkassa.coredatabase.api.StoredIdempotencyKey
import io.github.texport.superkassa.coredatabase.api.StoredUser

/**
 * Смены, счётчики, очередь, кассиры и ключи повтора узла — как есть.
 *
 * Состояние смены и роль кассира узел хранит строкой; незнакомое значение —
 * отказ переноса, а не подстановка: смена «не та» и роль «не та» меняют,
 * что касса разрешит кассиру.
 */
internal object LedgerRows {

    const val SHIFTS = "SELECT * FROM shift ORDER BY cashbox_id, shift_no, id"
    const val COUNTERS = "SELECT * FROM counter ORDER BY cashbox_id, scope, shift_id, counter_key"
    const val QUEUE = "SELECT * FROM queue_task ORDER BY cashbox_id, created_at, id"
    const val USERS = "SELECT * FROM kkm_user ORDER BY cashbox_id, created_at, id"
    const val IDEMPOTENCY = "SELECT * FROM idempotency ORDER BY cashbox_id, created_at, idempotency_key"

    fun shift(row: NodeRow) = ShiftInfo(
        id = row.text("id"),
        kkmId = row.text("cashbox_id"),
        shiftNo = row.long("shift_no"),
        status = ShiftStatus.valueOf(row.text("status")),
        openedAt = row.long("opened_at"),
        closedAt = row.longOrNull("closed_at"),
        openDocumentId = row.textOrNull("open_document_id"),
        closeDocumentId = row.textOrNull("close_document_id")
    )

    fun counter(row: NodeRow) = StoredCounter(
        kkmId = row.text("cashbox_id"),
        scope = row.text("scope"),
        shiftId = row.textOrNull("shift_id"),
        key = row.text("counter_key"),
        value = row.long("value")
    )

    fun queueTask(row: NodeRow) = QueueTask(
        id = row.text("id"),
        cashboxId = row.text("cashbox_id"),
        lane = row.text("lane"),
        type = row.text("type"),
        payloadRef = row.text("payload_ref"),
        createdAt = row.long("created_at"),
        status = row.text("status"),
        attempt = Math.toIntExact(row.long("attempt")),
        nextAttemptAt = row.longOrNull("next_attempt_at"),
        lastError = row.textOrNull("last_error")
    )

    fun user(row: NodeRow) = StoredUser(
        kkmId = row.text("cashbox_id"),
        id = row.text("id"),
        name = row.text("name"),
        role = UserRole.valueOf(row.text("role")),
        pinHash = row.text("pin_hash"),
        createdAt = row.long("created_at")
    )

    fun idempotencyKey(row: NodeRow) = StoredIdempotencyKey(
        kkmId = row.text("cashbox_id"),
        key = row.text("idempotency_key"),
        operation = row.text("operation"),
        responseRef = row.textOrNull("response_ref"),
        createdAt = row.long("created_at")
    )
}
