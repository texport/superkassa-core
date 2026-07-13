package io.github.texport.superkassa.core.presentation.api.model.ofd

import kotlinx.serialization.Serializable

@Serializable
enum class DeliveryStatus {
    ONLINE_OK,
    ONLINE_ERROR,
    OFFLINE_QUEUED,
    NOT_SENT
}
