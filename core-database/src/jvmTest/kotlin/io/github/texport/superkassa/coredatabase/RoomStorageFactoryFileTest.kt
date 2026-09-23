package io.github.texport.superkassa.coredatabase

import androidx.room.Room
import androidx.sqlite.execSQL
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmMode
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmState
import io.github.texport.superkassa.coredatabase.api.RoomStorageFactory
import io.github.texport.superkassa.coredatabase.api.StorageOpenException
import io.github.texport.superkassa.coredatabase.api.openRoomStorage
import io.github.texport.superkassa.coredatabase.impl.db.SuperkassaAppDatabase
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * Промышленное открытие базы кассы: база всегда в файле, а файл, который
 * не открылся, остаётся как был — ни сноса, ни пустой базы, ни памяти.
 */
class RoomStorageFactoryFileTest {
    private val room = RoomFile()
    private val home: File = createTempDirectory("owner-home-").toFile()

    @AfterTest
    fun cleanUp() {
        room.delete()
        home.deleteRecursively()
    }

    @Test
    fun `путь со словом test — база в файле, и касса переживает перезапуск`() {
        val path = File(home, "Users/tester/superkassa-test.db").path
        RoomStorageFactory.createRoomStorage(path).storagePort.createKkm(kkm())

        val restarted = openRoomStorage(Room.databaseBuilder<SuperkassaAppDatabase>(name = path))
        try {
            assertNotNull(restarted.storagePort.findKkm(KKM), "cash register must survive a restart")
        } finally {
            restarted.close()
        }
    }

    @Test
    fun `файл не база — отказ, и файл не тронут`() {
        val garbage = "not an SQLite database ".repeat(200).toByteArray()
        File(room.path).writeBytes(garbage)

        assertFailsWith<StorageOpenException> { RoomStorageFactory.createRoomStorage(room.path) }
        assertContentEquals(garbage, File(room.path).readBytes())
    }

    @Test
    fun `схема новее приложения — отказ, и касса в базе остаётся`() {
        room.session { it.createKkm(kkm()) }
        room.sql { it.execSQL("PRAGMA user_version = 99") }

        assertFailsWith<StorageOpenException> { RoomStorageFactory.createRoomStorage(room.path) }
        room.sql { connection ->
            connection.prepare("SELECT COUNT(*) FROM kkms").use { statement ->
                statement.step()
                assertEquals(1L, statement.getLong(0))
            }
        }
    }

    private fun kkm() = KkmInfo(
        id = KKM,
        createdAt = 1L,
        updatedAt = 1L,
        mode = KkmMode.REGISTRATION.name,
        state = KkmState.ACTIVE.name,
        registrationNumber = "600300012345"
    )

    private companion object {
        const val KKM = "kkm-1"
    }
}
