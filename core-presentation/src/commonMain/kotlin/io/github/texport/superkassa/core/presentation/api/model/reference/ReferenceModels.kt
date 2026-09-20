package io.github.texport.superkassa.core.presentation.api.model.reference

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import io.github.texport.superkassa.core.string.api.TrilingualMessage
import kotlinx.serialization.Serializable

@Serializable
@Schema(description = "Элемент справочника типов оплат")
data class PaymentTypeResponse(
    @Schema(description = "Код типа оплаты", example = "CASH") val code: String,
    @Schema(description = "Локализованное название") val name: TrilingualMessageResponse,
    /**
     * Принимает ли действующая версия протокола такой вид оплаты.
     *
     * Кредит и тара объявлены устаревшими и в схеме 2.0.4 отсутствуют: узел
     * отвергает чек с ними до фискализации. Без этого признака кассир узнавал
     * о запрете только отказом уже пробитого чека.
     */
    @Schema(description = "Допускается действующей версией протокола", example = "true")
    val supported: Boolean
)

@Serializable
@Schema(description = "Элемент справочника видов отрасли")
data class ReceiptDomainTypeResponse(
    @Schema(description = "Код вида отрасли", example = "DOMAIN_TAXI") val code: String,
    @Schema(description = "Локализованное название") val name: TrilingualMessageResponse
)

@Serializable
@Schema(description = "Элемент справочника типов документов")
data class DocumentTypeResponse(
    @Schema(description = "Код типа документа", example = "SALE") val code: String,
    @Schema(description = "Локализованное название") val name: TrilingualMessageResponse
)

@Serializable
@Schema(description = "Элемент справочника ролей пользователей")
data class UserRoleResponse(
    @Schema(description = "Код роли", example = "CASHIER") val code: String,
    @Schema(description = "Локализованное название") val name: TrilingualMessageResponse
)

@Serializable
@Schema(description = "Элемент справочника налоговых режимов")
data class TaxRegimeResponse(
    @Schema(description = "Код налогового режима", example = "NO_VAT") val code: String,
    @Schema(description = "Локализованное название") val name: TrilingualMessageResponse
)

@Serializable
@Schema(description = "Элемент справочника ширины чековой ленты")
data class PaperWidthResponse(
    @Schema(description = "Код ширины ленты (или FULLSCREEN)", example = "80") val code: String,
    @Schema(description = "Локализованное название") val name: TrilingualMessageResponse
)

@Serializable
@Schema(description = "Элемент справочника цветов брендирования")
data class BrandingColorResponse(
    @Schema(description = "HEX-код цвета", example = "#007AFF") val code: String,
    @Schema(description = "Локализованное название") val name: TrilingualMessageResponse
)

@Serializable
@Schema(description = "Элемент справочника состояний ККМ")
data class KkmStateResponse(
    @Schema(description = "Код состояния", example = "ACTIVE") val code: String,
    @Schema(description = "Локализованное название") val name: TrilingualMessageResponse
)

@Serializable
@Schema(description = "Элемент справочника режимов работы ККМ")
data class KkmModeResponse(
    @Schema(description = "Код режима работы", example = "ONLINE") val code: String,
    @Schema(description = "Локализованное название") val name: TrilingualMessageResponse
)

@Serializable
@Schema(description = "Элемент справочника статусов смены")
data class ShiftStatusResponse(
    @Schema(description = "Код статуса смены", example = "OPEN") val code: String,
    @Schema(description = "Локализованное название") val name: TrilingualMessageResponse
)

@Serializable
@Schema(description = "Элемент справочника статусов отправки в ОФД")
data class DeliveryStatusResponse(
    @Schema(description = "Код статуса отправки", example = "ONLINE_OK") val code: String,
    @Schema(description = "Локализованное название") val name: TrilingualMessageResponse
)

@Serializable
@Schema(description = "Элемент справочника статусов выполнения команд ОФД")
data class OfdCommandStatusResponse(
    @Schema(description = "Код статуса команды ОФД", example = "OK") val code: String,
    @Schema(description = "Локализованное название") val name: TrilingualMessageResponse
)

@Serializable
@Schema(description = "Элемент справочника типов фискальных операций чека")
data class ReceiptOperationTypeResponse(
    @Schema(description = "Код типа операции чека", example = "SELL") val code: String,
    @Schema(description = "Локализованное название") val name: TrilingualMessageResponse
)

@Serializable
@Schema(description = "Элемент справочника окружений ОФД")
data class OfdEnvironmentResponse(
    @Schema(description = "Код окружения", example = "PROD") val code: String,
    @Schema(description = "Локализованное название") val name: TrilingualMessageResponse
)

@Serializable
@Schema(description = "Элемент справочника провайдеров ОФД")
data class OfdProviderResponse(
    @Schema(description = "Код провайдера", example = "KAZAKHTELECOM") val code: String,
    @Schema(description = "Локализованное название") val name: TrilingualMessageResponse,
    @Schema(description = "Ссылка на веб-сайт провайдера", example = "oofd.kz") val website: String
)

@Serializable
@Schema(description = "Элемент справочника режимов работы ядра")
data class CoreModeResponse(
    @Schema(description = "Код режима", example = "SERVER") val code: String,
    @Schema(description = "Локализованное название") val name: TrilingualMessageResponse
)

@Serializable
@Schema(description = "Элемент справочника режимов авторизации")
data class AuthModeResponse(
    @Schema(description = "Код режима авторизации", example = "BEARER") val code: String,
    @Schema(description = "Локализованное название") val name: TrilingualMessageResponse
)

@Serializable
@Schema(description = "Элемент справочника поддерживаемых языков документов")
data class ReceiptLanguageResponse(
    @Schema(description = "Код языка", example = "MIXED") val code: String,
    @Schema(description = "Локализованное название") val name: TrilingualMessageResponse
)

@Serializable
@Schema(description = "Элемент справочника типов макетов чека")
data class ReceiptLayoutTypeResponse(
    @Schema(description = "Код типа макета", example = "TAPE_80MM") val code: String,
    @Schema(description = "Локализованное название") val name: TrilingualMessageResponse
)

@Serializable
@Schema(description = "Элемент справочника типов печатных документов")
data class PrintDocumentTypeResponse(
    @Schema(description = "Код типа печатного документа", example = "DOCUMENT") val code: String,
    @Schema(description = "Локализованное название") val name: TrilingualMessageResponse
)

@Serializable
@Schema(description = "Элемент справочника типов команд ОФД")
data class OfdCommandTypeResponse(
    @Schema(description = "Код типа команды", example = "COMMAND_TICKET") val code: String,
    @Schema(description = "Локализованное название") val name: TrilingualMessageResponse
)

@Serializable
@Schema(description = "Элемент справочника типов операций с наличными")
data class CashOperationTypeResponse(
    @Schema(description = "Код типа операции", example = "CASH_OUT") val code: String,
    @Schema(description = "Локализованное название") val name: TrilingualMessageResponse
)

@Serializable
@Schema(description = "Сериализуемая обертка для трехъязычного сообщения")
data class TrilingualMessageResponse(
    @Schema(description = "Текст на русском", example = "Пример") val ru: String,
    @Schema(description = "Текст на казахском", example = "Мысал") val kk: String,
    @Schema(description = "Текст на английском", example = "Example") val en: String
) {
    companion object {
        fun from(msg: TrilingualMessage): TrilingualMessageResponse = TrilingualMessageResponse(
            ru = msg.ru,
            kk = msg.kk,
            en = msg.en
        )
    }
}
