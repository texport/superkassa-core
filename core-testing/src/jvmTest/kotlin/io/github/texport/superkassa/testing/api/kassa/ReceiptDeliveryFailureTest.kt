package io.github.texport.superkassa.testing.api.kassa

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.sun.net.httpserver.HttpServer
import io.github.texport.superkassa.core.domain.api.model.settings.DeliveryChannelSettings
import io.github.texport.superkassa.core.domain.api.model.settings.DeliverySettings
import io.github.texport.superkassa.core.domain.api.model.settings.SmsProviderSettings
import io.github.texport.superkassa.core.presentation.api.model.delivery.ReceiptDeliveryState
import io.github.texport.superkassa.core.string.api.TrilingualMessage
import io.github.texport.superkassa.delivery.api.model.DeliveryResult
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

/**
 * Не дошедший чек виден кассиру и владельцу: канал, код и причина на трёх
 * языках. Отказ, который повтором не лечится, повторов не получает; прочий
 * повторяется до предела попыток. Адрес покупателя и ключ канала в журнал
 * не попадают.
 */
class ReceiptDeliveryFailureTest {
    private val directory = BenchDirectory()
    private var stand: DeliveryStand? = null
    private val journal = ListAppender<ILoggingEvent>().apply { start() }
    private val root = (LoggerFactory.getLogger(ROOT_LOGGER) as Logger).apply {
        level = Level.TRACE
        addAppender(journal)
    }

    @AfterTest
    fun tearDown() {
        root.detachAppender(journal)
        stand?.close()
        directory.close()
    }

    @Test
    fun `отказ провайдера - ждёт повтора с кодом и причиной, после предела попыток - не удалось`() {
        val sms = RecordingSms(answer = DeliveryResult(false, REJECTED.compact(), "DELIVERY_PROVIDER_REJECTED"))
        val kassa = open(smsReceipt(), listOf(sms))
        val sale = kassa.sell()

        kassa.deliverReceipts()
        val waiting = kassa.deliveries(sale).sms()
        repeat(ATTEMPTS) { passPause(kassa) }
        val failed = kassa.deliveries(sale).sms()

        assertEquals(ReceiptDeliveryState.PENDING to 1, waiting.state to waiting.attempts)
        assertEquals("DELIVERY_PROVIDER_REJECTED" to REJECTED.kk, waiting.failureCode to waiting.failureMessage?.kk)
        assertEquals(ReceiptDeliveryState.FAILED to ATTEMPTS, failed.state to failed.attempts)
        assertEquals(REJECTED.en, failed.failureMessage?.en)
        assertEquals(ATTEMPTS, sms.sent.size)
    }

    @Test
    fun `ненастроенный канал - сразу не удалось, без повторов`() {
        val telegram = DeliveryChannelSettings("TELEGRAM", documentFormat = "HTML", destination = "-1001234567")
        val kassa = open(DeliverySettings(channels = listOf(telegram)), emptyList())
        val sale = kassa.sell()

        kassa.deliverReceipts()
        passPause(kassa)
        val delivery = kassa.deliveries(sale).single()

        assertEquals(ReceiptDeliveryState.FAILED to 1, delivery.state to delivery.attempts)
        assertEquals("DELIVERY_TELEGRAM_NOT_CONFIGURED", delivery.failureCode)
        assertEquals(true, delivery.failureMessage?.ru?.contains("не настроен"), delivery.failureMessage?.ru)
    }

    @Test
    fun `повтор кассиром после окончательного отказа отправляет чек заново один раз`() {
        val sms = RecordingSms(answer = DeliveryResult(false, REJECTED.compact(), "DELIVERY_PROVIDER_REJECTED"))
        val kassa = open(smsReceipt(), listOf(sms))
        val sale = kassa.sell()
        kassa.deliverReceipts()
        repeat(ATTEMPTS) { passPause(kassa) }

        sms.answer = DeliveryResult(ok = true)
        val resent = kassa.resendReceipt(sale).sms()

        assertEquals(ReceiptDeliveryState.DELIVERED to null, resent.state to resent.failureCode)
        assertEquals(ATTEMPTS + 1, sms.sent.size)
        assertEquals(0, kassa.deliverReceipts())
    }

    @Test
    fun `адрес покупателя и ключ канала в журнал не попадают`() {
        val gateway = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/send") { it.sendResponseHeaders(SERVER_ERROR, -1); it.close() }
            start()
        }
        try {
            val url = "http://127.0.0.1:${gateway.address.port}/send?to={phone}&text={text}"
            val kassa = open(smsReceipt(DeliverySettings(sms = SmsProviderSettings(url, SMS_KEY))), emptyList())
            val sale = kassa.sell()
            kassa.deliverReceipts()

            assertEquals("DELIVERY_PROVIDER_REJECTED", kassa.deliveries(sale).sms().failureCode)
        } finally {
            gateway.stop(0)
        }
        // Ktor на уровне TRACE пишет адрес запроса целиком — с номером из шаблона
        // провайдера. Это журнал сторонней библиотеки, а не кассы: здесь
        // проверяется журнал кассы и её каналов.
        val lines = journal.list.filterNot { it.loggerName.startsWith("io.ktor") }.map { it.formattedMessage }
        assertEquals(true, lines.any { "Receipt delivery" in it }, "delivery must be journaled")
        assertEquals(emptyList(), lines.filter { BUYER_PHONE in it || "7017654321" in it || SMS_KEY in it })
    }

    private fun open(delivery: DeliverySettings, channels: List<RecordingSms>): ReadyKassa =
        DeliveryStand(directory, delivery, channels).also { stand = it }.kassa

    /** Часы кассы переходят за самую длинную паузу повтора, и фон заходит ещё раз. */
    private fun passPause(kassa: ReadyKassa) {
        kassa.clock.move(LONGEST_PAUSE)
        kassa.deliverReceipts()
    }

    private companion object {
        const val ATTEMPTS = 5
        const val ROOT_LOGGER = "ROOT"
        const val SERVER_ERROR = 500
        const val SMS_KEY = "sms-key-5f2c9a"
        val LONGEST_PAUSE = 31.minutes
        val REJECTED = TrilingualMessage(ru = "Провайдер отказал", kk = "Провайдер бас тартты", en = "Provider refused")
    }
}
