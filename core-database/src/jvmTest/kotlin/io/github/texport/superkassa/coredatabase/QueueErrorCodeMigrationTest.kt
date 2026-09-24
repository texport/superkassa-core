package io.github.texport.superkassa.coredatabase

import androidx.sqlite.execSQL
import io.github.texport.superkassa.core.domain.api.model.queue.QueueTask
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.coredatabase.api.RoomStorageFactory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * База версии 14 открывается строгим открытием: шаг 14 → 15 даёт задаче
 * очереди код отказа БФД, задачи прежней версии остаются без кода.
 * Код пишется рядом с текстом и в файле, и в памяти.
 */
class QueueErrorCodeMigrationTest {
    private val file = RoomFile()

    @AfterTest
    fun cleanUp() = file.delete()

    @Test
    fun `задача четырнадцатой версии остаётся и получает код отказа`() {
        file.session { }
        file.sql { connection ->
            connection.execSQL("ALTER TABLE queue_commands DROP COLUMN lastErrorCode")
            connection.execSQL(
                "INSERT INTO queue_commands (id, cashboxId, lane, type, payloadRef, status, attempt, lastError, " +
                    "nextAttemptAt, createdAt) VALUES ('task-old', 'kkm-1', 'OFFLINE', 'TICKET', 'doc-1', " +
                    "'FAILED', 1, 'RU: а | KK: ә | EN: a', 5, 1)"
            )
            connection.execSQL("PRAGMA user_version = 14")
        }

        val before = file.session { it.task().lastErrorCode }
        val after = file.session { rejectAndRead(it) }

        assertEquals(null to REFUSED, before to after)
    }

    @Test
    fun `в памяти код отказа пишется так же`() {
        val storage = RoomStorageFactory.createDefaultStorage().storagePort
        storage.enqueueQueueTask(queued())

        assertEquals(REFUSED, rejectAndRead(storage))
    }

    private fun rejectAndRead(storage: StoragePort): Int? {
        storage.updateQueueTaskStatus("task-old", "REJECTED", 2, "RU: а | KK: ә | EN: a", null, REFUSED)
        return storage.task().lastErrorCode
    }

    private fun StoragePort.task() = listQueueTasksByCashbox("kkm-1", "OFFLINE", limit = 10).single()

    private fun queued() = QueueTask(
        id = "task-old",
        cashboxId = "kkm-1",
        lane = "OFFLINE",
        type = "TICKET",
        payloadRef = "doc-1",
        createdAt = 1L,
        status = "PENDING",
        attempt = 0,
        nextAttemptAt = null,
        lastError = null
    )

    private companion object {
        /** RESULT_TYPE_INCORRECT_REQUEST_DATA. */
        const val REFUSED = 13
    }
}
