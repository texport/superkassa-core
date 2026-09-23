package io.github.texport.superkassa.embedded.api

import io.github.texport.superkassa.delivery.api.port.DeliveryPort
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Что касса в процессе приложения берёт от приложения.
 *
 * ОФД и версия протокола умолчаний не имеют: их выбирает владелец
 * установки, и касса в приложении не должна молча работать по другой
 * версии, чем узел той же установки.
 *
 * @property ofdProviderId провайдер ОФД, например `KAZAKHTELECOM`.
 * @property ofdProtocolVersion версия протокола CPCR: `202`, `203` или `204`.
 * @property ownerId имя владельца очереди в журнале и в блокировках очереди.
 * @property queueInterval пауза между заходами досылки автономной очереди.
 * @property queueBatchSize сколько документов одной кассы досылается за заход.
 * @property channels каналы доставки чека покупателю, которые приложение
 *   умеет само (почта, мессенджеры). Канала нет в списке — доставка
 *   по нему отвечает отказом, а не успехом.
 */
class SuperkassaConfig(
    val ofdProviderId: String,
    val ofdProtocolVersion: String,
    val ownerId: String = "embedded",
    val queueInterval: Duration = 5.seconds,
    val queueBatchSize: Int = 5,
    val channels: List<DeliveryPort> = emptyList()
) {
    init {
        require(queueBatchSize > 0) { "queueBatchSize must be positive" }
        require(queueInterval.isPositive()) { "queueInterval must be positive" }
    }
}
