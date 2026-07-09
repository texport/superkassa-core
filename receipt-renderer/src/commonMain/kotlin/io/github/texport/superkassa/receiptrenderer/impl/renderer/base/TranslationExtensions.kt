package io.github.texport.superkassa.receiptrenderer.impl.renderer.base

import io.github.texport.superkassa.core.domain.model.common.*
import io.github.texport.superkassa.core.domain.model.receipt.*

val VatGroup.translationKey: String
    get() = when (this) {
        VatGroup.NO_VAT -> "vat_no_vat"
        VatGroup.VAT_0 -> "vat_0"
        VatGroup.VAT_5 -> "vat_5"
        VatGroup.VAT_10 -> "vat_10"
        VatGroup.VAT_16 -> "vat_16"
    }

val PaymentType.translationKey: String
    get() = when (this) {
        PaymentType.CASH -> "cash"
        PaymentType.CARD -> "card"
        PaymentType.ELECTRONIC -> "electronic"
        PaymentType.MOBILE -> "mobile"
    }

val ReceiptOperationType.translationKey: String
    get() = when (this) {
        ReceiptOperationType.SELL -> "sell_receipt"
        ReceiptOperationType.SELL_RETURN -> "sell_return_receipt"
        ReceiptOperationType.BUY -> "buy_receipt"
        ReceiptOperationType.BUY_RETURN -> "buy_return_receipt"
    }

fun String.toOperationKey(): String = when (this) {
    "OPERATION_SELL" -> "operation_sell"
    "OPERATION_SELL_RETURN" -> "operation_sell_return"
    "OPERATION_BUY" -> "operation_buy"
    "OPERATION_BUY_RETURN" -> "operation_buy_return"
    else -> this
}

fun String.toPaymentKey(): String = when (this) {
    "PAYMENT_CASH" -> "cash"
    "PAYMENT_CARD" -> "card"
    "PAYMENT_CREDIT" -> "credit"
    "PAYMENT_TARE" -> "tare"
    "PAYMENT_MOBILE" -> "mobile"
    "PAYMENT_ELECTRONIC" -> "electronic"
    else -> this
}

fun String.toTaxKey(): String = when (this) {
    "TAX_TYPE_VAT_0" -> "vat_0"
    "TAX_TYPE_VAT_5" -> "vat_5"
    "TAX_TYPE_VAT_10" -> "vat_10"
    "TAX_TYPE_VAT_12" -> "vat_12"
    "TAX_TYPE_VAT_16" -> "vat_16"
    "TAX_TYPE_NO_VAT" -> "vat_no_vat"
    else -> this
}

fun String.toDiscountKey(): String = when (this) {
    "OPERATION_SELL" -> "discount_sell"
    "OPERATION_SELL_RETURN" -> "discount_sell_return"
    "OPERATION_BUY" -> "discount_buy"
    "OPERATION_BUY_RETURN" -> "discount_buy_return"
    else -> this
}

fun String.toMarkupKey(): String = when (this) {
    "OPERATION_SELL" -> "markup_sell"
    "OPERATION_SELL_RETURN" -> "markup_sell_return"
    "OPERATION_BUY" -> "markup_buy"
    "OPERATION_BUY_RETURN" -> "markup_buy_return"
    else -> this
}

fun String.toTotalResultKey(): String = when (this) {
    "OPERATION_SELL" -> "total_sell"
    "OPERATION_SELL_RETURN" -> "total_sell_return"
    "OPERATION_BUY" -> "total_buy"
    "OPERATION_BUY_RETURN" -> "total_buy_return"
    else -> this
}
