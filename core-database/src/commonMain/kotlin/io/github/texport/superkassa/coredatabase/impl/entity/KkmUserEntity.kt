package io.github.texport.superkassa.coredatabase.impl.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сущность Room для хранения пользователей ККМ (кассиры, администраторы) и хэшей ПИН-кодов.
 *
 * Сам пин не хранится: по хешу вход проверяется, а показать пин
 * не нужно никому.
 */
@Entity(tableName = "kkm_users")
data class KkmUserEntity(
    @PrimaryKey val id: String,
    val kkmId: String,
    val name: String,
    val role: String,
    val pinHash: String,
    val createdAt: Long
)
