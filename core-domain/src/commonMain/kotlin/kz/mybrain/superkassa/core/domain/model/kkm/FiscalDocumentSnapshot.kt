package kz.mybrain.superkassa.core.domain.model.kkm

/**
 * Слепок фискального документа для хранения истории и отображения.
 * Используется для предоставления краткой информации в ответах API.
 *
 * @property id Уникальный идентификатор документа.
 * @property cashboxId Идентификатор кассы (ККМ), создавшей документ.
 * @property shiftId Идентификатор смены, в которой был создан документ.
 * @property docType Тип документа (например, TICKET, REPORT).
 * @property docNo Порядковый номер документа (может быть пустым для неподтвержденных).
 * @property shiftNo Номер смены, в которой создан документ (может быть пустым).
 * @property createdAt Время создания документа (в миллисекундах).
 * @property totalAmount Итоговая сумма по документу (в минимальных денежных единицах, например, тиынах).
 * @property currency Валюта документа (например, KZT).
 * @property fiscalSign Фискальный признак (подпись) документа.
 * @property autonomousSign Автономный признак документа.
 * @property isAutonomous Флаг, указывающий, был ли документ оформлен в автономном режиме.
 * @property ofdStatus Статус доставки в ОФД.
 * @property deliveredAt Время доставки документа в ОФД (в миллисекундах).
 * @property receiptUrl Ссылка на электронный чек на сервере ОФД.
 * @property registrationNumber Регистрационный номер ККМ.
 * @property taxpayerName Наименование налогоплательщика.
 * @property taxpayerBin БИН/ИИН налогоплательщика.
 * @property taxpayerAddress Адрес использования ККМ.
 * @property factoryNumber Заводской номер ККМ.
 * @property ofdProvider Код провайдера ОФД.
 */
data class FiscalDocumentSnapshot(
    val id: String,
    val cashboxId: String,
    val shiftId: String,
    val docType: String,
    val docNo: Long?,
    val shiftNo: Long?,
    val createdAt: Long,
    val totalAmount: Long?,
    val currency: String?,
    val fiscalSign: String?,
    val autonomousSign: String?,
    val isAutonomous: Boolean,
    val ofdStatus: String?,
    val deliveredAt: Long?,
    val receiptUrl: String? = null,
    val registrationNumber: String? = null,
    val taxpayerName: String? = null,
    val taxpayerBin: String? = null,
    val taxpayerAddress: String? = null,
    val factoryNumber: String? = null,
    val ofdProvider: String? = null
)
