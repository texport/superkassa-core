package io.github.texport.superkassa.core.presentation.api.model.ofd

import kotlinx.serialization.Serializable

@Serializable
enum class OfdCommandStatus {
    OK,
    FAILED,
    TIMEOUT
}
