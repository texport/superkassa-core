package io.github.texport.superkassa.coredatabase.impl.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Заводит счёт неверных пинов по кассам.
 *
 * Таблица пустая: у касс, открытых до этого шага, счёт начинается с нуля.
 */
internal val MIGRATION_PIN_ATTEMPTS = object : Migration(11, 12) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `pin_attempts` (" +
                "`kkmId` TEXT NOT NULL, `failures` INTEGER NOT NULL, `lockedUntil` INTEGER NOT NULL, " +
                "PRIMARY KEY(`kkmId`))"
        )
    }
}
