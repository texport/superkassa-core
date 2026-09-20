package io.github.texport.superkassa.core.domain.api.model.receipt

import io.github.texport.superkassa.core.domain.api.model.common.Money
import kotlinx.serialization.Serializable

/**
 * Отраслевые реквизиты чека.
 *
 * Вид отрасли определяет, какой подблок обязателен: услуги и гостиницы —
 * [services], нефтепродукты — [gasOil], такси — [taxi], стоянки — [parking].
 * Торговля не требует ни одного. Подблок должен быть ровно один: лишний
 * означает противоречивый чек и отвергается получателем.
 */
@Serializable
data class ReceiptDomain(
    val type: ReceiptDomainType,
    val services: Services? = null,
    val gasOil: GasOil? = null,
    val taxi: Taxi? = null,
    val parking: Parking? = null
) {
    /** Сфера услуг и гостиницы: номер лицевого счёта. */
    @Serializable
    data class Services(val accountNumber: String)

    /** Обеспечение нефтепродуктами. */
    @Serializable
    data class GasOil(
        val correctionNumber: String? = null,
        val correctionSum: Money? = null,
        val cardNumber: String? = null
    )

    /** Такси: номер машины, признак заказа и текущий тариф. */
    @Serializable
    data class Taxi(val carNumber: String, val isOrder: Boolean, val currentFee: Money)

    /** Стоянка: время въезда и выезда в миллисекундах. */
    @Serializable
    data class Parking(val beginTimeMillis: Long, val endTimeMillis: Long)
}

/** Вид отрасли. Значения совпадают с DomainTypeEnum протокола. */
@Serializable
enum class ReceiptDomainType {
    DOMAIN_TRADING,
    DOMAIN_SERVICES,
    DOMAIN_GASOIL,
    DOMAIN_HOTELS,
    DOMAIN_TAXI,
    DOMAIN_PARKING
}
