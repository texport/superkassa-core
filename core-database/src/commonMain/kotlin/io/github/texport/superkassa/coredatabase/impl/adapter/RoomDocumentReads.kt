package io.github.texport.superkassa.coredatabase.impl.adapter

import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptDocumentTypes
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptStoredPayload
import io.github.texport.superkassa.core.domain.impl.logging.getLogger
import io.github.texport.superkassa.coredatabase.impl.dao.FiscalDocumentDao
import io.github.texport.superkassa.coredatabase.impl.dao.KkmDao
import io.github.texport.superkassa.coredatabase.impl.entity.FiscalDocumentEntity
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException

/**
 * Чтение фискальных документов — так же, как их читает узел.
 *
 * Реквизиты кассы (номер КГД, заводской номер, БИН, название и адрес
 * налогоплательщика, ОФД) берутся у кассы в момент чтения, а не из
 * записи документа: так документ, перенесённый из узла, и свой читаются
 * одинаково, и у чека те же реквизиты, что у внесения.
 */
internal class RoomDocumentReads(private val dao: FiscalDocumentDao, private val kkms: KkmDao) {
    private val logger = getLogger(RoomDocumentReads::class)

    fun byId(id: String): FiscalDocumentSnapshot? = runBlocking {
        dao.getById(id)?.let { withCashbox(listOf(it)).single() }
    }

    fun byShift(kkmId: String, shiftId: String, limit: Int, offset: Int): List<FiscalDocumentSnapshot> = runBlocking {
        withCashbox(dao.listByShift(kkmId, shiftId, limit, offset))
    }

    fun byPeriod(kkmId: String, from: Long, until: Long, limit: Int, offset: Int): List<FiscalDocumentSnapshot> =
        runBlocking { withCashbox(dao.listByPeriod(kkmId, from, until, limit, offset)) }

    fun firstPaymentTime(shiftId: String): Long? = runBlocking {
        dao.firstPaymentTime(shiftId, ReceiptDocumentTypes.ALL)
    }

    fun count(docType: String?): Long = runBlocking {
        val docs = dao.listByPeriod("", 0, Long.MAX_VALUE, Int.MAX_VALUE, 0)
        docs.count { docType == null || it.docType == docType }.toLong()
    }

    /**
     * Документ-чек и чек, из которого он собран.
     *
     * Нет чека — нет и ответа, как у узла: подставной чек без позиций
     * и оплат ушёл бы в ОФД фискальным документом, а пересчёт смены
     * сложил бы его итог. Документ, который чеком не является, — тоже `null`.
     */
    fun withReceipt(documentId: String): Pair<FiscalDocumentSnapshot, ReceiptRequest>? = runBlocking {
        val row = dao.getById(documentId) ?: return@runBlocking null
        if (row.docType !in ReceiptDocumentTypes.ALL) return@runBlocking null
        val receipt = receiptOf(row) ?: return@runBlocking null
        withCashbox(listOf(row)).single() to receipt
    }

    private fun receiptOf(row: FiscalDocumentEntity): ReceiptRequest? {
        val raw = row.receiptPayloadJson?.takeIf { it.isNotBlank() } ?: return null
        return try {
            val stored = StoredReceiptJson.decodeFromString<ReceiptStoredPayload>(raw)
            // toReceiptRequest очищает ключ повтора как конфиденциальный;
            // хранилище его хранит и возвращает само.
            stored.toReceiptRequest().copy(idempotencyKey = stored.idempotencyKey)
        } catch (e: SerializationException) {
            logger.warn("Stored receipt of document {} cannot be parsed: {}", row.id, e::class.simpleName)
            null
        }
    }

    /** Документы одной выборки принадлежат одной кассе: её реквизиты читаются один раз. */
    private suspend fun withCashbox(rows: List<FiscalDocumentEntity>): List<FiscalDocumentSnapshot> {
        val cashboxes = rows.map { it.cashboxId }.distinct().associateWith { kkms.getById(it)?.toDomain() }
        return rows.map { row ->
            val kkm = cashboxes[row.cashboxId]
            val service = kkm?.ofdServiceInfo
            row.toDomain().copy(
                registrationNumber = kkm?.registrationNumber,
                taxpayerName = service?.orgTitle,
                taxpayerBin = service?.orgInn,
                taxpayerAddress = service?.orgAddress,
                factoryNumber = kkm?.factoryNumber,
                ofdProvider = kkm?.ofdProvider
            )
        }
    }
}
