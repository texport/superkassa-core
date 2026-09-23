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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * База версии 9 открывается строгим открытием: шаг 9 → 10 заводит ключи
 * повтора и ссылку на чек, Room принимает схему, а записанное остаётся.
 */
class IdempotencyAndReceiptUrlMigrationTest {
    private val dir: File = createTempDirectory("room-migration-").toFile()
    private val path = File(dir, "kassa.db").absolutePath

    @AfterTest
    fun cleanUp() {
        dir.deleteRecursively()
    }

    @Test
    fun `база девятой версии поднимается до нынешней без потери записей`() {
        openRoomStorage(Room.databaseBuilder<SuperkassaAppDatabase>(name = path)).close()
        downgradeToVersion9()

        val storage = openRoomStorage(Room.databaseBuilder<SuperkassaAppDatabase>(name = path))
        try {
            assertEquals("kept", storage.storagePort.findKkm("kkm-old")?.name)
            assertNull(storage.storagePort.findIdempotencyResponse("kkm-old", "key-1"))
            assertTrue(storage.storagePort.insertIdempotency("kkm-old", "key-1", "CREATE_RECEIPT"))
            assertFalse(storage.storagePort.insertIdempotency("kkm-old", "key-1", "CREATE_RECEIPT"))
        } finally {
            storage.close()
        }
    }

    /** Схема, какой её оставила версия 9: без таблицы ключей, ссылки на чек, оформления кассы и счёта пинов. */
    private fun downgradeToVersion9() {
        val connection = BundledSQLiteDriver().open(path)
        try {
            connection.execSQL(
                "INSERT INTO kkms (id, state, mode, autoCloseShift, autoCashout, createdAt, updatedAt, name) " +
                    "VALUES ('kkm-old', 'ACTIVE', 'REGISTRATION', 0, 0, 1, 1, 'kept')"
            )
            connection.execSQL("DROP TABLE idempotency_keys")
            connection.execSQL("ALTER TABLE fiscal_documents DROP COLUMN receiptUrl")
            connection.execSQL("ALTER TABLE kkms DROP COLUMN brandingJson")
            connection.execSQL("DROP TABLE pin_attempts")
            connection.execSQL("PRAGMA user_version = 9")
        } finally {
            connection.close()
        }
    }
}
