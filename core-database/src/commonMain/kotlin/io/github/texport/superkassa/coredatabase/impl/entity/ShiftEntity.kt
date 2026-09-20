package io.github.texport.superkassa.coredatabase.impl.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftStatus

/**
 * Сущность Room для хранения смен ККМ.
 */
@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey val id: String,
    val kkmId: String,
    val shiftNo: Long,
    val status: String,
    val openedAt: Long,
    val closedAt: Long?,
    val openDocumentId: String?,
    val closeDocumentId: String?
) {
    fun toDomain(): ShiftInfo {
        return ShiftInfo(
            id = id,
            kkmId = kkmId,
            shiftNo = shiftNo,
            status = ShiftStatus.valueOf(status),
            openedAt = openedAt,
            closedAt = closedAt,
            openDocumentId = openDocumentId,
            closeDocumentId = closeDocumentId
        )
    }

    companion object {
        fun fromDomain(domain: ShiftInfo): ShiftEntity {
            return ShiftEntity(
                id = domain.id,
                kkmId = domain.kkmId,
                shiftNo = domain.shiftNo,
                status = domain.status.name,
                openedAt = domain.openedAt,
                closedAt = domain.closedAt,
                openDocumentId = domain.openDocumentId,
                closeDocumentId = domain.closeDocumentId
            )
        }
    }
}
