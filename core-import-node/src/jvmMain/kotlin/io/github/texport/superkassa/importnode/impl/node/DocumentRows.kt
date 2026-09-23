package io.github.texport.superkassa.importnode.impl.node

import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptStoredPayload
import io.github.texport.superkassa.coredatabase.api.StoredDocument
import io.github.texport.superkassa.importnode.api.NodeImportException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Фискальный документ узла (`fiscal_document`) — как есть.
 *
 * Тип, номер (у неотправленного его нет), итог в тиынах и статус ОФД
 * переносятся без пересчёта. Реквизиты налогоплательщика узел в документе
 * не хранит, а подставляет от кассы при чтении (`StorageMapper.toFiscalDocumentSnapshot`);
 * здесь так же.
 *
 * Чек документа узел пишет JSON-ом того же `ReceiptStoredPayload`. Чек,
 * который не разбирается, — отказ переноса: без позиций и оплат документ
 * нельзя ни дослать в ОФД, ни пересчитать смену.
 */
internal class DocumentRows(private val kkms: Map<String, KkmInfo>) {

    fun document(row: NodeRow): StoredDocument {
        val id = row.text("id")
        return StoredDocument(snapshot(id, row), receipt(id, row.blobOrNull("payload_bin")))
    }

    private fun snapshot(id: String, row: NodeRow): FiscalDocumentSnapshot = FiscalDocumentSnapshot(
        id = id,
        cashboxId = row.text("cashbox_id"),
        shiftId = row.textOrNull("shift_id") ?: "",
        docType = row.text("doc_type"),
        docNo = row.longOrNull("doc_no"),
        printedDocumentNumber = row.longOrNull("printed_document_number"),
        shiftNo = row.longOrNull("shift_no"),
        createdAt = row.long("created_at"),
        totalAmount = row.longOrNull("total_amount"),
        currency = row.textOrNull("currency"),
        fiscalSign = row.textOrNull("fiscal_sign"),
        autonomousSign = row.textOrNull("autonomous_sign"),
        isAutonomous = row.bool("is_autonomous"),
        ofdStatus = row.textOrNull("ofd_status"),
        ofdErrorCode = row.intOrNull("ofd_error_code"),
        ofdErrorText = row.textOrNull("ofd_error_text"),
        deliveredAt = row.longOrNull("delivered_at"),
        receiptUrl = row.textOrNull("receipt_url")
    ).withTaxpayer()

    private fun FiscalDocumentSnapshot.withTaxpayer(): FiscalDocumentSnapshot {
        val kkm = kkms[cashboxId]
        return copy(
            registrationNumber = kkm?.registrationNumber,
            taxpayerName = kkm?.ofdServiceInfo?.orgTitle,
            taxpayerBin = kkm?.ofdServiceInfo?.orgInn,
            taxpayerAddress = kkm?.ofdServiceInfo?.orgAddress,
            factoryNumber = kkm?.factoryNumber,
            ofdProvider = kkm?.ofdProvider
        )
    }

    private fun receipt(id: String, payload: ByteArray?): ReceiptStoredPayload? {
        if (payload == null || payload.isEmpty()) return null
        return try {
            json.decodeFromString(ReceiptStoredPayload.serializer(), payload.decodeToString())
        } catch (e: SerializationException) {
            // Причина без исключения разбора: его текст несёт кусок самого чека.
            throw NodeImportException("Receipt of document $id cannot be read: ${e::class.simpleName}")
        }
    }

    companion object {
        const val SQL = "SELECT * FROM fiscal_document ORDER BY cashbox_id, created_at, id"

        private val json = Json { ignoreUnknownKeys = true }
    }
}
