package io.github.texport.superkassa.coredatabase.impl.adapter

import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptDocumentTypes
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptStoredPayload
import io.github.texport.superkassa.coredatabase.impl.dao.FiscalDocumentDao
import io.github.texport.superkassa.coredatabase.impl.dao.ShiftDao
import io.github.texport.superkassa.coredatabase.impl.entity.FiscalDocumentEntity
import kotlinx.coroutines.runBlocking

/**
 * Запись фискальных документов — так же, как их пишет узел.
 *
 * Итог — в тиынах. Тип чека — операция по справочнику узла (`SALE`,
 * `RETURN`, `BUY`, `BUY_RETURN`). Номера документа при записи нет: его
 * даёт ОФД в ответе или касса как автономный номер при разрыве связи.
 * Открытие смены в ОФД не уходит и потому внутреннее, а не «ждёт отправки».
 */
internal class RoomDocuments(private val dao: FiscalDocumentDao, private val shifts: ShiftDao) {

    fun saveReceipt(request: ReceiptRequest, documentId: String, shiftId: String, createdAt: Long): Boolean {
        val type = ReceiptDocumentTypes.of(request.operation)
        val payload = StoredReceiptJson.encodeToString(ReceiptStoredPayload.fromReceiptRequest(request))
        return insert(Draft(documentId, request.kkmId, shiftId, type, createdAt, request.total.tiyn()), payload)
    }

    fun saveCashOperation(kkmId: String, type: String, amount: Money, id: String, shiftId: String, at: Long): Boolean =
        insert(Draft(id, kkmId, shiftId, type, at, amount.tiyn()), payload = null)

    fun saveShiftDocument(kkmId: String, type: String, documentId: String, shiftId: String, createdAt: Long): Boolean =
        insert(Draft(documentId, kkmId, shiftId, type, createdAt, totalTiyn = 0L), payload = null)

    /**
     * Статус доставки: переданное записывается как есть, `null` у признаков
     * и кода отказа означает «нет», а не «оставить» — как у узла. Иначе
     * принятый после отказа документ так и показывал бы причину отказа.
     * Не трогаются только признак автономности без значения и ссылка на чек.
     */
    fun updateStatus(documentId: String, status: DeliveryUpdate): Boolean = change(documentId) {
        it.copy(
            fiscalSign = status.fiscalSign,
            autonomousSign = status.autonomousSign,
            ofdStatus = status.ofdStatus,
            ofdErrorCode = status.ofdErrorCode,
            ofdErrorText = status.ofdErrorText,
            deliveredAt = status.deliveredAt,
            isAutonomous = status.isAutonomous ?: it.isAutonomous
        )
    }

    fun saveReceiptUrl(documentId: String, receiptUrl: String): Boolean =
        change(documentId) { it.copy(receiptUrl = receiptUrl) }

    fun updateDocumentNumber(documentId: String, docNo: Long): Boolean = change(documentId) { it.copy(docNo = docNo) }

    fun updatePrintedNumber(documentId: String, number: Long): Boolean =
        change(documentId) { it.copy(printedDocumentNumber = number) }

    fun deleteByKkm(kkmId: String) = runBlocking { dao.deleteByKkm(kkmId) }

    private fun insert(draft: Draft, payload: String?): Boolean = runBlocking {
        val shiftNo = shifts.getById(draft.shiftId)?.shiftNo
        dao.insert(draft.toEntity(shiftNo, payload))
        true
    }

    /** Правка одной записи целиком: сохранённый чек и прочие поля остаются как были. */
    private fun change(id: String, edit: (FiscalDocumentEntity) -> FiscalDocumentEntity): Boolean = runBlocking {
        val current = dao.getById(id) ?: return@runBlocking false
        dao.insert(edit(current))
        true
    }

    /** Новый документ до записи: всё, что о нём известно в момент оформления. */
    private class Draft(
        val id: String,
        val kkmId: String,
        val shiftId: String,
        val type: String,
        val createdAt: Long,
        val totalTiyn: Long
    ) {
        fun toEntity(shiftNo: Long?, payload: String?) = FiscalDocumentEntity(
            id = id, cashboxId = kkmId, shiftId = shiftId, docType = type, docNo = null, shiftNo = shiftNo,
            createdAt = createdAt, totalAmount = totalTiyn, currency = CURRENCY,
            fiscalSign = null, autonomousSign = null, isAutonomous = false,
            ofdStatus = statusOf(type), ofdErrorCode = null, deliveredAt = null,
            registrationNumber = null, taxpayerName = null, taxpayerBin = null, taxpayerAddress = null,
            factoryNumber = null, ofdProvider = null, receiptPayloadJson = payload
        )
    }

    private companion object {
        const val CURRENCY = "KZT"
        const val SHIFT_OPEN = "SHIFT_OPEN"

        /** Документ, которому в ОФД дороги нет. */
        const val INTERNAL = "INTERNAL"

        /** Документ ждёт отправки. */
        const val PENDING = "PENDING"

        fun statusOf(type: String): String = if (type == SHIFT_OPEN) INTERNAL else PENDING
    }
}

/** Исход доставки документа, как его передаёт ядро. */
internal class DeliveryUpdate(
    val fiscalSign: String?,
    val autonomousSign: String?,
    val ofdStatus: String,
    val ofdErrorCode: Int?,
    val deliveredAt: Long?,
    val isAutonomous: Boolean?,
    val ofdErrorText: String?
)
