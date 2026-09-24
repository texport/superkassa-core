package io.github.texport.superkassa.core.domain.api.model.delivery

import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryRequest
import io.github.texport.superkassa.core.domain.api.port.integration.DeliveryPort
import io.github.texport.superkassa.core.domain.api.port.integration.DeliveryTaskStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Задача доставки, повторы после отказа и порты по умолчанию. */
class DeliveryTaskModelTest {

    @Test
    fun `пауза удваивается от первой до предела`() {
        val policy = DeliveryRetryPolicy(attempts = 8, firstPause = 30.seconds, longestPause = 5.minutes)

        val pauses = (0..7).map { policy.pauseAfter(it) }

        assertEquals(
            listOf(30.seconds, 30.seconds, 60.seconds, 120.seconds, 240.seconds, 5.minutes, 5.minutes, 5.minutes),
            pauses
        )
    }

    @Test
    fun `негодные повторы отвергаются при заведении`() {
        assertFailsWith<IllegalArgumentException> { DeliveryRetryPolicy(attempts = 0) }
        assertFailsWith<IllegalArgumentException> { DeliveryRetryPolicy(firstPause = 0.seconds) }
        assertFailsWith<IllegalArgumentException> { DeliveryRetryPolicy(firstPause = 2.minutes, longestPause = 1.minutes) }
        assertFailsWith<IllegalArgumentException> { DeliveryRetryPolicy(lease = 0.seconds) }
    }

    @Test
    fun `строка задачи не несёт получателя`() {
        val task = DeliveryTask(
            id = DeliveryTask.idOf("doc-1", "SMS", "LINK"),
            kkmId = "kkm-1",
            documentId = "doc-1",
            channel = "SMS",
            destination = "+77011112233",
            payloadType = "LINK",
            nextAttemptAt = 1L,
            createdAt = 1L
        )

        assertEquals("doc-1/SMS/LINK", task.id)
        assertFalse("+77011112233" in task.toString(), task.toString())
        assertEquals(1L, task.updatedAt)
    }

    @Test
    fun `хранилище без задач доставки честно отказывает`() {
        val store = object : DeliveryTaskStore {}

        assertFailsWith<UnsupportedOperationException> { store.addDeliveryTasks(emptyList()) }
        assertFailsWith<UnsupportedOperationException> { store.dueDeliveryTasks(0L, 1) }
        assertFailsWith<UnsupportedOperationException> { store.claimDeliveryTask("id", 0L, 1L) }
        assertFailsWith<UnsupportedOperationException> { store.deliveryTasksOf("doc-1") }
        assertFailsWith<UnsupportedOperationException> {
            store.saveDeliveryTask(DeliveryTask("id", "kkm", "doc", "SMS", null, "LINK", nextAttemptAt = 0L, createdAt = 0L))
        }
    }

    @Test
    fun `канал без причины отказа получает общий код и повторяется`() {
        val request = DeliveryRequest("kkm-1", "doc-1", "SMS", "+77011112233", "LINK", payloadUrl = "https://bfd.kz/r")
        val refusing = object : DeliveryPort {
            override fun deliver(request: DeliveryRequest) = false
        }
        val accepting = object : DeliveryPort {
            override fun deliver(request: DeliveryRequest) = true
        }

        val refused = refusing.send(request)

        assertEquals(DeliveryPort.UNEXPLAINED_FAILURE to true, refused.failure?.code to refused.retryable)
        assertEquals(true, refused.failure?.message?.ru?.contains("SMS"))
        assertEquals(DeliveryOutcome.DELIVERED, accepting.send(request))
    }
}
