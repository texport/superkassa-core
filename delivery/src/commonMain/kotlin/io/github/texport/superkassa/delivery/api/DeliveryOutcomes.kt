package io.github.texport.superkassa.delivery.api

import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryFailure
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryOutcome
import io.github.texport.superkassa.core.domain.api.port.integration.DeliveryPort
import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.string.api.TrilingualMessage
import io.github.texport.superkassa.delivery.api.model.DeliveryResult

/**
 * Итог канала — итогом доставки ядра: код, текст на трёх языках и то,
 * стоит ли повторять.
 *
 * Каналы отдают текст строкой трёх языков ([TrilingualMessage.compact]);
 * строка другого вида показывается как есть на всех трёх. Отказ без
 * кода получает код [DeliveryPort.UNEXPLAINED_FAILURE].
 *
 * @param channel имя канала — для текста отказа без причины.
 */
fun DeliveryResult.toOutcome(channel: String): DeliveryOutcome {
    if (ok) return DeliveryOutcome.DELIVERED
    val text = message?.let { TrilingualMessage.ofCompact(it) ?: TrilingualMessage.mono(it) }
        ?: CoreStrings.deliveryFailedWithoutReason(channel)
    return DeliveryOutcome.failed(DeliveryFailure(code ?: DeliveryPort.UNEXPLAINED_FAILURE, text), retryable)
}
