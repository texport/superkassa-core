package io.github.texport.superkassa.core.presentation.api.model.kkm

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Запрос на изменение названия кассы.
 *
 * Пустое значение снимает название: касса снова показывается
 * регистрационным номером.
 */
@Serializable
@Schema(description = "Название кассы, данное владельцем")
data class KkmNameUpdateRequest(
    @Schema(description = "Название кассы; пусто — снять название", example = "Касса 2 на Достык")
    val name: String? = null
)
