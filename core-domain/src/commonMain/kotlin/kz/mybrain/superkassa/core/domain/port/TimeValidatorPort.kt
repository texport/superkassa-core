package kz.mybrain.superkassa.core.domain.port

import kz.mybrain.superkassa.core.domain.model.common.TimeValidationResult

/**
 * Порт проверки расхождения системного времени ККМ с эталонным (серверным или фискальным).
 * Используется для предотвращения регистрации фискальных документов с некорректным временем.
 */
interface TimeValidatorPort {

    /**
     * Выполняет валидацию текущего времени кассы.
     *
     * @param clock текущий источник времени [ClockPort].
     * @return результат проверки [TimeValidationResult], содержащий признак корректности и величину расхождения.
     */
    fun validate(clock: ClockPort): TimeValidationResult
}
