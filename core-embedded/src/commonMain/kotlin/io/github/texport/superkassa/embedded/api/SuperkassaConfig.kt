package io.github.texport.superkassa.embedded.api

import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryRetryPolicy
import io.github.texport.superkassa.delivery.api.port.DeliveryPort
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
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
 * @property shiftCheckInterval пауза между проверками автозакрытия смены:
 *   у касс с настройкой «автозакрытие» смена закрывается до предела в сутки.
 * @property deliveryInterval пауза между заходами доставки чеков покупателям.
 * @property deliveryAttempts сколько раз чек отправляется по каналу, прежде
 *   чем доставка станет окончательным отказом.
 * @property deliveryRetryPause пауза перед первым повтором доставки; дальше она
 *   удваивается до получаса.
 * @property channels каналы доставки чека покупателю вместо собранных
 *   из настроек. По умолчанию SMS, Telegram, WhatsApp и почта собираются
 *   из `CoreSettings.delivery`, и приложению их знать не нужно; канал
 *   из этого списка заменяет одноимённый канал из настроек — так проверка
 *   подставляет свой. Ненастроенный канал отвечает отказом, а не успехом.
 */
class SuperkassaConfig(
    val ofdProviderId: String,
    val ofdProtocolVersion: String,
    val ownerId: String = "embedded",
    val queueInterval: Duration = 5.seconds,
    val queueBatchSize: Int = 5,
    val shiftCheckInterval: Duration = 1.minutes,
    val deliveryInterval: Duration = 3.seconds,
    val deliveryAttempts: Int = DeliveryRetryPolicy().attempts,
    val deliveryRetryPause: Duration = DeliveryRetryPolicy().firstPause,
    val channels: List<DeliveryPort> = emptyList()
) {
    init {
        require(queueBatchSize > 0) { "queueBatchSize must be positive" }
        require(queueInterval.isPositive()) { "queueInterval must be positive" }
        require(shiftCheckInterval.isPositive()) { "shiftCheckInterval must be positive" }
        require(deliveryInterval.isPositive()) { "deliveryInterval must be positive" }
    }

    /**
     * Повторы доставки чека: попытки и первая пауза — отсюда, предел паузы
     * и срок занятости — ядра. Заводится сразу: негодное число попыток
     * отказывает здесь, а не при открытии кассы.
     */
    internal val deliveryPolicy: DeliveryRetryPolicy = DeliveryRetryPolicy(
        attempts = deliveryAttempts,
        firstPause = deliveryRetryPause,
        longestPause = maxOf(DeliveryRetryPolicy().longestPause, deliveryRetryPause)
    )
}
