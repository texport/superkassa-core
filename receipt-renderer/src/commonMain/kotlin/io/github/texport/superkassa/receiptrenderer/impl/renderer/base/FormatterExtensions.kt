package io.github.texport.superkassa.receiptrenderer.impl.renderer.base

import io.github.texport.superkassa.core.domain.api.model.common.*
import io.github.texport.superkassa.receiptrenderer.impl.ReceiptFormatter

fun Money.formatted(): String {
    return ReceiptFormatter.formatMoney(this)
}

fun String.escaped(): String {
    return ReceiptFormatter.escape(this)
}

fun Long.formatQuantity(): String {
    return ReceiptFormatter.formatQuantity(this)
}
