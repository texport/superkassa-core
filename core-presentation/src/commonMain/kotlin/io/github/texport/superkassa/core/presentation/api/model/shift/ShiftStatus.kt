package io.github.texport.superkassa.core.presentation.api.model.shift

import kotlinx.serialization.Serializable

@Serializable
enum class ShiftStatus {
    OPEN,
    CLOSED
}
