package io.github.texport.superkassa.core.domain.impl.usecase.shift

import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftDayLimitState
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.logging.getLogger
import io.github.texport.superkassa.core.string.api.CoreStrings

/**
 * Смена не длиннее суток — одно правило для запрета операций, экрана
 * и автозакрытия.
 *
 * Требование к ККМ (пункты 14, 52 и 93): отсчёт идёт от окончания
 * оформления первого платёжного документа смены, а не от её открытия —
 * открыть смену могут задолго до первой продажи. «Превышение более
 * 24 часов» — строго больше суток: ровно сутки ещё можно.
 *
 * Часы — [ClockPort] кассы. Прежде запрет считало ядро по своим часам
 * от первого чека, экран — по часам узла от открытия смены и с другой
 * границей, и продажа получала отказ, о котором экран не знал.
 */
class ShiftDayLimit(
    private val storage: StoragePort,
    private val clock: ClockPort
) {
    private val logger = getLogger(ShiftDayLimit::class)

    /** Где смена сейчас относительно предела. */
    fun stateOf(shift: ShiftInfo): ShiftDayLimitState {
        val deadline = deadlineOf(shift)
        return ShiftDayLimitState(deadlineAt = deadline, exceeded = deadline != null && clock.now() > deadline)
    }

    /**
     * Отказ кассовой операции в смене, которая длится дольше суток.
     *
     * X-отчёт и закрытие смены сюда не заходят: иначе кассиру было бы
     * нечем ни посмотреть смену, ни выйти из запрета.
     */
    fun requireWithinDay(kkmId: String) {
        val shift = storage.findOpenShift(kkmId) ?: return
        if (!stateOf(shift).exceeded) return
        logger.warn("Shift {} is longer than a day: operations are refused until it is closed", shift.shiftNo)
        throw ValidationException(CoreStrings.shiftLongerThanDay(), "SHIFT_LONGER_THAN_DAY")
    }

    /**
     * Пора закрывать смену автоматически: до предела осталось меньше
     * [AUTO_CLOSE_LEAD_MILLIS] или он уже пройден. Смену без платёжных
     * документов предел не касается, и закрывать её незачем.
     */
    fun isDueForAutoClose(shift: ShiftInfo): Boolean {
        val deadline = deadlineOf(shift) ?: return false
        return clock.now() >= deadline - AUTO_CLOSE_LEAD_MILLIS
    }

    private fun deadlineOf(shift: ShiftInfo): Long? =
        storage.firstPaymentTimeInShift(shift.id)?.plus(DAY_MILLIS)

    companion object {
        /** Сутки — предел продолжительности смены по требованиям к ККМ. */
        const val DAY_MILLIS: Long = 24L * 60 * 60 * 1000

        /**
         * Запас автозакрытия до предела. Полчаса вмещают несколько
         * проверок по часам и повтор после таймаута БФД, пока продажа
         * ещё разрешена.
         */
        const val AUTO_CLOSE_LEAD_MILLIS: Long = 30L * 60 * 1000
    }
}
