package io.github.texport.superkassa.core.presentation.api.model.kkm

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Информация о ККМ, возвращаемая API.
 */
@Serializable
@Schema(description = "Информация о ККМ")
data class KkmResponse(
    @Schema(description = "ID ККМ", example = "kkm-123") val kkmId: String,
    @Schema(description = "Метка времени создания (epoch ms)", example = "1700000000000")
    val createdAt: Long,
    @Schema(
        description = "Метка времени последнего обновления (epoch ms)",
        example = "1700000000000"
    )
    val updatedAt: Long,
    @Schema(description = "Режим работы ККМ", example = "REGISTRATION") val mode: String,
    @Schema(description = "Состояние ККМ", example = "ACTIVE") val state: String,
    @Schema(
        description = "Название кассы, данное владельцем. Пусто у касс, заведённых до появления поля",
        example = "Касса 2 на Достык"
    )
    val name: String? = null,
    @Schema(description = "ID провайдера ОФД", example = "kazakhtelecom")
    val ofdId: String? = null,
    @Schema(description = "Среда ОФД", example = "test") val ofdEnvironment: String? = null,
    @Schema(description = "Регистрационный номер ККМ (КГД)", example = "123456789012")
    val kkmKgdId: String? = null,
    @Schema(description = "Заводской номер ККМ", example = "SWK-0001")
    val factoryNumber: String? = null,
    @Schema(description = "Год выпуска", example = "2024") val manufactureYear: Int? = null,
    @Schema(description = "Системный ID в ОФД", example = "sys-123")
    val ofdSystemId: String? = null,
    @Schema(description = "Сервисная информация ОФД")
    val ofdServiceInfo: OfdServiceInfoResponse? = null,
    @Schema(
        description = "Зашифрованный токен ОФД (Base64)",
        example = "encrypted-token-base64==",
        hidden = true
    )
    val tokenEncryptedBase64: String? = null,
    @Schema(description = "Время обновления токена", example = "1700000000000")
    val tokenUpdatedAt: Long? = null,
    @Schema(description = "Номер последней смены", example = "10") val lastShiftNo: Int? = null,
    @Schema(description = "Номер последнего чека", example = "150")
    val lastReceiptNo: Int? = null,
    @Schema(description = "Номер последнего Z-отчета", example = "10")
    val lastZReportNo: Int? = null,
    @Schema(
        description = "Время начала автономного режима (если активен)",
        example = "1700000000000"
    )
    val autonomousSince: Long? = null,
    @Schema(description = "Автоматическое закрытие смены", example = "false")
    val autoCloseShift: Boolean = false,
    @Schema(description = "Автоматическое изъятие наличных при закрытии смены", example = "false")
    val autoCashout: Boolean = false,
    @Schema(description = "Хэш последней фискальной операции", example = "base64hash==")
    val lastFiscalHashBase64: String? = null,
    @Schema(
        description = "Налоговый режим ККМ (NO_VAT, VAT_PAYER, MIXED)",
        example = "NO_VAT"
    )
    val taxRegime: String? = null,
    @Schema(
        description = "Базовая группа НДС по умолчанию (NO_VAT, VAT_0, VAT_16)",
        example = "NO_VAT"
    )
    val defaultVatGroup: String? = null,
    @Schema(description = "Настройки брендирования чеков")
    val branding: ReceiptBrandingResponse? = null,
    @Schema(description = "Код причины блокировки ОФД")
    val blockReasonCode: Int? = null,

    // Агрегированные статусы (для UI)
    @Schema(description = "Статус открытой смены (true = открыта, false = закрыта)", example = "true")
    val isShiftOpen: Boolean = false,
    @Schema(description = "Время открытия текущей смены (epoch ms)", example = "1700000000000")
    val shiftOpenedAt: Long? = null,
    @Schema(description = "Количество чеков в офлайн-очереди", example = "5")
    val offlineQueueCount: Int = 0,
    /**
     * Сколько документов кассы до ОФД так и не дошло.
     *
     * Отбракованная задача из очереди уходит, а документ остаётся
     * неотправленным: без этого числа касса выглядела чистой, хотя
     * фискальный документ потерян.
     */
    val stuckQueueCount: Int = 0,
    @Schema(description = "Текст последней ошибки синхронизации с ОФД")
    val lastSyncError: String? = null,
    @Schema(description = "Режим программирования (касса заблокирована для настроек)", example = "false")
    val isProgrammingMode: Boolean = false,
    @Schema(description = "Флаг наличия/валидности токена ОФД", example = "true")
    val isTokenValid: Boolean = false
)
