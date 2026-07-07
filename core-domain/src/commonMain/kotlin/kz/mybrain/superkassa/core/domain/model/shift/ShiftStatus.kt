package kz.mybrain.superkassa.core.domain.model.shift

/**
 * Статусы смены ККМ (открыта, закрыта).
 */
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
