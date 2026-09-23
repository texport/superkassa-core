package io.github.texport.superkassa.coredatabase

import androidx.room.Room
import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.queue.QueueTask
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.coredatabase.api.StorageSnapshot
import io.github.texport.superkassa.coredatabase.api.StoredCounter
import io.github.texport.superkassa.coredatabase.api.StoredDocument
import io.github.texport.superkassa.coredatabase.api.StoredIdempotencyKey
import io.github.texport.superkassa.coredatabase.api.openRoomStorage
import io.github.texport.superkassa.coredatabase.api.restoreRoomStorage
import io.github.texport.superkassa.coredatabase.impl.db.SuperkassaAppDatabase
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Перенос кладёт записи как есть и одной транзакцией: документ без номера
 * остаётся без номера, итог — в тиынах, а сорвавшийся перенос не оставляет
 * в базе ничего.
 */
class RestoreRoomStorageTest {
    private val dir: File = createTempDirectory("room-restore-").toFile()
    private val path = File(dir, "kassa.db").absolutePath

    @AfterTest
    fun cleanUp() {
        dir.deleteRecursively()
    }

    @Test
    fun `документ ложится со своим типом, номером и итогом`() {
        restoreRoomStorage(builder(), snapshot())
        read { storage ->
            val document = storage.findFiscalDocumentById("d-1")
            assertEquals("SALE" to 150_050L, document?.docType to document?.totalAmount)
            assertNull(document?.docNo)
            assertEquals(3L, storage.loadCounters("kkm", "GLOBAL")["ofd.req_num"])
            assertEquals("d-1", storage.findIdempotencyResponse("kkm", "k-1"))
        }
    }

    @Test
    fun `в базу с кассой перенос не пишет`() {
        restoreRoomStorage(builder(), snapshot())
        assertFailsWith<IllegalStateException> { restoreRoomStorage(builder(), snapshot().copy(queue = emptyList())) }
    }

    @Test
    fun `сорвавшийся на середине перенос не оставляет ничего`() {
        val key = StoredIdempotencyKey("kkm", "k-1", "CREATE_RECEIPT", null, 1)
        val broken = snapshot().copy(idempotencyKeys = listOf(key, key))
        assertFailsWith<IllegalStateException> { restoreRoomStorage(builder(), broken) }
        read { storage -> assertEquals(0, storage.countKkms(null, null)) }
    }

    @Test
    fun `задача очереди незнакомой дорожки отказывает до записи`() {
        val task = QueueTask("q", "kkm", "ONLINE", "TICKET", "d-1", 1, "PENDING", 0, null, null)
        assertFailsWith<IllegalArgumentException> { restoreRoomStorage(builder(), snapshot().copy(queue = listOf(task))) }
    }

    @Test
    fun `счётчик с двоеточием в ключе отказывает до записи`() {
        val counter = StoredCounter("kkm", "SHIFT", "s:1", "cash.sum", 1)
        assertFailsWith<IllegalArgumentException> { restoreRoomStorage(builder(), snapshot().copy(counters = listOf(counter))) }
    }

    private fun builder() = Room.databaseBuilder<SuperkassaAppDatabase>(name = path)

    private fun read(check: (StoragePort) -> Unit) {
        val storage = openRoomStorage(builder())
        try {
            check(storage.storagePort)
        } finally {
            storage.close()
        }
    }

    private fun snapshot() = StorageSnapshot(
        kkms = listOf(KkmInfo(id = "kkm", createdAt = 1, updatedAt = 1, mode = "REGISTRATION", state = "ACTIVE")),
        users = emptyList(),
        shifts = emptyList(),
        documents = listOf(StoredDocument(document(), receipt = null)),
        counters = listOf(StoredCounter("kkm", "GLOBAL", null, "ofd.req_num", 3)),
        queue = listOf(QueueTask("q", "kkm", "OFFLINE", "TICKET", "d-1", 1, "PENDING", 0, null, null)),
        idempotencyKeys = listOf(StoredIdempotencyKey("kkm", "k-1", "CREATE_RECEIPT", "d-1", 1))
    )

    private fun document() = FiscalDocumentSnapshot(
        id = "d-1", cashboxId = "kkm", shiftId = "s-1", docType = "SALE", docNo = null, shiftNo = 1, createdAt = 1,
        totalAmount = 150_050L, currency = "KZT", fiscalSign = null, autonomousSign = null, isAutonomous = false,
        ofdStatus = "PENDING", deliveredAt = null
    )
}
