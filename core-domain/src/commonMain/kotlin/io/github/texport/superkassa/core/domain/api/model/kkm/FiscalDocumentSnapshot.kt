package io.github.texport.superkassa.core.domain.api.model.kkm

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
    /** Сквозной номер печатного документа, присваиваемый самой кассой. */
    val printedDocumentNumber: Long? = null,
    val shiftNo: Long?,
    val createdAt: Long,
    val totalAmount: Long?,
    val currency: String?,
    val fiscalSign: String?,
    val autonomousSign: String?,
    val isAutonomous: Boolean,
    val ofdStatus: String?,
    val ofdErrorCode: Int? = null,
    /**
     * Причина отказа словами ОФД.
     *
     * Код отказа обслуживанию мало что говорит: «Код отказа 15» стоит и за
     * снятой с учёта кассой, и за нехваткой обязательного реквизита
     * в позиции. Текст ОФД называет причину, и без него её искали
     * в журнале узла.
     */
    val ofdErrorText: String? = null,
    val deliveredAt: Long?,
    val receiptUrl: String? = null,
    val registrationNumber: String? = null,
    val taxpayerName: String? = null,
    val taxpayerBin: String? = null,
    val taxpayerAddress: String? = null,
    val factoryNumber: String? = null,
    val ofdProvider: String? = null
)

/**
 * Стал ли документ фискальным.
 *
 * Документ, который ОФД отказался провести, остаётся в журнале без
 * признака: ни фискального, ни автономного. Ни в счётчики, ни в денежный
 * ящик он попадать не должен — ни при пробитии, ни при пересчёте. Иначе
 * X-отчёт добавляет в кассу деньги по документам, которых не было.
 *
 * Признаком служит отсутствие всех следов сразу: ни фискального признака,
 * ни автономного, ни отметки об отправке. Отказ ОФД — такой же «не
 * провели», как и вечное ожидание отправки.
 *
 * Функция расширения, а не свойство: снимок отдаётся наружу, а вычисляемое
 * свойство утекает в сериализацию лишним полем.
 */
fun FiscalDocumentSnapshot.becameFiscal(): Boolean =
    fiscalSign != null ||
        autonomousSign != null ||
        isAutonomous ||
        (ofdStatus != NEVER_ACCEPTED && ofdStatus != REJECTED)

/** Документ ещё ни разу не был принят ОФД. */
private const val NEVER_ACCEPTED = "PENDING"

/** Документ отвергнут ОФД: фискальным он не стал. */
private const val REJECTED = "FAILED"
