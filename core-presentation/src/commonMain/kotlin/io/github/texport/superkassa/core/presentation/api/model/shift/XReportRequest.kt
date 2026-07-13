package io.github.texport.superkassa.core.presentation.api.model.shift

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Запрос на X-отчет (сменный отчет без гашения).
 */
@Serializable
data class XReportRequest(
    @kotlinx.serialization.Transient
    @Schema(description = "Не используется. Передано для обратной совместимости.", example = "deprecated")
    private val _unused: String? = null
)
