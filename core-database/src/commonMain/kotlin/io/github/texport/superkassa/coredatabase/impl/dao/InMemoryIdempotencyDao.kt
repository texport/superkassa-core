package io.github.texport.superkassa.coredatabase.impl.dao

import io.github.texport.superkassa.coredatabase.impl.entity.IdempotencyEntity

/**
 * Ключи повтора в памяти — для хранилища тестов без диска.
 *
 * Отвечает так же, как таблица: заведённый ключ второй раз не заводится.
 */
internal class InMemoryIdempotencyDao : IdempotencyDao {
    private val keys = mutableMapOf<Pair<String, String>, IdempotencyEntity>()

    override suspend fun insert(entity: IdempotencyEntity): Long {
        val id = entity.kkmId to entity.idempotencyKey
        if (id in keys) return -1L
        keys[id] = entity
        return keys.size.toLong()
    }

    override suspend fun find(kkmId: String, key: String): IdempotencyEntity? = keys[kkmId to key]

    override suspend fun complete(kkmId: String, key: String, responseRef: String?): Int {
        val current = keys[kkmId to key] ?: return 0
        keys[kkmId to key] = current.copy(responseRef = responseRef)
        return 1
    }

    override suspend fun forgetUnfinished(kkmId: String, key: String) {
        if (keys[kkmId to key]?.responseRef == null) keys.remove(kkmId to key)
    }

    override suspend fun deleteByKkm(kkmId: String) {
        keys.keys.removeAll { it.first == kkmId }
    }
}
