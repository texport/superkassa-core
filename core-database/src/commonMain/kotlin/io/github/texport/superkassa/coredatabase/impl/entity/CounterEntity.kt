package io.github.texport.superkassa.coredatabase.impl.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сущность Room для хранения фискальных и сменных счетчиков ККМ.
 */
@Entity(tableName = "counters")
data class CounterEntity(
    @PrimaryKey val key: String,
    val value: Long
)
