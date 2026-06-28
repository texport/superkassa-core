package kz.mybrain.superkassa.core.domain.model.receipt

import kotlinx.serialization.Serializable
import kz.mybrain.superkassa.core.domain.model.common.Money
import kz.mybrain.superkassa.core.domain.model.common.VatGroup

/**
 * Позиция чека (товар или услуга).
 *
 * @property name Наименование товара или услуги.
 * @property sectionCode Код (номер) товарной секции (по умолчанию "001").
 * @property quantity Количество товара (умноженное на 1000 для целочисленного представления, например 1 шт = 1000).
 * @property price Цена единицы товара.
 * @property sum Общая стоимость позиции с учетом количества.
 * @property barcode Штрих-код товара (EAN-13 и др.).
 * @property vatGroup Группа ставки НДС для позиции (если null, используется ставка по умолчанию для ККМ).
 * @property discount Скидка на позицию.
 * @property markup Наценка на позицию.
 * @property measureUnitCode Числовой код единицы измерения по ОКЕИ (если null, то 796 - штука).
 * @property listExciseStamp Список связанных акцизных марок.
 * @property ntin Национальный товарный код (НТИН).
 * @property isStorno Флаг сторнирования (аннулирования) позиции.
 */
@Serializable
data class ReceiptItem(
    val name: String,
    val sectionCode: String,
    val quantity: Long,
    val price: Money,
    val sum: Money,
    val barcode: String? = null,
    val vatGroup: VatGroup? = null,
    val discount: Money? = null,
    val markup: Money? = null,
    val measureUnitCode: String? = null,
    val listExciseStamp: List<String>? = null,
    val ntin: String? = null,
    val isStorno: Boolean = false
)
