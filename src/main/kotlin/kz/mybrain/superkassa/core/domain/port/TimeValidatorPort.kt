package kz.mybrain.superkassa.core.domain.port

/**
 * Результат валидации системного времени.
 */
data class TimeValidationResult(val ok: Boolean, val reason: String? = null)

/**
 * Порт для проверки корректности системного времени ККМ.
 */
interface TimeValidatorPort {
    /**
     * Выполняет валидацию времени по часам ККМ.
     */
    fun validate(clock: ClockPort): TimeValidationResult
}
