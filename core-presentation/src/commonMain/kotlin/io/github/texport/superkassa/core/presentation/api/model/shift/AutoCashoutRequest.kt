package io.github.texport.superkassa.core.presentation.api.model.shift

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Запрос на автоизъятие наличных при закрытии смены.
 *
 * Настройка читалась при закрытии смены, но задать её было нечем: в API
 * её не было, и касса закрывала смену, оставляя наличные в ящике.
 */
@Serializable
@Schema(description = "Запрос на обновление настройки автоизъятия наличных при закрытии смены")
data class AutoCashoutRequest(
    @Schema(description = "Изымать ли наличные при закрытии смены", example = "true")
    val autoCashout: Boolean
)
