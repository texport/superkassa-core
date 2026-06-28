package kz.mybrain.superkassa.core.domain.model.settings

import kotlinx.serialization.Serializable

/**
 * Режимы работы ядра фискального регистратора.
 */
@Suppress("unused")
@Serializable
enum class CoreMode {
    /**
     * Десктопный режим работы.
     */
    DESKTOP,

    /**
     * Серверный режим работы.
     */
    SERVER
}
