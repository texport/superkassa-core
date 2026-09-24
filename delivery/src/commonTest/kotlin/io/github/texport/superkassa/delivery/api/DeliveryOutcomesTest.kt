package io.github.texport.superkassa.delivery.api

import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryOutcome
import io.github.texport.superkassa.core.domain.api.port.integration.DeliveryPort
import io.github.texport.superkassa.core.string.api.TrilingualMessage
import io.github.texport.superkassa.delivery.api.model.DeliveryResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Итог канала становится итогом доставки ядра: код, три языка, повторяемость. */
class DeliveryOutcomesTest {

    @Test
    fun `принятое каналом - доставлено`() {
        assertEquals(DeliveryOutcome.DELIVERED, DeliveryResult(ok = true).toOutcome("SMS"))
    }

    @Test
    fun `текст трёх языков разбирается по языкам, код и повторяемость сохраняются`() {
        val text = TrilingualMessage(ru = "не настроен", kk = "бапталмаған", en = "not configured")

        val outcome = DeliveryResult(false, text.compact(), "DELIVERY_SMS_NOT_CONFIGURED", retryable = false).toOutcome("SMS")

        assertEquals("DELIVERY_SMS_NOT_CONFIGURED" to text, outcome.failure?.code to outcome.failure?.message)
        assertEquals(false, outcome.retryable)
    }

    @Test
    fun `простой текст - на всех трёх языках, отказ без текста и кода - общий`() {
        val plain = DeliveryResult(false, "Printer is off").toOutcome("PRINT")
        val silent = DeliveryResult(false).toOutcome("PRINT")

        assertEquals(TrilingualMessage.mono("Printer is off"), plain.failure?.message)
        assertEquals(DeliveryPort.UNEXPLAINED_FAILURE, silent.failure?.code)
        assertTrue(silent.retryable)
        assertTrue(silent.failure?.message?.en?.contains("PRINT") == true)
    }
}
