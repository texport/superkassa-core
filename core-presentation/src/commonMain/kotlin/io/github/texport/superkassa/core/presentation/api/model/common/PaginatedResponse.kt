package io.github.texport.superkassa.core.presentation.api.model.common

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Пагинированный ответ.
 */
@Serializable
@Schema(description = "Универсальный пагинированный ответ со списком элементов")
data class PaginatedResponse<T>(
    @Schema(description = "Список элементов на текущей странице")
    val items: List<T>,
    @Schema(description = "Общее количество элементов во всей выборке", example = "100")
    val total: Int,
    @Schema(description = "Количество элементов на странице (лимит)", example = "20")
    val limit: Int,
    @Schema(description = "Смещение от начала выборки (оффсет)", example = "0")
    val offset: Int,
    @Schema(description = "Флаг наличия следующих страниц с элементами", example = "true")
    val hasMore: Boolean
)
