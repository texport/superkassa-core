package io.github.texport.superkassa.coredatabase

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import io.github.texport.superkassa.core.domain.api.model.auth.PinAttempts
import io.github.texport.superkassa.core.domain.api.port.integration.PinAttemptsPort
import io.github.texport.superkassa.coredatabase.api.openRoomStorage
import io.github.texport.superkassa.coredatabase.impl.db.SuperkassaAppDatabase
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * База версии 11 открывается строгим открытием: шаг 11 → 12 заводит счёт
 * неверных пинов, записанное остаётся, а счёт хранится между открытиями.
 */
class PinAttemptsMigrationTest {
    private val dir: File = createTempDirectory("room-pin-attempts-").toFile()
    private val path = File(dir, "kassa.db").absolutePath

    @AfterTest
    fun cleanUp() {
        dir.deleteRecursively()
    }

    @Test
    fun `база одиннадцатой версии получает счёт пинов и хранит его между открытиями`() {
        openRoomStorage(Room.databaseBuilder<SuperkassaAppDatabase>(name = path)).close()
        downgradeToVersion11()

        val first = openRoomStorage(Room.databaseBuilder<SuperkassaAppDatabase>(name = path))
        assertEquals("kept", first.storagePort.findKkm("kkm-old")?.name)
        (first.storagePort as PinAttemptsPort).getAndUpdate("kkm-old") { PinAttempts(5, 1_000L) }
        first.close()

        val second = openRoomStorage(Room.databaseBuilder<SuperkassaAppDatabase>(name = path))
        try {
            val stored = (second.storagePort as PinAttemptsPort).getAndUpdate("kkm-old") { it }
            assertEquals(PinAttempts(5, 1_000L), stored)
        } finally {
            second.close()
        }
    }

    /** Схема, какой её оставила версия 11: без таблицы счёта пинов. */
    private fun downgradeToVersion11() {
        val connection = BundledSQLiteDriver().open(path)
        try {
            connection.execSQL(
                "INSERT INTO kkms (id, state, mode, autoCloseShift, autoCashout, createdAt, updatedAt, name) " +
                    "VALUES ('kkm-old', 'ACTIVE', 'REGISTRATION', 0, 0, 1, 1, 'kept')"
            )
            connection.execSQL("DROP TABLE pin_attempts")
            connection.execSQL("PRAGMA user_version = 11")
        } finally {
            connection.close()
        }
    }
}
