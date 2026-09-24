package io.github.texport.superkassa.core.domain.api.model.delivery

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Как повторяется доставка чека после отказа канала.
 *
 * Пауза перед повтором удваивается от [firstPause] до [longestPause]:
 * провайдер, лёгший на минуту, не получает подряд все попытки, а чек,
 * не ушедший утром, всё ещё уходит в тот же день.
 *
 * @property attempts сколько раз задача отправляется, прежде чем стать окончательным отказом.
 * @property firstPause пауза после первого отказа.
 * @property longestPause предел паузы.
 * @property lease сколько задача считается занятой отправкой: касса, упавшая
 *   посреди отправки, после этого срока досылает её заново.
 */
data class DeliveryRetryPolicy(
    val attempts: Int = 5,
    val firstPause: Duration = 30.seconds,
    val longestPause: Duration = 30.minutes,
    val lease: Duration = 2.minutes
) {
    init {
        require(attempts > 0) { "attempts must be positive" }
        require(firstPause.isPositive()) { "firstPause must be positive" }
        require(longestPause >= firstPause) { "longestPause must not be shorter than firstPause" }
        require(lease.isPositive()) { "lease must be positive" }
    }

    /** Пауза после отказа попытки номер [attempt], считая с единицы. */
    fun pauseAfter(attempt: Int): Duration {
        var pause = firstPause
        repeat((attempt - 1).coerceAtLeast(0)) {
            if (pause >= longestPause) return longestPause
            pause *= 2
        }
        return pause.coerceAtMost(longestPause)
    }
}
