package kz.mybrain.superkassa.core.domain.model.receipt

import kotlinx.serialization.Serializable
import kz.mybrain.superkassa.core.domain.model.common.Money
import kz.mybrain.superkassa.core.domain.model.common.TaxRegime
import kz.mybrain.superkassa.core.domain.model.common.VatGroup

/**
 * Запрос на формирование фискального чека.
 *
 * @property kkmId Идентификатор кассы (ККМ), регистрирующей чек.
 * @property pin ПИН-код пользователя для авторизации операции.
 * @property operation Тип фискальной операции (продажа, возврат и т.д.).
 * @property items Список позиций (товаров/услуг) в чеке.
 * @property payments Список платежей по чеку.
 * @property total Итоговая сумма по чеку.
 * @property taken Сумма принятых от клиента денежных средств.
 * @property change Сумма выданной сдачи.
 * @property idempotencyKey Уникальный ключ идемпотентности запроса.
 * @property parentTicket Ссылка на родительский чек (заполняется для операций возврата).
 * @property taxRegime Налоговый режим, применяемый к чеку (по умолчанию NO_VAT).
 * @property defaultVatGroup Ставка НДС по умолчанию на весь чек (если null, используется ставка по умолчанию для ККМ).
 * @property discount Сумма скидки на весь чек.
 * @property markup Сумма наценки на весь чек.
 * @property customerBin БИН/ИИН покупателя (если требуется указание).
 * @property ticketTaxes Рассчитанные строки распределения налогов по чеку (заполняются во внутреннем Use Case).
 */
@Serializable
data class ReceiptRequest(
    val kkmId: String,
    val pin: String,
    val operation: ReceiptOperationType,
    val items: List<ReceiptItem>,
    val payments: List<ReceiptPayment>,
    val total: Money,
    val taken: Money? = null,
    val change: Money? = null,
    val idempotencyKey: String,
    val parentTicket: ParentTicket? = null,
    val taxRegime: TaxRegime = TaxRegime.NO_VAT,
    val defaultVatGroup: VatGroup? = null,
    val discount: Money? = null,
    val markup: Money? = null,
    val customerBin: String? = null,
    val ticketTaxes: List<TaxLine>? = null
)
