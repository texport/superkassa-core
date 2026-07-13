package io.github.texport.superkassa.core.presentation.api.model.receipt

import kotlinx.serialization.Serializable

@Serializable
enum class PrintDocumentType {
    DOCUMENT,
    X_REPORT,
    OPEN_SHIFT,
    CLOSE_SHIFT
}
