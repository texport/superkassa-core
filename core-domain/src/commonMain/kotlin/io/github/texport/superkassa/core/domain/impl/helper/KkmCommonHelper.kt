package io.github.texport.superkassa.core.domain.impl.helper

import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase
import io.github.texport.superkassa.core.domain.api.port.internal.OfflineQueuePort
import io.github.texport.superkassa.core.domain.impl.usecase.ofd.GenerateRequestNumberUseCase
import io.github.texport.superkassa.core.domain.impl.helper.ofd.OfdCommandRequestFactory
import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.domain.api.exception.ConflictException
import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdServiceInfo
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.internal.OfdManagerPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.api.port.integration.TimeValidatorPort
import io.github.texport.superkassa.core.domain.api.port.internal.TokenCodecPort

/**
 * Вспомогательный класс, инкапсулирующий общую логику и утилиты для взаимодействия с ККМ (контрольно-кассовыми машинами)
 * и ОФД (операторами фискальных данных).
 *
 * Предоставляет методы для проверки системного времени, работы с конфигурацией ОФД, отправки команд и проверки
 * возможности выполнения синхронизации.
 *
 * @property storage Порт для доступа к хранилищу данных.
 * @property clock Порт для получения текущего системного времени.
 * @property timeValidator Порт для валидации системного времени.
 * @property tokenCodec Кодек для шифрования и дешифрования токенов ОФД.
 * @property generateRequestNumberUseCase Сценарий генерации уникального номера запроса.
 * @property ofdCommandRequestFactory Фабрика для построения объектов запросов к ОФД.
 * @property ofd Менеджер для отправки команд в ОФД.
 */
class KkmCommonHelper(
    private val storage: StoragePort,
    private val clock: ClockPort,
    private val timeValidator: TimeValidatorPort,
    private val tokenCodec: TokenCodecPort,
    private val generateRequestNumberUseCase: GenerateRequestNumberUseCase,
    private val ofdCommandRequestFactory: OfdCommandRequestFactory,
    private val ofd: OfdManagerPort
) {
    /**
     * Проверяет корректность текущего системного времени с помощью валидатора [timeValidator].
     * Если время невалидно, выбрасывает [ValidationException] с соответствующим сообщением об ошибке.
     *
     * @throws ValidationException если системное время некорректно.
     */
    fun ensureSystemTimeValid() {
        val result = timeValidator.validate(clock)
        if (!result.ok) {
            val msg = result.trilingualMessage ?: CoreStrings.systemTimeInvalid()
            throw ValidationException(msg, "SYSTEM_TIME_INVALID")
        }
    }

    /**
     * Возвращает информацию об услуге ОФД по умолчанию с незаполненными (шаблонными) значениями.
     * Используется в качестве заглушки при отсутствии переопределений.
     *
     * @return Объект [OfdServiceInfo] со стандартными дефолтными значениями.
     */
    fun defaultServiceInfo(): OfdServiceInfo {
        return OfdServiceInfo(
            orgTitle = "UNKNOWN",
            orgAddress = "UNKNOWN",
            orgAddressKz = "UNKNOWN",
            orgInn = "000000000000",
            orgOkved = "00000",
            geoLatitude = 0,
            geoLongitude = 0,
            geoSource = "UNKNOWN"
        )
    }

    /**
     * Формирует и отправляет команду ОФД.
     * При необходимости обновляет токен кассы в хранилище после получения успешного ответа.
     *
     * @param kkm Данные ККМ, для которой выполняется команда.
     * @param commandType Тип отправляемой команды ОФД.
     * @param payloadRef Ссылка на полезную нагрузку команды.
     * @param tokenOverride Необязательный токен ОФД для переопределения стандартного токена ККМ.
     * @param serviceInfoOverride Необязательная информация об услуге для переопределения стандартной.
     * @param registrationNumberOverride Необязательный регистрационный номер для переопределения.
     * @param factoryNumberOverride Необязательный заводской номер для переопределения.
     * @param ofdProviderOverride Необязательный провайдер ОФД для переопределения.
     * @param updateToken Флаг, указывающий, нужно ли сохранять обновленный токен из ответа ОФД в базу данных.
     * @return Результат выполнения команды [OfdCommandResult].
     * @throws ValidationException если токен ОФД отсутствует или не может быть расшифрован.
     */
    fun sendOfdCommand(
        kkm: KkmInfo,
        commandType: OfdCommandType,
        payloadRef: String,
        tokenOverride: Long? = null,
        serviceInfoOverride: OfdServiceInfo? = null,
        registrationNumberOverride: String? = null,
        factoryNumberOverride: String? = null,
        ofdProviderOverride: String? = null,
        updateToken: Boolean = true
    ): OfdCommandResult {
        val token = tokenOverride
            ?: tokenCodec.decodeToken(kkm.tokenEncryptedBase64)
            ?: throw ValidationException(CoreStrings.ofdTokenRequired(), "OFD_TOKEN_REQUIRED")
        val reqNum = generateRequestNumberUseCase.execute(kkm.id)
        val now = clock.now()
        val request = ofdCommandRequestFactory.build(
            kkm = kkm,
            commandType = commandType,
            payloadRef = payloadRef,
            token = token,
            reqNum = reqNum,
            now = now,
            serviceInfoOverride = serviceInfoOverride,
            registrationNumberOverride = registrationNumberOverride,
            factoryNumberOverride = factoryNumberOverride,
            ofdProviderOverride = ofdProviderOverride,
            defaultServiceInfo = ::defaultServiceInfo
        )
        val result = ofd.send(request)
        if (updateToken) {
            result.responseToken?.let { nextToken ->
                storage.updateKkmToken(kkm.id, tokenCodec.encodeToken(nextToken), now)
            }
        }
        return result
    }

    /**
     * Проверяет, разрешена ли синхронизация для указанной ККМ, и возвращает информацию о кассе.
     * Выполняет проверку системного времени, прав пользователя и состояния очереди офлайн-запросов.
     *
     * @param kkmId Уникальный идентификатор ККМ.
     * @param pin ПИН-код пользователя для проверки авторизации.
     * @param allowOpenShift Разрешить ли синхронизацию при открытой смене (по умолчанию false).
     * @param authorization Сценарий проверки авторизации пользователя.
     * @param queue Порт для работы с офлайн-очередью запросов.
     * @return Данные ККМ [KkmInfo], если все проверки пройдены успешно.
     * @throws ConflictException если смена открыта (и [allowOpenShift] равен false) или если офлайн-очередь не пуста.
     * @throws ValidationException если системное время невалидно.
     */
    fun requireSyncAllowed(
        kkmId: String,
        pin: String,
        allowOpenShift: Boolean = false,
        authorization: AuthorizeUserUseCase,
        queue: OfflineQueuePort
    ): KkmInfo {
        ensureSystemTimeValid()
        authorization.requireRole(kkmId, pin, setOf(io.github.texport.superkassa.core.domain.api.model.auth.UserRole.ADMIN))
        return storage.inTransaction {
            val kkm = authorization.requireKkm(kkmId)
            if (!allowOpenShift) {
                val openShift = storage.findOpenShift(kkmId)
                if (openShift != null) {
                    throw ConflictException(CoreStrings.kkmSyncShiftOpen(), "KKM_SYNC_SHIFT_OPEN")
                }
            }
            if (!queue.canSendDirectly(kkmId)) {
                throw ConflictException(
                    CoreStrings.kkmSyncQueueNotEmpty(),
                    "KKM_SYNC_QUEUE_NOT_EMPTY"
                )
            }
            kkm
        }
    }
}
