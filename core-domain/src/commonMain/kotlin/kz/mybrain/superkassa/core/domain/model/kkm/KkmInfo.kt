package kz.mybrain.superkassa.core.domain.model.kkm

import kz.mybrain.superkassa.core.domain.model.common.TaxRegime
import kz.mybrain.superkassa.core.domain.model.common.VatGroup
import kz.mybrain.superkassa.core.domain.model.ofd.OfdServiceInfo
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptBranding

/**
 * Информация о состоянии, настройках и реквизитах ККМ.
 *
 * @property id Уникальный идентификатор кассы.
 * @property createdAt Время создания записи о ККМ (в миллисекундах).
 * @property updatedAt Время последнего обновления записи о ККМ (в миллисекундах).
 * @property mode Текущий режим работы кассы (например, REGISTRATION, PROGRAMMING).
 * @property state Текущее состояние кассы (например, ACTIVE, BLOCKED).
 * @property ofdProvider Имя провайдера ОФД.
 * @property registrationNumber Регистрационный номер ККМ, присвоенный НК.
 * @property factoryNumber Заводской (серийный) номер устройства.
 * @property manufactureYear Год выпуска устройства.
 * @property systemId Идентификатор в внешней системе.
 * @property ofdServiceInfo Настройки подключения и параметры службы ОФД.
 * @property tokenEncryptedBase64 Зашифрованный токен авторизации ОФД в формате Base64.
 * @property tokenUpdatedAt Время последнего обновления токена авторизации.
 * @property lastShiftNo Номер последней открытой/закрытой смены.
 * @property lastReceiptNo Номер последнего оформленного чека.
 * @property lastZReportNo Номер последнего оформленного Z-отчета.
 * @property autonomousSince Время начала работы в автономном (офлайн) режиме.
 * @property autoCloseShift Флаг автоматического закрытия смены по истечении 24 часов.
 * @property lastFiscalHashBase64 Хеш последнего фискального документа в формате Base64.
 * @property taxRegime Налоговый режим ККМ (по умолчанию неплательщик НДС).
 * @property defaultVatGroup Группа НДС по умолчанию для позиций без явно указанной ставки.
 * @property branding Настройки брендирования и локализации печатных форм чеков.
 */
data class KkmInfo(
    val id: String,
    val createdAt: Long,
    val updatedAt: Long,
    val mode: String,
    val state: String,
    val ofdProvider: String? = null,
    val registrationNumber: String? = null,
    val factoryNumber: String? = null,
    val manufactureYear: Int? = null,
    val systemId: String? = null,
    val ofdServiceInfo: OfdServiceInfo? = null,
    val tokenEncryptedBase64: String? = null,
    val tokenUpdatedAt: Long? = null,
    val lastShiftNo: Int? = null,
    val lastReceiptNo: Int? = null,
    val lastZReportNo: Int? = null,
    val autonomousSince: Long? = null,
    val autoCloseShift: Boolean = false,
    val lastFiscalHashBase64: String? = null,
    val taxRegime: TaxRegime = TaxRegime.NO_VAT,
    val defaultVatGroup: VatGroup = VatGroup.NO_VAT,
    val branding: ReceiptBranding = ReceiptBranding()
)
