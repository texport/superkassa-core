package io.github.texport.superkassa.coredatabase.impl.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Заводит таблицу ключей повтора фискальных операций и ссылку на чек у документа.
 *
 * Без таблицы повтор чека с тем же ключом пробивал второй чек: хранилище
 * отвечало «ключа не было» на любой вопрос. Без ссылки чек, распечатанный
 * позже, выходил без адреса проверки в ОФД. Тот же шаг приводит документы,
 * записанные прежним Room, к записи узла: см. [convertLegacyLedger].
 */
internal val MIGRATION_IDEMPOTENCY_AND_RECEIPT_URL = object : Migration(9, 10) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `idempotency_keys` (" +
                "`kkmId` TEXT NOT NULL, `idempotencyKey` TEXT NOT NULL, `operation` TEXT NOT NULL, " +
                "`responseRef` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`kkmId`, `idempotencyKey`))"
        )
        connection.execSQL("ALTER TABLE fiscal_documents ADD COLUMN receiptUrl TEXT")
        convertLegacyLedger(connection)
    }
}
