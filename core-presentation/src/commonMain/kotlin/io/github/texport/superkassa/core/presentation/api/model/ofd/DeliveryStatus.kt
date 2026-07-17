package io.github.texport.superkassa.core.presentation.api.model.ofd

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Статусы доставки фискальных документов в ОФД.
 */
@Serializable
@Schema(description = "Статус отправки в ОФД")
enum class DeliveryStatus {
    /** Успешно доставлен в ОФД */
    @Schema(description = "Доставлен")
    ONLINE_OK,

    /** Ошибка отправки при попытке онлайн передачи */
    @Schema(description = "Ошибка доставки")
    ONLINE_ERROR,

    /** Помещен в автономную очередь для последующей отправки */
    @Schema(description = "В офлайн-очереди")
    OFFLINE_QUEUED,

    /** Не отправлялся */
    @Schema(description = "Не отправлен")
    NOT_SENT
}
