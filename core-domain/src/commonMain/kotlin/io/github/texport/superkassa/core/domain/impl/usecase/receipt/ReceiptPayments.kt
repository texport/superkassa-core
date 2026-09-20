package io.github.texport.superkassa.core.domain.impl.usecase.receipt

import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.receipt.PaymentType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptPayment
import io.github.texport.superkassa.core.string.api.CoreStrings

/**
 * Оплаты чека и сдача с них.
 *
 * Один чек может быть оплачен несколькими видами сразу: часть картой,
 * часть наличными — обычный случай в магазине, и протокол принимает
 * список оплат, а не один вид.
 *
 * Отсюда две проверки, которых нет у чека с единственной оплатой.
 * Суммы оплат обязаны в точности сложиться в итог: иначе ОФД примет
 * документ, в котором заплачено не столько, сколько пробито, а увидят
 * расхождение на инкассации. И сдача считается от наличной части, а не
 * от итога чека: при оплате картой и наличными вместе сдача с итога
 * уходила в ноль, и покупатель недополучал свои деньги.
 */
internal fun paymentsOf(
    declared: List<CreateReceiptCommand.PaymentInput>,
    supported: Set<PaymentType>,
    protocolVersion: String
): List<ReceiptPayment> = declared.map { dto ->
    val paymentType = try {
        PaymentType.valueOf(dto.type)
    } catch (_: IllegalArgumentException) {
        throw IllegalArgumentException(
            "Invalid payment type: ${dto.type}. Valid: ${PaymentType.entries.joinToString { it.name }}"
        )
    }
    if (paymentType !in supported) {
        // Отказ до фискализации: иначе касса оформит документ,
        // который не примет ни ОФД сейчас, ни повтор из очереди.
        throw ValidationException(
            CoreStrings.paymentTypeNotSupported(paymentType.name, protocolVersion),
            "PAYMENT_TYPE_NOT_SUPPORTED"
        )
    }
    ReceiptPayment(type = paymentType, sum = Money.fromTenge(dto.sum))
}

/** Оплачено ровно столько, сколько пробито. */
internal fun requireBalanced(payments: List<ReceiptPayment>, totalTiyn: Long) {
    val paidTiyn = payments.sumOf { it.sum.tiyn() }
    if (paidTiyn != totalTiyn) {
        throw ValidationException(
            CoreStrings.paymentsTotalMismatch(tenge(paidTiyn), tenge(totalTiyn)),
            "PAYMENTS_TOTAL_MISMATCH"
        )
    }
}

/**
 * Принятые деньги и сдача.
 *
 * «Принято» относится только к наличным: карту покупатель не даёт
 * с запасом. Поэтому и сдача — разница между принятым и наличной
 * частью чека.
 */
internal fun takenAndChange(payments: List<ReceiptPayment>, taken: Decimal?): Pair<Money, Money?> {
    val cashTiyn = payments.filter { it.type == PaymentType.CASH }.sumOf { it.sum.tiyn() }
    if (taken == null) return Pair(Money.fromTiyn(cashTiyn), null)
    val takenTiyn = taken.scaled(TIYN_SCALE)
    require(takenTiyn >= cashTiyn) {
        "taken must be >= sum of CASH payments (cashSum=$cashTiyn, taken=$takenTiyn)"
    }
    return Pair(Money.fromTiyn(takenTiyn), Money.fromTiyn(takenTiyn - cashTiyn))
}

/** Тиыны словами кассира: «1234.56». */
private fun tenge(tiyn: Long): String {
    val whole = tiyn / Money.TIYN_IN_TENGE
    val part = (tiyn % Money.TIYN_IN_TENGE).toString().padStart(2, '0')
    return "$whole.$part"
}

/** Знаков после запятой у тенге. */
private const val TIYN_SCALE: Int = 2
