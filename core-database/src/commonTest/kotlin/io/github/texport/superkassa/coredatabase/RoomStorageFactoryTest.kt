package io.github.texport.superkassa.coredatabase

import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmMode
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmState
import io.github.texport.superkassa.coredatabase.api.RoomStorageFactory
import io.github.texport.superkassa.offlinequeue.api.model.QueueCommand
import io.github.texport.superkassa.offlinequeue.api.model.QueueCommandType
import io.github.texport.superkassa.offlinequeue.api.model.QueueLane
import io.github.texport.superkassa.offlinequeue.api.model.QueueStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RoomStorageFactoryTest {

    @Test
    fun testRoomStorageFactoryOperations() {
        val pair = RoomStorageFactory.createDefaultStorage()
        val storage = pair.storagePort
        val queue = pair.queueStoragePort

        val now = 1000L
        val kkm = KkmInfo(
            id = "kkm-1",
            createdAt = now,
            updatedAt = now,
            mode = KkmMode.REGISTRATION.name,
            state = KkmState.ACTIVE.name,
            registrationNumber = "RNM-100",
            factoryNumber = "SWK-999",
            autoCloseShift = true,
            autoCashout = false
        )

        assertTrue(storage.createKkm(kkm))
        val fetched = storage.findKkm("kkm-1")
        assertNotNull(fetched)
        assertEquals("RNM-100", fetched.registrationNumber)

        val cmd = QueueCommand(
            id = "cmd-1",
            cashboxId = "kkm-1",
            lane = QueueLane.OFFLINE,
            type = QueueCommandType.TICKET,
            payloadRef = "payload-1",
            createdAt = now,
            status = QueueStatus.PENDING,
            attempt = 0
        )

        assertTrue(queue.enqueue(cmd))
        val commands = queue.getCommandsByStatus("kkm-1", QueueLane.OFFLINE, setOf(QueueStatus.PENDING))
        assertEquals(1, commands.size)
        assertEquals("cmd-1", commands.first().id)
    }

    @Test
    fun testCreateRoomStorageDisk() {
        val pair = RoomStorageFactory.createRoomStorage("test_room_storage.db")
        assertNotNull(pair.storagePort)
        assertNotNull(pair.queueStoragePort)
    }
}
