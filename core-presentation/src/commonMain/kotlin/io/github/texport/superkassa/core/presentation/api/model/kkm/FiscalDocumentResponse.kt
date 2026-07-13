package io.github.texport.superkassa.core.presentation.api.model.kkm

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Слепок фискального документа для хранения истории и отображения.
 */
@Serializable
@Schema(description = "Краткая информация о фискальном документе")
data class FiscalDocumentResponse(
    @Schema(description = "ID документа", example = "doc-uuid") val id: String,
    @Schema(description = "ID кассы (ККМ)", example = "kkm-uuid") val cashboxId: String,
    @Schema(description = "ID смены", example = "shift-uuid") val shiftId: String,
    @Schema(description = "Тип документа (например, TICKET, REPORT)", example = "TICKET") val docType: String,
    @Schema(description = "Порядковый номер документа", example = "101") val docNo: Long?,
    @Schema(description = "Номер смены", example = "10") val shiftNo: Long?,
    @Schema(description = "Время создания (epoch ms)", example = "1700000000000") val createdAt: Long,
    @Schema(description = "Итоговая сумма по документу (в тиынах)", example = "150000") val totalAmount: Long?,
    @Schema(description = "Валюта документа (например, KZT)", example = "KZT") val currency: String?,
    @Schema(description = "Фискальный признак (подпись) документа", example = "3810283") val fiscalSign: String?,
    @Schema(description = "Автономный признак документа", example = "2837192") val autonomousSign: String?,
    @Schema(description = "Оформлен ли документ в автономном режиме", example = "false") val isAutonomous: Boolean,
    @Schema(description = "Статус доставки в ОФД", example = "DELIVERED") val ofdStatus: String?,
    @Schema(description = "Время доставки документа в ОФД (epoch ms)", example = "1700000000000") val deliveredAt: Long?,
    @Schema(description = "Ссылка на электронный чек на сервере ОФД", example = "http://ofd.example.com/receipt/123") val receiptUrl: String? = null,
    @Schema(description = "Регистрационный номер ККМ", example = "123456789012") val registrationNumber: String? = null,
    @Schema(description = "Наименование налогоплательщика", example = "TOO Example") val taxpayerName: String? = null,
    @Schema(description = "БИН/ИИН налогоплательщика", example = "123456789012") val taxpayerBin: String? = null,
    @Schema(description = "Адрес использования ККМ", example = "Алматы, Абая 1") val taxpayerAddress: String? = null,
    @Schema(description = "Заводской номер ККМ", example = "SWK-0001") val factoryNumber: String? = null,
    @Schema(description = "Код провайдера ОФД", example = "kazakhtelecom") val ofdProvider: String? = null
)
