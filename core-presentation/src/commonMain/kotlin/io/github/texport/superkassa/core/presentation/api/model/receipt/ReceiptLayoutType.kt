package io.github.texport.superkassa.core.presentation.api.model.receipt

import kotlinx.serialization.Serializable

@Serializable
enum class ReceiptLayoutType {
    TAPE_80MM,
    TAPE_58MM,
    FULLSCREEN
}
