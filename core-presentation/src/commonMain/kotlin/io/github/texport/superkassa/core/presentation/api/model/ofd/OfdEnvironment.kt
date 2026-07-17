package io.github.texport.superkassa.core.presentation.api.model.ofd

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Среды/окружения взаимодействия с ОФД.
 */
@Serializable
@Schema(description = "Среда взаимодействия с ОФД")
enum class OfdEnvironment {
    /** Стенд разработки */
    @Schema(description = "Стенд разработки")
    DEV,

    /** Тестовый стенд ОФД */
    @Schema(description = "Тестовый стенд")
    TEST,

    /** Продуктивный сервер ОФД */
    @Schema(description = "Продуктивный сервер")
    PROD
}
