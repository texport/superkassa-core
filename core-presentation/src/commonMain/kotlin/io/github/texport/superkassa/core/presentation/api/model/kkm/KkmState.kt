package io.github.texport.superkassa.core.presentation.api.model.kkm

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Состояния ККМ.
 */
@Serializable
@Schema(description = "Состояние ККМ")
enum class KkmState {
    /** Стадия регистрации аппарата */
    @Schema(description = "Регистрация")
    REGISTRATION,

    /** Касса активна и готова к работе */
    @Schema(description = "Активна")
    ACTIVE,

    /** Касса заблокирована */
    @Schema(description = "Заблокирована")
    BLOCKED,

    /** Режим программирования настроек */
    @Schema(description = "Программирование")
    PROGRAMMING
}
