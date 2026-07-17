package io.github.texport.superkassa.core.presentation.api.model.ofd

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Провайдеры ОФД (Операторы фискальных данных Республики Казахстан).
 */
@Serializable
@Schema(description = "Провайдер ОФД")
enum class OfdProvider {
    /** АО «Казахтелеком» (oofd.kz) */
    @Schema(description = "АО «Казахтелеком»")
    KAZAKHTELECOM
}
