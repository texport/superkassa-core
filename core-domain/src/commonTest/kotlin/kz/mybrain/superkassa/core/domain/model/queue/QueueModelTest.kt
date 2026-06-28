package kz.mybrain.superkassa.core.domain.model.queue

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class QueueModelTest {

    @Test
    fun testOfflineQueueCommandRequestEqualsAndHashCode() {
        val req1 = OfflineQueueCommandRequest("kkm-1", "TICKET", "ref-1")
        val req1Copy = req1.copy()
        val reqSame = OfflineQueueCommandRequest("kkm-1", "TICKET", "ref-1")
        val reqDiffKkm = req1.copy(kkmId = "kkm-2")
        val reqDiffType = req1.copy(type = "CLOSE_SHIFT")
        val reqDiffRef = req1.copy(payloadRef = "ref-2")

        assertEquals(req1, req1)
        assertEquals(req1, req1Copy)
        assertEquals(req1, reqSame)
        assertEquals(req1.hashCode(), reqSame.hashCode())

        assertNotEquals(req1, Any())
        assertFalse(req1.equals(null))
        assertNotEquals(req1, reqDiffKkm)
        assertNotEquals(req1, reqDiffType)
        assertNotEquals(req1, reqDiffRef)

        assertTrue(req1.toString().contains("kkmId=kkm-1"))
    }

    @Test
    fun testQueueTaskEqualsAndHashCode() {
        val task1 = QueueTask(
            id = "t-1",
            cashboxId = "c-1",
            lane = "lane-1",
            type = "type-1",
            payloadRef = "ref-1",
            createdAt = 1000L,
            status = "PENDING",
            attempt = 1,
            nextAttemptAt = 2000L,
            lastError = "error-1"
        )
        val task1Copy = task1.copy()
        val taskSame = QueueTask("t-1", "c-1", "lane-1", "type-1", "ref-1", 1000L, "PENDING", 1, 2000L, "error-1")
        val taskDiffId = task1.copy(id = "t-2")
        val taskDiffCashbox = task1.copy(cashboxId = "c-2")
        val taskDiffLane = task1.copy(lane = "lane-2")
        val taskDiffType = task1.copy(type = "type-2")
        val taskDiffRef = task1.copy(payloadRef = "ref-2")
        val taskDiffCreated = task1.copy(createdAt = 1001L)
        val taskDiffStatus = task1.copy(status = "SUCCESS")
        val taskDiffAttempt = task1.copy(attempt = 2)
        val taskDiffNextAttempt = task1.copy(nextAttemptAt = 3000L)
        val taskDiffLastError = task1.copy(lastError = "error-2")
        val taskNullNextAttempt = task1.copy(nextAttemptAt = null)
        val taskNullLastError = task1.copy(lastError = null)

        assertEquals(task1, task1)
        assertEquals(task1, task1Copy)
        assertEquals(task1, taskSame)
        assertEquals(task1.hashCode(), taskSame.hashCode())

        assertNotEquals(task1, Any())
        assertFalse(task1.equals(null))
        assertNotEquals(task1, taskDiffId)
        assertNotEquals(task1, taskDiffCashbox)
        assertNotEquals(task1, taskDiffLane)
        assertNotEquals(task1, taskDiffType)
        assertNotEquals(task1, taskDiffRef)
        assertNotEquals(task1, taskDiffCreated)
        assertNotEquals(task1, taskDiffStatus)
        assertNotEquals(task1, taskDiffAttempt)
        assertNotEquals(task1, taskDiffNextAttempt)
        assertNotEquals(task1, taskDiffLastError)
        assertNotEquals(task1, taskNullNextAttempt)
        assertNotEquals(task1, taskNullLastError)

        assertTrue(task1.toString().contains("id=t-1"))
        assertTrue(taskNullLastError.toString().contains("lastError=null"))
    }
}
