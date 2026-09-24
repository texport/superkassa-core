package io.github.texport.superkassa.coredatabase.impl.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryFailure
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryTask
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryTaskStatus
import io.github.texport.superkassa.core.string.api.TrilingualMessage

/**
 * Задача доставки чека покупателю по одному каналу.
 *
 * Причина отказа хранится кодом и тремя языками по колонкам: журнал
 * показывает её на языке кассира, не разбирая склеенную строку.
 */
@Entity(
    tableName = "delivery_tasks",
    indices = [Index(value = ["documentId"]), Index(value = ["status", "nextAttemptAt"])]
)
data class DeliveryTaskEntity(
    @PrimaryKey val id: String,
    val kkmId: String,
    val documentId: String,
    val channel: String,
    val destination: String?,
    val payloadType: String,
    val status: String,
    val attempts: Int,
    val nextAttemptAt: Long,
    val failureCode: String?,
    val failureRu: String?,
    val failureKk: String?,
    val failureEn: String?,
    val createdAt: Long,
    val updatedAt: Long
) {
    /** Получатель — персональные данные покупателя: строка записи его не несёт. */
    override fun toString(): String = "DeliveryTaskEntity(id=$id, status=$status, attempts=$attempts)"

    /** Задача ядра из записи. */
    fun toDomain(): DeliveryTask = DeliveryTask(
        id = id,
        kkmId = kkmId,
        documentId = documentId,
        channel = channel,
        destination = destination,
        payloadType = payloadType,
        status = DeliveryTaskStatus.valueOf(status),
        attempts = attempts,
        nextAttemptAt = nextAttemptAt,
        failure = failureCode?.let {
            DeliveryFailure(it, TrilingualMessage(failureRu.orEmpty(), failureKk.orEmpty(), failureEn.orEmpty()))
        },
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    /** Запись из задачи ядра. */
    companion object {
        /** Запись задачи [task]. */
        fun fromDomain(task: DeliveryTask): DeliveryTaskEntity = DeliveryTaskEntity(
            id = task.id,
            kkmId = task.kkmId,
            documentId = task.documentId,
            channel = task.channel,
            destination = task.destination,
            payloadType = task.payloadType,
            status = task.status.name,
            attempts = task.attempts,
            nextAttemptAt = task.nextAttemptAt,
            failureCode = task.failure?.code,
            failureRu = task.failure?.message?.ru,
            failureKk = task.failure?.message?.kk,
            failureEn = task.failure?.message?.en,
            createdAt = task.createdAt,
            updatedAt = task.updatedAt
        )
    }
}
