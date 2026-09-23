package io.github.texport.superkassa.core.domain.api.model.auth

/**
 * Неверные пины кассы подряд и блокировка, которую они заработали.
 *
 * Первые [FREE_ATTEMPTS] − 1 ошибок — опечатки и не наказываются. С пятой
 * касса заперта на [FIRST_LOCK_MS], и каждая следующая ошибка после
 * блокировки запирает вдвое дольше, но не дольше [LONGEST_LOCK_MS]. Пин
 * из 4 цифр так перебирается месяцами, а владелец, забывший пин, ждёт
 * не больше четверти часа — блокировки «до сброса» нет. Верный пин
 * обнуляет счёт.
 *
 * @property failures неверные пины подряд.
 * @property lockedUntil до какого момента касса заперта (epoch ms); 0 — не заперта.
 */
data class PinAttempts(val failures: Int = 0, val lockedUntil: Long = 0L) {

    fun isLockedAt(now: Long): Boolean = now < lockedUntil

    /**
     * Попытка засчитана неудачной, пока пин не подтвердится.
     *
     * Запертую кассу попытка не трогает, только срок, ушедший за четверть
     * часа от [now], возвращается к ней: так часы, переведённые назад,
     * не запирают кассу на сутки.
     */
    fun charged(now: Long): PinAttempts {
        if (isLockedAt(now)) return copy(lockedUntil = minOf(lockedUntil, now + LONGEST_LOCK_MS))
        val count = failures + 1
        return PinAttempts(count, if (count < FREE_ATTEMPTS) 0L else now + lockFor(count))
    }

    companion object {
        /** С этой ошибки подряд касса запирается. */
        const val FREE_ATTEMPTS: Int = 5

        /** Первая блокировка. */
        const val FIRST_LOCK_MS: Long = 30_000L

        /** Дольше касса не запирается никогда. */
        const val LONGEST_LOCK_MS: Long = 15 * 60_000L

        /** Удвоений хватает, чтобы дойти до потолка; дальше сдвиг не растёт. */
        private const val MAX_DOUBLINGS = 10

        private fun lockFor(count: Int): Long {
            val doublings = (count - FREE_ATTEMPTS).coerceAtMost(MAX_DOUBLINGS)
            return minOf(FIRST_LOCK_MS shl doublings, LONGEST_LOCK_MS)
        }
    }
}
