package io.github.texport.superkassa.testing.api.kassa

import io.github.texport.superkassa.core.domain.api.model.settings.DeliveryChannelSettings
import io.github.texport.superkassa.core.domain.api.model.settings.DeliverySettings
import io.github.texport.superkassa.core.presentation.api.model.delivery.ReceiptDeliveryResponse
import io.github.texport.superkassa.delivery.api.model.DeliveryChannel
import io.github.texport.superkassa.delivery.api.model.DeliveryRequest
import io.github.texport.superkassa.delivery.api.model.DeliveryResult
import io.github.texport.superkassa.delivery.api.port.DeliveryPort
import io.github.texport.superkassa.embedded.api.SuperkassaConfig
import io.github.texport.superkassa.testing.api.kassa.BenchDirectory.Companion.NOT_PAYER
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** Телефон покупателя в настройках доставки: в журнал он попадать не должен. */
internal const val BUYER_PHONE = "+77017654321"

/** Чек покупателю по SMS страницей: рисовать её быстрее, чем PDF. */
internal fun smsReceipt(extra: DeliverySettings = DeliverySettings()) = extra.copy(
    channels = listOf(DeliveryChannelSettings("SMS", documentFormat = "HTML", destination = BUYER_PHONE)) + extra.channels
)

/**
 * Подменный SMS: запоминает отправленное и отвечает [answer].
 * Пока [gate] закрыт, отправка ждёт — так ведёт себя медленный провайдер.
 */
internal class RecordingSms(
    private val gate: CountDownLatch = CountDownLatch(0),
    @Volatile var answer: DeliveryResult = DeliveryResult(ok = true)
) : DeliveryPort {
    override val channel: DeliveryChannel = DeliveryChannel.SMS
    val sent: MutableList<DeliveryRequest> = CopyOnWriteArrayList()
    val entered = CountDownLatch(1)

    override fun send(request: DeliveryRequest): DeliveryResult {
        entered.countDown()
        gate.await(WAIT.inWholeSeconds, TimeUnit.SECONDS)
        sent += request
        return answer
    }
}

/**
 * Касса на стенде с доставкой [delivery] и каналами [channels].
 *
 * Настройки доставки действуют со следующего запуска, поэтому касса
 * заводится, получает их и открывается заново — как у владельца.
 */
internal class DeliveryStand(
    private val directory: BenchDirectory,
    delivery: DeliverySettings,
    channels: List<DeliveryPort>,
    config: (List<DeliveryPort>) -> SuperkassaConfig = { testSuperkassaConfig(it) }
) : AutoCloseable {
    var bench: TestBench = directory.open()
        private set
    var kassa: ReadyKassa
        private set

    init {
        val first = bench.registerKassa(NOT_PAYER).also { it.openShift() }
        bench.superkassa.settings.run { updateSettings(getSettings().copy(delivery = delivery)) }
        bench.close()
        bench = directory.reopen(bench.bfd, bench.clock, config(channels))
        kassa = bench.kassa(first.kkmId, first.adminPin, first.cashierPin)
    }

    /** Перезапуск кассы на том же каталоге — с каналами [channels]. */
    fun restart(channels: List<DeliveryPort>) {
        bench.close()
        bench = directory.reopen(bench.bfd, bench.clock, testSuperkassaConfig(channels))
        kassa = bench.kassa(kassa.kkmId, kassa.adminPin, kassa.cashierPin)
    }

    override fun close() = bench.close()
}

/** Настройки проверки, но фон доставки заходит каждые 20 мс — как у кассы, только чаще. */
internal fun backgroundConfig(channels: List<DeliveryPort>) = SuperkassaConfig(
    ofdProviderId = "KAZAKHTELECOM",
    ofdProtocolVersion = "203",
    ownerId = "testing",
    queueInterval = 1.hours,
    shiftCheckInterval = 1.hours,
    deliveryInterval = 20.milliseconds,
    channels = channels
)

/** Ждёт, пока [condition] не станет истинным, не дольше [WAIT]. */
internal fun eventually(condition: () -> Boolean) {
    val deadline = System.nanoTime() + WAIT.inWholeNanoseconds
    while (!condition()) {
        check(System.nanoTime() < deadline) { "condition was not met in $WAIT" }
        Thread.sleep(POLL_MILLIS)
    }
}

/** Единственная доставка чека — по SMS. */
internal fun List<ReceiptDeliveryResponse>.sms(): ReceiptDeliveryResponse = single { it.channel == "SMS" }

internal val WAIT: Duration = 10.seconds
private const val POLL_MILLIS = 20L
