package io.github.texport.superkassa.coredatabase.impl.adapter

import kotlinx.serialization.json.Json

/**
 * Формат хранения чека рядом с документом.
 *
 * Один на запись чека кассой и на перенос готовых записей: иначе чек,
 * положенный переносом, читался бы по другим правилам, чем свой.
 * Неизвестные ключи не роняют разбор старых записей.
 */
internal val StoredReceiptJson: Json = Json { ignoreUnknownKeys = true }

/** Ключ счётчика в таблице: касса, область, смена и имя через двоеточие. */
internal fun counterKey(kkmId: String, scope: String, shiftId: String?, key: String): String =
    "$kkmId:$scope:${shiftId ?: ""}:$key"
