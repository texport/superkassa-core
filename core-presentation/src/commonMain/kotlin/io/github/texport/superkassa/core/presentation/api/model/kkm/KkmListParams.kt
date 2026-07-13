package io.github.texport.superkassa.core.presentation.api.model.kkm

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import io.github.texport.superkassa.core.presentation.api.annotations.Min
import io.github.texport.superkassa.core.presentation.api.annotations.Max
import io.github.texport.superkassa.core.presentation.api.annotations.NotBlank
import kotlinx.serialization.Serializable

/**
 * Параметры листинга ККМ.
 */
@Serializable
@Schema(description = "Параметры фильтрации и пагинации списка ККМ")
data class KkmListParams(
    @Schema(description = "Лимит количества возвращаемых записей (1-1000)", example = "50")
    @field:Min(1)
    @field:Max(1000)
    val limit: Int = 50,
    @Schema(description = "Смещение для постраничной навигации", example = "0")
    @field:Min(0)
    val offset: Int = 0,
    @Schema(description = "Фильтрация по состоянию ККМ (например, active, blocked)", example = "active")
    val state: String? = null,
    @Schema(description = "Поисковый запрос по заводскому номеру или названию", example = "SWK")
    val search: String? = null,
    @Schema(
        description = "Поле для сортировки. Допустимые значения: createdAt, updatedAt, state, registrationNumber",
        example = "createdAt"
    )
    @field:NotBlank
    val sortBy: String = "createdAt",
    @Schema(description = "Направление сортировки: ASC (по возрастанию) или DESC (по убыванию)", example = "DESC")
    @field:NotBlank
    val sortOrder: String = "DESC"
) {
    init {
        require(limit in 1..1000) { "limit должен быть от 1 до 1000" }
        require(offset >= 0) { "offset должен быть >= 0" }
        require(sortOrder in listOf("ASC", "DESC")) { "sortOrder должен быть ASC или DESC" }
        require(sortBy in listOf("createdAt", "updatedAt", "state", "registrationNumber")) {
            "sortBy должен быть одним из: createdAt, updatedAt, state, registrationNumber"
        }
    }
}
