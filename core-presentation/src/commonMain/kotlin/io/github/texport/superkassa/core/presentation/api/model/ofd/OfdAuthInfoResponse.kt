package io.github.texport.superkassa.core.presentation.api.model.ofd

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Информация об авторизации в ОФД, возвращаемая API.
 */
@Serializable
@Schema(description = "Информация об авторизации в ОФД")
data class OfdAuthInfoResponse(
    @Schema(description = "Текущий токен ОФД", example = "token-xyz") val token: String?,
    @Schema(description = "Следующий номер запроса (reqNum)", example = "105")
    val nextReqNum: Int
)
