package io.github.texport.superkassa.core.presentation.api.model.ofd

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Провайдеры ОФД (Операторы фискальных данных Республики Казахстан).
 *
 * Состав перечисления повторяет доменный `OfdProvider`: справочник узла — это
 * то, к чему касса действительно может подключиться, а не подмножество.
 */
@Serializable
@Schema(description = "Провайдер ОФД")
enum class OfdProvider {
    /** АО «Казахтелеком» (oofd.kz) */
    @Schema(description = "АО «Казахтелеком»")
    KAZAKHTELECOM,

    /** ТОО «БФД» */
    @Schema(description = "ОФД БФД")
    BFD
}
