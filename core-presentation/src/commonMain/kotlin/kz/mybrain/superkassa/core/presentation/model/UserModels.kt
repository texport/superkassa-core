package kz.mybrain.superkassa.core.presentation.model

import kz.mybrain.superkassa.core.presentation.annotations.Schema
import kz.mybrain.superkassa.core.presentation.annotations.NotBlank
import kz.mybrain.superkassa.core.presentation.annotations.Size
import kotlinx.serialization.Serializable
import kz.mybrain.superkassa.core.domain.model.auth.UserRole

/**
 * Запрос на создание пользователя ККМ (кассира или администратора).
 *
 * @property name Имя нового пользователя.
 * @property role Роль пользователя (ADMIN или CASHIER).
 * @property userPin ПИН-код нового пользователя.
 */
@Serializable
@Schema(description = "Запрос на создание пользователя")
data class UserCreateRequest(
    @Schema(description = "Имя нового пользователя", example = "Иван Иванов")
    @field:NotBlank(message = "Name is required")
    val name: String,
    @Schema(description = "Роль пользователя (ADMIN или CASHIER)", example = "CASHIER")
    val role: UserRole,
    @Schema(description = "ПИН-код нового пользователя", example = "1234")
    @field:NotBlank(message = "User PIN is required")
    @field:Size(min = 4, max = 10, message = "PIN length must be between 4 and 10")
    val userPin: String
)

/**
 * Запрос на обновление параметров пользователя ККМ.
 *
 * @property name Новое имя пользователя.
 * @property role Новая роль пользователя.
 * @property userPin Новый ПИН-код пользователя.
 */
@Serializable
@Schema(description = "Запрос на обновление пользователя")
data class UserUpdateRequest(
    @Schema(description = "Новое имя пользователя", example = "Петр Петров")
    val name: String? = null,
    @Schema(description = "Новая роль", example = "ADMIN") val role: UserRole? = null,
    @Schema(description = "Новый ПИН-код пользователя", example = "4321")
    val userPin: String? = null
)

/**
 * Запрос на удаление пользователя.
 *
 * @property _unused Не используется.
 */
@Serializable
@Schema(description = "Запрос на удаление пользователя")
data class UserDeleteRequest(
    @Schema(
        description = "Не используется. ПИН-код передается только в заголовке Authorization",
        example = "deprecated"
    )
    val _unused: String? = null
)

/**
 * Данные пользователя ККМ, возвращаемые API.
 *
 * @property userId Уникальный идентификатор пользователя.
 * @property name Имя пользователя.
 * @property role Роль пользователя.
 * @property pin ПИН-код пользователя (для выгрузок/сверки).
 */
@Serializable
@Schema(description = "Данные пользователя")
data class UserResponse(
    @Schema(description = "ID пользователя", example = "user-123") val userId: String,
    @Schema(description = "Имя пользователя", example = "Иван Иванов") val name: String,
    @Schema(description = "Роль пользователя", example = "CASHIER") val role: UserRole,
    @Schema(
        description = "ПИН-код пользователя (возвращается только для справки)",
        example = "1234"
    )
    val pin: String?
)
