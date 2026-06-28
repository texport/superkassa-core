package kz.mybrain.superkassa.core.domain.model.common

import kotlinx.serialization.Serializable

/**
 * Слепок значений счетчиков ККМ.
 *
 * @property scope Область видимости счетчика (GLOBAL или SHIFT).
 * @property shiftId Идентификатор смены (null для глобальных счетчиков).
 * @property key Уникальный ключ счетчика (например, формат из [CounterKeyFormats]).
 * @property value Числовое значение счетчика.
 * @property updatedAt Время последнего обновления значения счетчика (в миллисекундах).
 */
@Serializable
data class CounterSnapshot(
    val scope: String,
    val shiftId: String? = null,
    val key: String,
    val value: Long,
    val updatedAt: Long
)
