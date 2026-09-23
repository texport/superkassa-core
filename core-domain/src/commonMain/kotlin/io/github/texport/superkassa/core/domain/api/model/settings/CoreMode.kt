package io.github.texport.superkassa.core.domain.api.model.settings

import kotlinx.serialization.Serializable

/**
 * Режимы работы ядра фискального регистратора.
 */
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
