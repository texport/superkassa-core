package kz.mybrain.superkassa.core.domain.model.ofd

/**
 * Запрос фискальной команды, отправляемой в ОФД.
 *
 * @property kkmId Идентификатор кассы (ККМ), отправляющей запрос.
 * @property commandType Тип отправляемой команды (например, TICKET, REPORT).
 * @property payloadRef Ссылка на полезную нагрузку во внутреннем хранилище.
 * @property ofdProviderId Код провайдера ОФД.
 * @property ofdEnvironmentId Идентификатор окружения ОФД (DEV, TEST, PROD).
 * @property deviceId Идентификатор устройства в системе ОФД.
 * @property token Уникальный числовой токен сессии ОФД.
 * @property reqNum Порядковый номер текущего запроса в сессии.
 * @property registrationNumber Регистрационный номер кассы.
 * @property factoryNumber Серийный (заводской) номер кассы.
 * @property ofdSystemId Идентификатор системы кассового ядра в ОФД.
 * @property serviceInfo Дополнительные регистрационные данные организации.
 * @property offlineBeginMillis Время начала работы в автономном режиме (в миллисекундах, если применимо).
 * @property offlineEndMillis Время окончания автономного режима (в миллисекундах, если применимо).
 */
data class OfdCommandRequest(
    val kkmId: String,
    val commandType: OfdCommandType,
    val payloadRef: String,
    val ofdProviderId: String,
    val ofdEnvironmentId: String,
    val deviceId: Long,
    val token: Long,
    val reqNum: Int,
    val registrationNumber: String? = null,
    val factoryNumber: String? = null,
    val ofdSystemId: String? = null,
    val serviceInfo: OfdServiceInfo? = null,
    val offlineBeginMillis: Long? = null,
    val offlineEndMillis: Long? = null
)
