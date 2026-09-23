package io.github.texport.superkassa.coredatabase.impl.adapter

import io.github.texport.superkassa.core.domain.api.model.auth.KkmUser
import io.github.texport.superkassa.core.domain.api.model.auth.PinHashes
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.coredatabase.impl.dao.KkmDao
import io.github.texport.superkassa.coredatabase.impl.dao.KkmUserDao
import io.github.texport.superkassa.coredatabase.impl.entity.KkmEntity
import io.github.texport.superkassa.coredatabase.impl.entity.KkmUserEntity
import kotlinx.coroutines.runBlocking

/** Кассы и их кассиры. */
internal class RoomKkms(private val kkmDao: KkmDao, private val userDao: KkmUserDao) {

    fun save(info: KkmInfo): Boolean = runBlocking {
        kkmDao.insert(KkmEntity.fromDomain(info))
        true
    }

    fun find(id: String): KkmInfo? = runBlocking { kkmDao.getById(id)?.toDomain() }

    fun byRegistrationNumber(number: String): KkmInfo? = runBlocking {
        kkmDao.getByRegistrationNumber(number)?.toDomain()
    }

    /** По номеру кассы в ОФД, а не по своему идентификатору: так узнаётся её повторная регистрация. */
    fun bySystemId(systemId: String): KkmInfo? = runBlocking { kkmDao.getBySystemId(systemId)?.toDomain() }

    fun list(limit: Int, offset: Int): List<KkmInfo> = runBlocking { kkmDao.list(limit, offset).map { it.toDomain() } }

    fun count(): Int = runBlocking { kkmDao.list(Int.MAX_VALUE, 0).size }

    fun delete(id: String): Boolean = runBlocking {
        kkmDao.deleteById(id)
        true
    }

    fun deleteWithUsers(kkmId: String) = runBlocking {
        kkmDao.deleteById(kkmId)
        userDao.deleteByKkm(kkmId)
    }

    fun updateToken(id: String, tokenEncryptedBase64: String, updatedAt: Long): Boolean = runBlocking {
        val current = kkmDao.getById(id) ?: return@runBlocking false
        kkmDao.insert(
            current.copy(tokenEncryptedBase64 = tokenEncryptedBase64, tokenUpdatedAt = updatedAt, updatedAt = updatedAt)
        )
        true
    }

    /** Пин занят другим кассиром этой кассы — отказ, как у узла с его уникальным индексом. */
    fun createUser(kkmId: String, userId: String, name: String, role: UserRole, pinHash: String, at: Long): Boolean =
        runBlocking {
            if (pinTakenByOther(kkmId, userId, pinHash)) return@runBlocking false
            userDao.insert(KkmUserEntity(userId, kkmId, name, role.name, pinHash, at))
            true
        }

    fun updateUser(kkmId: String, userId: String, name: String?, role: UserRole?, pinHash: String?): Boolean =
        runBlocking {
            val current = userOf(kkmId, userId) ?: return@runBlocking false
            if (pinHash != null && pinTakenByOther(kkmId, userId, pinHash)) return@runBlocking false
            val updated = current.copy(
                name = name ?: current.name,
                role = role?.name ?: current.role,
                pinHash = pinHash ?: current.pinHash
            )
            userDao.insert(updated)
            true
        }

    fun deleteUser(kkmId: String, userId: String): Boolean = runBlocking {
        if (userOf(kkmId, userId) == null) return@runBlocking false
        userDao.deleteById(userId)
        true
    }

    fun users(kkmId: String): List<KkmUser> = runBlocking { userDao.listByKkm(kkmId).map { it.toUser() } }

    fun userById(kkmId: String, userId: String): KkmUser? = runBlocking { userOf(kkmId, userId)?.toUser() }

    /**
     * Кассир по хешу пина: сравниваются все кассиры кассы и каждый — за
     * постоянное время, чтобы по времени ответа не угадывалось, кто и
     * насколько совпал.
     */
    fun userByPin(kkmId: String, pinHash: String): KkmUser? = runBlocking {
        var found: KkmUserEntity? = null
        for (user in userDao.listByKkm(kkmId)) {
            if (PinHashes.same(user.pinHash, pinHash) && found == null) found = user
        }
        found?.toUser()
    }

    private suspend fun pinTakenByOther(kkmId: String, userId: String, pinHash: String): Boolean =
        userDao.listByKkm(kkmId).any { it.id != userId && PinHashes.same(it.pinHash, pinHash) }

    /** Кассир этой кассы: чужой по идентификатору не находится, как у узла. */
    private suspend fun userOf(kkmId: String, userId: String): KkmUserEntity? =
        userDao.getById(userId)?.takeIf { it.kkmId == kkmId }

    private fun KkmUserEntity.toUser() =
        KkmUser(id = id, name = name, role = UserRole.valueOf(role), createdAt = createdAt)
}
