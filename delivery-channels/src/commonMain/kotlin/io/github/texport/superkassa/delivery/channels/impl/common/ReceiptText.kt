package io.github.texport.superkassa.delivery.channels.impl.common

import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.delivery.api.model.DeliveryRequest

/**
 * Текст сообщения покупателю: ссылка на чек, а без ссылки — номер готового чека.
 *
 * На русском, как у узла: язык покупателя касса не знает, а отправлять
 * три языка в одном SMS значит втрое платить за сообщение.
 */
internal fun receiptText(request: DeliveryRequest): String {
    val url = request.payloadUrl
    val text = if (url.isNullOrBlank()) {
        CoreStrings.receiptReadyText(request.documentId)
    } else {
        CoreStrings.receiptLinkText(url)
    }
    return text.ru
}
