package io.github.texport.superkassa.core.presentation.api.model.ofd

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Типы команд взаимодействия с ОФД.
 */
@Serializable
@Schema(description = "Тип команды ОФД")
enum class OfdCommandType {
    /** Команда передачи чека */
    @Schema(description = "Команда чека")
    TICKET,

    /** Системная сервисная команда */
    @Schema(description = "Системная команда")
    SYSTEM,

    /** Запрос информации о состоянии/статусе ККМ в ОФД */
    @Schema(description = "Запрос информации")
    INFO,

    /** Команда внесения/изъятия денег */
    @Schema(description = "Команда внесения/изъятия наличных")
    MONEY_PLACEMENT,

    /** Команда формирования X-отчета */
    @Schema(description = "Команда X-отчета")
    REPORT,

    /** Команда закрытия смены (Z-отчет) */
    @Schema(description = "Команда закрытия смены (Z-отчет)")
    CLOSE_SHIFT,

    /** Команда обновления/синхронизации справочника номенклатур */
    @Schema(description = "Команда номенклатуры")
    NOMENCLATURE
}
