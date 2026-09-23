package io.github.texport.superkassa.core.domain.impl.usecase.print.protocol

import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import kotlinx.serialization.json.JsonObject
import kotlin.io.encoding.Base64

/**
 * Общая часть документа: номер, смена, момент, сумма и реквизиты кассы.
 *
 * Смена как запись этой машины сюда не попадает: документ пробит
 * не здесь, и связывать его с чужой сменой нечем. Номер смены при этом
 * есть — он приходит в самом документе и печатается в шапке.
 *
 * @param body блок документа в запросе: чек или движение денег.
 * @param kkm касса, которой рисуется документ, с реквизитами из пакета.
 * @param type вид документа в журнале кассы.
 * @param totalTiyn сумма документа в тиынах.
 * @param ofdStatus отметка о передаче в ОФД.
 */
internal fun snapshotOf(
    body: JsonObject,
    kkm: KkmInfo,
    type: String,
    totalTiyn: Long,
    ofdStatus: String
): FiscalDocumentSnapshot {
    val number = body.number("printedDocumentNumber")
    return FiscalDocumentSnapshot(
        id = number?.toString().orEmpty(),
        cashboxId = kkm.id,
        shiftId = "",
        docType = type,
        docNo = number,
        printedDocumentNumber = number,
        shiftNo = body.number("frShiftNumber"),
        createdAt = body.moment("dateTime") ?: 0L,
        totalAmount = totalTiyn,
        currency = CURRENCY,
        fiscalSign = null,
        autonomousSign = null,
        isAutonomous = false,
        ofdStatus = ofdStatus,
        deliveredAt = null,
        registrationNumber = kkm.registrationNumber,
        taxpayerName = kkm.ofdServiceInfo?.orgTitle,
        taxpayerBin = kkm.ofdServiceInfo?.orgIinOrBin,
        taxpayerAddress = kkm.ofdServiceInfo?.orgAddress,
        factoryNumber = kkm.factoryNumber
    )
}

/**
 * Ссылка на чек у ОФД.
 *
 * В протоколе она лежит двоичным полем, и в JSON попадает записанной
 * по основанию 64. Раскодированное берётся, только если это ссылка:
 * иначе под QR-кодом чека встала бы строка случайных байтов.
 */
internal fun receiptLinkOf(answer: JsonObject): String? {
    val raw = answer.text("qrCode") ?: answer.text("qrCodeBase64") ?: return null
    val decoded = runCatching { BASE64.decode(raw).decodeToString() }.getOrNull()
    return decoded?.takeIf { it.startsWith(LINK) } ?: raw.takeIf { it.startsWith(LINK) }
}

/** Основание 64 с необязательным выравниванием: так его читает и JVM узла. */
private val BASE64 = Base64.Default.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL)

/** Начало ссылки на чек: по нему раскодированное отличается от мусора. */
private const val LINK = "http"

/** Валюта документа: тенге и его сотая доля — тиын. */
private const val CURRENCY = "KZT"
