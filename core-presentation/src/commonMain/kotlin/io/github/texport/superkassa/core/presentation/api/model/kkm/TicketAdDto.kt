package io.github.texport.superkassa.core.presentation.api.model.kkm

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Рекламная строка ОФД в том виде, в каком её отдаёт и принимает API.
 *
 * Вид и версия нужны не для печати: их касса сообщает ОФД, чтобы получить
 * только новые объявления.
 */
@Serializable
@Schema(description = "Рекламная строка ОФД")
data class TicketAdDto(
    @Schema(description = "Вид объявления по CPCR", example = "TICKET_AD_OFD")
    val type: String = "TICKET_AD_OFD",
    @Schema(description = "Версия объявления этого вида", example = "17")
    val version: Long = 0L,
    @Schema(description = "Печатаемая строка", example = "Проверьте чек на kgd.gov.kz")
    val text: String
)
