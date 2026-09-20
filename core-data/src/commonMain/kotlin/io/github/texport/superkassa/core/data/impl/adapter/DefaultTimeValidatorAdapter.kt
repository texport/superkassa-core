package io.github.texport.superkassa.core.data.impl.adapter

import io.github.texport.superkassa.core.domain.api.model.common.TimeValidationResult
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.integration.TimeValidatorPort
import kotlin.math.abs

/**
 * Валидатор корректности системного времени кассового аппарата по умолчанию.
 *
 * Проверяет рассинхронизацию (drift) между временем ККМ и эталонными часами системы.
 * По умолчанию допустимое расхождение составляет не более 24 часов (86400 секунд).
 *
 * @property maxAllowedDriftSeconds максимально допустимое расхождение времени в секундах.
 */
internal class DefaultTimeValidatorAdapter(
    private val maxAllowedDriftSeconds: Long = 86400L
) : TimeValidatorPort {

    /**
     * Валидирует рассинхронизацию времени на ККМ.
     *
     * @param clock источник времени ККМ для проверки.
     * @return результат проверки [TimeValidationResult] с флагом `ok` и описанием причины в случае ошибки.
     */
    override fun validate(clock: ClockPort): TimeValidationResult {
        val deviceTimeSeconds = clock.now() / 1000L
        val systemTimeSeconds = kotlin.time.Clock.System.now().toEpochMilliseconds() / 1000L
        val driftSeconds = abs(deviceTimeSeconds - systemTimeSeconds)
        val isValid = driftSeconds <= maxAllowedDriftSeconds
        return TimeValidationResult(
            ok = isValid,
            reason = if (isValid) null else "Time drift exceeded $maxAllowedDriftSeconds seconds"
        )
    }
}
