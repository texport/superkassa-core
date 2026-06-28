package kz.mybrain.superkassa.core.domain.model.shift

import kotlinx.serialization.Serializable

/**
 * Информация о текущей или завершенной кассовой смене.
 *
 * @property id Уникальный идентификатор смены.
 * @property kkmId Идентификатор ККМ, к которой относится смена.
 * @property shiftNo Номер смены.
 * @property status Текущий статус смены (открыта/закрыта).
 * @property openedAt Временная метка открытия смены (в миллисекундах).
 * @property closedAt Временная метка закрытия смены (в миллисекундах, null если смена еще открыта).
 * @property openDocumentId Идентификатор фискального документа открытия смены.
 * @property closeDocumentId Идентификатор фискального документа закрытия смены.
 */
@Serializable
data class ShiftInfo(
    val id: String,
    val kkmId: String,
    val shiftNo: Long,
    val status: ShiftStatus,
    val openedAt: Long,
    val closedAt: Long? = null,
    val openDocumentId: String? = null,
    val closeDocumentId: String? = null
)
