package io.github.texport.superkassa.receiptrenderer.impl.renderer.ticket

import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdEnvironment
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdProvider
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Ссылка проверки чека, собранная кассой самостоятельно.
 *
 * Ссылку на сетевой чек присылает ОФД. У чека, пробитого в разрыве связи,
 * её взять неоткуда, а чек без QR-кода покупателю проверить нечем — КГД
 * требует код на каждом чеке. Поэтому касса собирает ссылку сама из того,
 * что уже знает: свой номер документа, регистрационный номер, сумма и время.
 * После досылки чек находится по тому же номеру, и ссылка оживает.
 *
 * Порядок параметров тот же, что у касс парка: `i` — номер документа,
 * `f` — регистрационный номер кассы, `s` — сумма, `t` — дата и время.
 */
internal object OfflineReceiptLink {

    /**
     * Собирает ссылку проверки для чека без ответа ОФД.
     *
     * @param doc снимок фискального документа.
     * @param totalTiyn сумма чека в тиынах.
     * @return ссылка либо `null`, если чего-то из обязательного нет.
     */
    fun of(doc: FiscalDocumentSnapshot, totalTiyn: Long): String? {
        val domain = checkDomain(doc.ofdProvider) ?: return null
        val registration = doc.registrationNumber?.takeIf { it.isNotBlank() } ?: return null
        val number = doc.docNo ?: return null
        val sum = "${totalTiyn / TIYN_IN_TENGE}.${(totalTiyn % TIYN_IN_TENGE).pad()}"
        return "https://$domain?i=$number&f=$registration&s=$sum&t=${stamp(doc.createdAt)}"
    }

    /**
     * Домен проверки чека того ОФД, за которым закреплена касса.
     *
     * @param tag тег провайдера вида `BFD:DEV`.
     */
    private fun checkDomain(tag: String?): String? {
        val parts = tag?.split(TAG_SEPARATOR) ?: return null
        if (parts.size != TAG_PARTS) return null
        val environment = OfdEnvironment.findEnvironment(parts[1]) ?: return null
        return OfdProvider.findProvider(parts[0])?.endpoints?.get(environment)?.checkDomain
    }

    /** Дата и время в виде `ГГГГММДДTччммсс`, как в ссылке от ОФД. */
    private fun stamp(millis: Long): String {
        val moment = Instant.fromEpochMilliseconds(millis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        val date = "${moment.year}${(moment.month.ordinal + 1).pad()}${moment.day.pad()}"
        val time = "${moment.hour.pad()}${moment.minute.pad()}${moment.second.pad()}"
        return "${date}T$time"
    }

    private fun Int.pad(): String = toString().padStart(TWO_DIGITS, '0')

    private fun Long.pad(): String = toString().padStart(TWO_DIGITS, '0')

    private const val TIYN_IN_TENGE = 100L
    private const val TWO_DIGITS = 2
    private const val TAG_SEPARATOR = ':'
    private const val TAG_PARTS = 2
}
