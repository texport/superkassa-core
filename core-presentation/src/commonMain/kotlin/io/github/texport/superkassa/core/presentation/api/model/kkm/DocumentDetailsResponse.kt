package io.github.texport.superkassa.core.presentation.api.model.kkm

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptItemView
import kotlinx.serialization.Serializable

/**
 * Документ вместе с его составом.
 *
 * Списку документов состав не нужен — там сотни строк за смену, — а вот
 * возврату он нужен обязательно: вернуть можно только то, что было
 * продано, и в том количестве, в каком было продано. Кассир, разбирающий
 * отказ ОФД, по тем же причинам должен видеть, кто пробил документ.
 *
 * @property document Сам документ: номер, суммы, состояние доставки.
 * @property items Позиции чека; у отчётов и операций с наличными пусто.
 * @property operatorName Кто оформил документ.
 */
@Serializable
@Schema(description = "Документ с составом чека")
data class DocumentDetailsResponse(
    @Schema(description = "Документ") val document: FiscalDocumentResponse,
    @Schema(description = "Позиции чека") val items: List<ReceiptItemView> = emptyList(),
    @Schema(description = "Кто оформил документ") val operatorName: String? = null
)
