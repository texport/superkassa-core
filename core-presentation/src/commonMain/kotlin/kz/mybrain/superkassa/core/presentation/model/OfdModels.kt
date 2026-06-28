package kz.mybrain.superkassa.core.presentation.model

import kz.mybrain.superkassa.core.presentation.annotations.Schema
import kz.mybrain.superkassa.core.presentation.annotations.NotBlank
import kotlinx.serialization.Serializable

/**
 * Запрос на обновление токена авторизации в ОФД.
 *
 * @property token Новый токен ОФД.
 */
@Serializable
@Schema(description = "Запрос на обновление токена ОФД")
data class OfdTokenUpdateRequest(
    @Schema(description = "Новый токен ОФД", example = "new-token-123")
    @field:NotBlank
    val token: String
)

/**
 * Информация об авторизации в ОФД, возвращаемая API.
 *
 * @property token Текущий активный токен ОФД.
 * @property nextReqNum Номер следующего запроса в ОФД.
 */
@Serializable
@Schema(description = "Информация об авторизации в ОФД")
data class OfdAuthInfoResponse(
    @Schema(description = "Текущий токен ОФД", example = "token-xyz") val token: String?,
    @Schema(description = "Следующий номер запроса (reqNum)", example = "105")
    val nextReqNum: Int
)
