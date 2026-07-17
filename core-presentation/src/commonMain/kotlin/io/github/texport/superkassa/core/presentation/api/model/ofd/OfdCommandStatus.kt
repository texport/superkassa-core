package io.github.texport.superkassa.core.presentation.api.model.ofd

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Статусы выполнения команд взаимодействия с ОФД.
 */
@Serializable
@Schema(description = "Статус выполнения команды ОФД")
enum class OfdCommandStatus {
    /** Успешное выполнение */
    @Schema(description = "Успешно")
    OK,

    /** Выполнение завершилось ошибкой */
    @Schema(description = "Ошибка")
    FAILED,

    /** Истекло время ожидания ответа */
    @Schema(description = "Таймаут")
    TIMEOUT
}
