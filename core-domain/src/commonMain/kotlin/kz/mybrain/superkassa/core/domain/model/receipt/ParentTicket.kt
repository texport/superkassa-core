package kz.mybrain.superkassa.core.domain.model.receipt

import kotlinx.serialization.Serializable
import kz.mybrain.superkassa.core.domain.model.common.Money

/**
 * Ссылка на родительский чек (для чеков возврата).
 * Описание исходного чека для операций возврата (соответствует parentTicket в протоколе).
 *
 * @property parentTicketNumber Номер исходного чека.
 * @property parentTicketDateTimeMillis Время оформления исходного чека (в миллисекундах).
 * @property kgdKkmId Идентификатор ККМ в КГД, на которой был оформлен исходный чек.
 * @property parentTicketTotal Полная сумма исходного чека.
 * @property parentTicketIsOffline Признак того, был ли исходный чек оформлен в автономном режиме.
 */
@Serializable
data class ParentTicket(
    val parentTicketNumber: Long,
    val parentTicketDateTimeMillis: Long,
    val kgdKkmId: String,
    val parentTicketTotal: Money,
    val parentTicketIsOffline: Boolean
)
