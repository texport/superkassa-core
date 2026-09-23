package io.github.texport.superkassa.coredatabase.impl.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Неверные пины кассы подряд и срок её блокировки.
 *
 * Лежит в базе, а не в памяти: иначе перезапуск приложения снимал бы
 * блокировку, и перебор пина шёл бы от перезапуска к перезапуску.
 */
@Entity(tableName = "pin_attempts")
data class PinAttemptEntity(
    @PrimaryKey val kkmId: String,
    val failures: Int,
    val lockedUntil: Long
)
