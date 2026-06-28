package kz.mybrain.superkassa.core.presentation.model

import kotlinx.serialization.Serializable

/**
 * Ответ на операцию повторной отправки неотправленных чеков.
 *
 * @property results Результаты отправки по каждому каналу.
 */
@Serializable
data class DeliveryRetryResponse(
    val results: List<DeliveryRetryItemResponse>
)

/**
 * Статус доставки по конкретному каналу.
 *
 * @property channel Название канала доставки (например, SMS, EMAIL).
 * @property success Успешность доставки.
 */
@Serializable
data class DeliveryRetryItemResponse(
    val channel: String,
    val success: Boolean
)
