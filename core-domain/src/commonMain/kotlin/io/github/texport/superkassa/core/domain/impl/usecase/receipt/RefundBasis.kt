package io.github.texport.superkassa.core.domain.impl.usecase.receipt

import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.kkm.becameFiscal
import io.github.texport.superkassa.core.domain.api.model.receipt.ParentTicket
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort

/**
 * Чек-основание возврата среди документов этой кассы.
 *
 * Ищется по документам от даты основания до сегодняшнего дня: возврат
 * бывает и в другой смене, а ссылка на основание лежит в самом чеке.
 * Учитываются только документы, ставшие фискальными.
 */
internal class RefundBasis(private val storage: StoragePort) {

    /** Сколько по этому чеку-основанию уже возвращено, в тиынах. */
    fun refundedAgainst(kkmId: String, basis: ParentTicket): Long = receiptsSince(kkmId, basis)
        .filter { (_, receipt) -> receipt.parentTicket?.parentTicketNumber == basis.parentTicketNumber }
        .sumOf { (_, receipt) -> receipt.total.tiyn() }

    /**
     * Сам чек-основание, если он пробит на этой кассе.
     *
     * @param kgdNumber регистрационный номер кассы в КГД: основание с другой
     * кассы здесь не ищется, у неё свой счёт номеров.
     * @return сохранённый чек либо `null`, если его здесь нет.
     */
    fun find(kkmId: String, kgdNumber: String?, basis: ParentTicket): ReceiptRequest? {
        if (kgdNumber == null || kgdNumber != basis.kgdKkmId) return null
        return receiptsSince(kkmId, basis)
            .firstOrNull { (doc, receipt) -> doc.docNo == basis.parentTicketNumber && receipt.parentTicket == null }
            ?.second
    }

    private fun receiptsSince(kkmId: String, basis: ParentTicket): Sequence<Pair<FiscalDocumentSnapshot, ReceiptRequest>> =
        sequence {
            var offset = 0
            while (true) {
                val page = storage.listFiscalDocumentsByPeriod(
                    kkmId = kkmId,
                    fromInclusive = basis.parentTicketDateTimeMillis,
                    toExclusive = Long.MAX_VALUE,
                    limit = PAGE,
                    offset = offset
                )
                if (page.isEmpty()) return@sequence
                page.filter { it.becameFiscal() }.forEach { doc ->
                    storage.findFiscalDocumentWithReceiptPayload(doc.id)?.let { yield(it) }
                }
                offset += PAGE
            }
        }
}

/** Сколько документов читается за один заход. */
private const val PAGE = 200
