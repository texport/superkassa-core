package io.github.texport.superkassa.coredatabase.impl.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.texport.superkassa.offlinequeue.api.model.QueueCommand
import io.github.texport.superkassa.offlinequeue.api.model.QueueCommandType
import io.github.texport.superkassa.offlinequeue.api.model.QueueLane
import io.github.texport.superkassa.offlinequeue.api.model.QueueStatus

/**
 * Сущность Room для хранения команд офлайн-очереди.
 */
@Entity(tableName = "queue_commands")
data class QueueCommandEntity(
    @PrimaryKey val id: String,
    val cashboxId: String,
    val lane: String,
    val type: String,
    val payloadRef: String,
    val status: String,
    val attempt: Int,
    val lastError: String?,
    val nextAttemptAt: Long?,
    val createdAt: Long,
    val lastErrorCode: Int? = null
) {
    fun toDomain(): QueueCommand {
        return QueueCommand(
            id = id,
            cashboxId = cashboxId,
            lane = QueueLane.valueOf(lane),
            type = QueueCommandType.valueOf(type),
            payloadRef = payloadRef,
            createdAt = createdAt,
            status = QueueStatus.valueOf(status),
            attempt = attempt,
            nextAttemptAt = nextAttemptAt,
            lastError = lastError,
            lastErrorCode = lastErrorCode
        )
    }

    companion object {
        fun fromDomain(domain: QueueCommand): QueueCommandEntity {
            return QueueCommandEntity(
                id = domain.id,
                cashboxId = domain.cashboxId,
                lane = domain.lane.name,
                type = domain.type.name,
                payloadRef = domain.payloadRef,
                status = domain.status.name,
                attempt = domain.attempt,
                lastError = domain.lastError,
                nextAttemptAt = domain.nextAttemptAt,
                createdAt = domain.createdAt,
                lastErrorCode = domain.lastErrorCode
            )
        }
    }
}
