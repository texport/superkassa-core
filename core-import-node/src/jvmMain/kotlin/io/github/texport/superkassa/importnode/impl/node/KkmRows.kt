package io.github.texport.superkassa.importnode.impl.node

import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdServiceInfo
import io.github.texport.superkassa.core.domain.api.model.common.TaxRegime
import io.github.texport.superkassa.core.domain.api.model.common.VatGroup
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptBranding
import io.github.texport.superkassa.coredatabase.api.parseBrandingJson
import io.github.texport.superkassa.importnode.api.NodeImportException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Base64

/**
 * Касса узла (`cashbox`) — в модель кассы ядра.
 *
 * Разбор повторяет узел (`StorageMapper.mapKkm`): токен и хеш последнего
 * документа — base64 байтов колонки, сведения ОФД — JSON, неразборный
 * режим налога — «без НДС». Касса после переноса видит то же, что видел узел.
 * Оформление чека — исключение: неразборное оформление узел заменял
 * умолчанием, но хранил строку; перенос её не сохранит, поэтому отказывает.
 */
internal object KkmRows {

    const val SQL = "SELECT * FROM cashbox ORDER BY created_at, id"

    private val json = Json { ignoreUnknownKeys = true }

    fun kkm(row: NodeRow): KkmInfo = registration(row).copy(
        lastShiftNo = row.intOrNull("last_shift_no"),
        lastReceiptNo = row.intOrNull("last_receipt_no"),
        lastZReportNo = row.intOrNull("last_z_report_no"),
        autonomousSince = row.longOrNull("autonomous_since"),
        lastFiscalHashBase64 = row.blobOrNull("last_fiscal_hash")?.let(Base64.getEncoder()::encodeToString),
        blockReasonCode = row.intOrNull("block_reason_code")
    )

    /** Регистрация, токен и настройки кассы; фискальное состояние дописывает [kkm]. */
    private fun registration(row: NodeRow): KkmInfo = KkmInfo(
        id = row.text("id"),
        createdAt = row.long("created_at"),
        updatedAt = row.long("updated_at"),
        mode = row.text("mode"),
        state = row.text("state"),
        ofdProvider = row.textOrNull("ofd_provider"),
        registrationNumber = row.textOrNull("registration_number"),
        factoryNumber = row.textOrNull("factory_number"),
        manufactureYear = row.intOrNull("manufacture_year"),
        systemId = row.textOrNull("system_id"),
        ofdServiceInfo = serviceInfo(row.textOrNull("ofd_service_info")),
        tokenEncryptedBase64 = row.blobOrNull("token_enc")?.let(Base64.getEncoder()::encodeToString),
        tokenUpdatedAt = row.longOrNull("token_updated_at"),
        autoCloseShift = row.bool("auto_close_shift"),
        autoCashout = row.bool("auto_cashout"),
        taxRegime = enumOr(row.textOrNull("tax_regime"), TaxRegime.NO_VAT),
        defaultVatGroup = enumOr(row.textOrNull("default_vat_group"), VatGroup.NO_VAT),
        name = row.textOrNull("name"),
        branding = branding(row.text("id"), row.textOrNull("branding_json"))
    )

    /** Оформление чека кассы; пустая колонка — оформление по умолчанию, как у узла. */
    private fun branding(id: String, payload: String?): ReceiptBranding {
        if (payload.isNullOrBlank()) return ReceiptBranding()
        return try {
            parseBrandingJson(payload)
        } catch (e: IllegalArgumentException) {
            // Причина без текста исключения разбора: он несёт кусок самой строки.
            throw NodeImportException("Receipt branding of cash register $id cannot be read: ${e::class.simpleName}")
        }
    }

    private fun serviceInfo(payload: String?): OfdServiceInfo? = payload?.takeIf { it.isNotBlank() }?.let {
        runCatching { json.decodeFromString(ServiceInfoJson.serializer(), it).toDomain() }.getOrNull()
    }

    private inline fun <reified T : Enum<T>> enumOr(value: String?, default: T): T =
        value?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: default

    /** Сведения ОФД так, как их пишет узел. */
    @Serializable
    private data class ServiceInfoJson(
        val orgTitle: String,
        val orgAddress: String,
        val orgAddressKz: String,
        val orgInn: String,
        val orgOkved: String,
        val geoLatitude: Int,
        val geoLongitude: Int,
        val geoSource: String
    ) {
        fun toDomain() = OfdServiceInfo(
            orgTitle,
            orgAddress,
            orgAddressKz,
            orgInn,
            orgOkved,
            geoLatitude,
            geoLongitude,
            geoSource
        )
    }
}
