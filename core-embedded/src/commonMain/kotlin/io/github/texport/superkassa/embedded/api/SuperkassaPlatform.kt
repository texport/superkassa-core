package io.github.texport.superkassa.embedded.api

import androidx.room.RoomDatabase
import io.github.texport.superkassa.core.domain.api.port.integration.DocumentConvertPort
import io.github.texport.superkassa.coredatabase.impl.db.SuperkassaAppDatabase

/**
 * Что касса берёт от платформы: каталог данных, базу, рисование документов и принтер.
 *
 * Создаётся приложением: на JVM — с каталогом данных, на Android — с `Context`.
 */
expect class SuperkassaPlatform {
    /** Каталог данных кассы: база, настройки, замок владельца. */
    internal val dataDir: String

    internal fun databaseBuilder(path: String): RoomDatabase.Builder<SuperkassaAppDatabase>

    internal fun documents(): DocumentConvertPort

    internal fun printer(): DocumentPrinter
}
