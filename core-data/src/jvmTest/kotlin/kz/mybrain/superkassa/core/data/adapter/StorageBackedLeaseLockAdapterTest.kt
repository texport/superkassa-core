package kz.mybrain.superkassa.core.data.adapter

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kz.mybrain.superkassa.core.domain.port.StoragePort

class StorageBackedLeaseLockAdapterTest {
    private val storage: StoragePort = mockk()
    private val adapter = StorageBackedLeaseLockAdapter(storage)

    @Test
    fun testTryAcquire() {
        every { storage.tryAcquireQueueLock("kkm-1", "owner-1", 1000L, 500L) } returns true
        assertTrue(adapter.tryAcquire("kkm-1", "owner-1", 1000L, 500L))

        every { storage.tryAcquireQueueLock("kkm-1", "owner-1", 1000L, 500L) } returns false
        assertFalse(adapter.tryAcquire("kkm-1", "owner-1", 1000L, 500L))

        verify(exactly = 2) {
            storage.tryAcquireQueueLock("kkm-1", "owner-1", 1000L, 500L)
        }
    }

    @Test
    fun testRenew() {
        every { storage.renewQueueLock("kkm-1", "owner-1", 2000L, 1000L) } returns true
        assertTrue(adapter.renew("kkm-1", "owner-1", 2000L, 1000L))

        every { storage.renewQueueLock("kkm-1", "owner-1", 2000L, 1000L) } returns false
        assertFalse(adapter.renew("kkm-1", "owner-1", 2000L, 1000L))

        verify(exactly = 2) {
            storage.renewQueueLock("kkm-1", "owner-1", 2000L, 1000L)
        }
    }

    @Test
    fun testRelease() {
        every { storage.releaseQueueLock("kkm-1", "owner-1") } returns true
        assertTrue(adapter.release("kkm-1", "owner-1"))

        every { storage.releaseQueueLock("kkm-1", "owner-1") } returns false
        assertFalse(adapter.release("kkm-1", "owner-1"))

        verify(exactly = 2) {
            storage.releaseQueueLock("kkm-1", "owner-1")
        }
    }
}
