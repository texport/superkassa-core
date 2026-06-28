package kz.mybrain.superkassa.core.presentation.model

import kz.mybrain.superkassa.core.presentation.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Запрос с подтверждением PIN-кода.
 *
 * @property _unused Не используется.
 */
@Serializable
@Schema(description = "Запрос с подтверждением ПИН-кода")
data class PinRequest(
    @Schema(
        description = "Не используется. ПИН-код передается только в заголовке Authorization",
        example = "deprecated"
    )
    val _unused: String? = null
)

/**
 * Режим авторизации.
 */
@Serializable
enum class AuthMode {
    NONE,
    BEARER
}
