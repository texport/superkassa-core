package io.github.texport.superkassa.coredatabase

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import io.github.texport.superkassa.coredatabase.api.openRoomStorage
import io.github.texport.superkassa.coredatabase.impl.db.SuperkassaAppDatabase
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * База версии 12 открывается строгим открытием: шаг 12 → 13 переименовывает
 * колонки БИН/ИИН и ОКЭД, а записанные в них сведения кассы остаются.
 */
class KkmTaxpayerColumnsMigrationTest {
    private val dir: File = createTempDirectory("room-taxpayer-").toFile()
    private val path = File(dir, "kassa.db").absolutePath

    @AfterTest
    fun cleanUp() {
        dir.deleteRecursively()
    }

    @Test
    fun `БИН и ОКЭД кассы из базы двенадцатой версии остаются на месте`() {
        openRoomStorage(Room.databaseBuilder<SuperkassaAppDatabase>(name = path)).close()
        downgradeToVersion12()

        val storage = openRoomStorage(Room.databaseBuilder<SuperkassaAppDatabase>(name = path))
        try {
            val service = storage.storagePort.findKkm("kkm-old")?.ofdServiceInfo
            assertEquals("060140012345" to "47111", service?.orgIinOrBin to service?.orgOked)
        } finally {
            storage.close()
        }
    }

    /** Схема, какой её оставила версия 12: колонки налогоплательщика под прежними именами. */
    private fun downgradeToVersion12() {
        val connection = BundledSQLiteDriver().open(path)
        try {
            connection.execSQL("ALTER TABLE kkms RENAME COLUMN orgIinOrBin TO orgInn")
            connection.execSQL("ALTER TABLE kkms RENAME COLUMN orgOked TO orgOkved")
            connection.execSQL(
                "INSERT INTO kkms (id, state, mode, autoCloseShift, autoCashout, createdAt, updatedAt, " +
                    "orgTitle, orgInn, orgOkved) " +
                    "VALUES ('kkm-old', 'ACTIVE', 'REGISTRATION', 0, 0, 1, 1, 'ТОО Дала', '060140012345', '47111')"
            )
            connection.execSQL("PRAGMA user_version = 12")
        } finally {
            connection.close()
        }
    }
}
