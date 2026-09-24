package io.github.texport.superkassa.coredatabase.impl.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Заводит задачи доставки чека покупателю.
 *
 * Таблица пустая: чеки, пробитые до этого шага, доставлялись сразу,
 * и досылать по ним нечего.
 */
internal val MIGRATION_DELIVERY_TASKS = object : Migration(13, 14) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `delivery_tasks` (`id` TEXT NOT NULL, `kkmId` TEXT NOT NULL, " +
                "`documentId` TEXT NOT NULL, `channel` TEXT NOT NULL, `destination` TEXT, `payloadType` TEXT NOT NULL, " +
                "`status` TEXT NOT NULL, `attempts` INTEGER NOT NULL, `nextAttemptAt` INTEGER NOT NULL, " +
                "`failureCode` TEXT, `failureRu` TEXT, `failureKk` TEXT, `failureEn` TEXT, " +
                "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_delivery_tasks_documentId` ON `delivery_tasks` (`documentId`)"
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_delivery_tasks_status_nextAttemptAt` ON `delivery_tasks` (`status`, `nextAttemptAt`)"
        )
    }
}
