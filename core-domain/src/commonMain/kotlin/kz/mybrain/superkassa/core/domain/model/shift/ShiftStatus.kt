package kz.mybrain.superkassa.core.domain.model.shift

import kotlinx.serialization.Serializable

/**
 * Статусы смены ККМ (открыта, закрыта).
 */
@Serializable
enum class ShiftStatus {
    /**
     * Кассовая смена открыта.
     */
    OPEN,

    /**
     * Кассовая смена закрыта.
     */
    CLOSED
}
