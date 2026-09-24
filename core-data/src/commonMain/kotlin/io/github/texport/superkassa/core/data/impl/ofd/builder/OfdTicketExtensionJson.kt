package io.github.texport.superkassa.core.data.impl.ofd.builder

import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Сведения о покупателе в чеке БФД — `TicketRequest.extension_options`.
 *
 * По CPCR все три поля необязательны и есть во всех обслуживаемых
 * версиях (2.0.2, 2.0.3, 2.0.4): `customer_email` (1), `customer_phone` (2),
 * `customer_iin_or_bin` (4, с версии 2.0.1). Формата телефона и почты
 * спецификация не задаёт, поэтому контакт уходит так, как его записал
 * кассир. Поля для Telegram в протоколе нет — этот контакт остаётся
 * только каналу доставки.
 *
 * Прежде в БФД уходил только БИН/ИИН покупателя: телефон и почта, по
 * которым касса сама отправляла чек, до фискального документа не доходили.
 * Контакт — персональные данные: сюда он попадает только в тело запроса,
 * журнал обмена тел запросов не пишет.
 */
internal object OfdTicketExtensionJson {

    /** Расширения чека или `null`, если о покупателе нечего сообщить. */
    fun of(request: ReceiptRequest): JsonObject? {
        val contact = request.customerContact
        val fields = listOfNotNull(
            contact?.email.filled()?.let { "customerEmail" to it },
            contact?.phone.filled()?.let { "customerPhone" to it },
            request.customerBin.filled()?.let { "customerIinOrBin" to it }
        )
        if (fields.isEmpty()) return null
        return buildJsonObject { fields.forEach { (name, value) -> put(name, JsonPrimitive(value)) } }
    }

    /** Пустое необязательное поле кодек БФД отвергает: поле либо заполнено, либо его нет. */
    private fun String?.filled(): String? = this?.takeIf { it.isNotBlank() }
}
