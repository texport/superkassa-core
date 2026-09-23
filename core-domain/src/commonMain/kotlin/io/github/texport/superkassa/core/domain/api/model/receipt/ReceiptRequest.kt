package io.github.texport.superkassa.core.domain.api.model.receipt

import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.common.TaxRegime
import io.github.texport.superkassa.core.domain.api.model.common.VatGroup

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
 * @property defaultVatGroup Ставка кассы: ею облагается позиция без своей ставки, когда НДС задан по позициям.
 * @property vatGroup НДС на весь чек: одна ставка на весь итог чека, позиции своих ставок не несут.
 * `null` — НДС по позициям. Способы взаимоисключающие, как скидка на чек и на позиции.
 * @property discount Сумма скидки на весь чек.
 * @property markup Сумма наценки на весь чек.
 * @property customerBin БИН/ИИН покупателя (если требуется указание).
 * @property ticketTaxes Рассчитанные строки распределения налогов по чеку (заполняются во внутреннем Use Case).
 */
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
    /** Отраслевые реквизиты чека, если отрасль объявлена. */
    val domain: ReceiptDomain? = null,
    val taxRegime: TaxRegime = TaxRegime.NO_VAT,
    val defaultVatGroup: VatGroup? = null,
    val vatGroup: VatGroup? = null,
    val discount: Money? = null,
    val markup: Money? = null,
    val customerBin: String? = null,
    val ticketTaxes: List<TaxLine>? = null,
    /**
     * Кто оформил чек.
     *
     * Имя, а не пин: пин на диск не пишется никогда, а разбор отказа ОФД
     * без имени кассира упирается в «кто-то пробил в 14:53».
     */
    val operatorName: String? = null
)
