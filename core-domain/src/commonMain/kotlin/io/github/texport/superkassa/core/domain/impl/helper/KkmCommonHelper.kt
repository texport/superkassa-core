package io.github.texport.superkassa.core.domain.impl.helper

import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase
import io.github.texport.superkassa.core.domain.api.port.internal.OfflineQueuePort
import io.github.texport.superkassa.core.domain.impl.usecase.ofd.GenerateRequestNumberUseCase
import io.github.texport.superkassa.core.domain.impl.helper.ofd.BfdExchange
import io.github.texport.superkassa.core.domain.impl.helper.ofd.INVALID_REQUEST_NUMBER
import io.github.texport.superkassa.core.domain.impl.helper.ofd.INVALID_RETRY_REQUEST
import io.github.texport.superkassa.core.domain.impl.helper.ofd.OfdCommandRequestFactory
import io.github.texport.superkassa.core.domain.impl.helper.ofd.OfdRequestOverrides
import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.domain.api.exception.ConflictException
import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdServiceInfo
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.internal.OfdManagerPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.api.port.integration.inTransaction
import io.github.texport.superkassa.core.domain.api.port.integration.TimeValidatorPort
import io.github.texport.superkassa.core.domain.api.port.internal.TokenCodecPort

/**
 * Общее для сценариев кассы: проверка системного времени, обмен с БФД
 * и допуск к синхронизации.
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
    private val exchange = BfdExchange(
        storage,
        clock,
        tokenCodec,
        generateRequestNumberUseCase,
        ofdCommandRequestFactory,
        ofd,
        ::defaultServiceInfo
    )

    /** @throws ValidationException если системное время кассы негодно. */
    fun ensureSystemTimeValid() {
        val result = timeValidator.validate(clock)
        if (!result.ok) {
            val msg = result.trilingualMessage ?: CoreStrings.systemTimeInvalid()
            throw ValidationException(msg, "SYSTEM_TIME_INVALID")
        }
    }

    /** Сведения об услуге-заглушке, пока настоящих нет. */
    fun defaultServiceInfo(): OfdServiceInfo {
        return OfdServiceInfo(
            orgTitle = "UNKNOWN",
            orgAddress = "UNKNOWN",
            orgAddressKz = "UNKNOWN",
            orgIinOrBin = "000000000000",
            orgOked = "00000",
            geoLatitude = 0,
            geoLongitude = 0,
            geoSource = "UNKNOWN"
        )
    }

    /**
     * Формирует и отправляет команду ОФД.
     * При необходимости обновляет токен кассы в хранилище после получения успешного ответа.
     *
     * Обмен с БФД у кассы один за раз: строка кассы блокируется от выбора
     * номера запроса до записи его исхода. Без этого «Проверить связь»,
     * нажатая, пока чек ждёт ответа, уходила с номером этого чека.
     *
     * Подмены (`*Override`) нужны регистрации: она шлёт то, чего в хранилище
     * ещё нет. `updateToken = false` оставляет токен кассы прежним.
     *
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
        val overrides = OfdRequestOverrides(
            tokenOverride,
            serviceInfoOverride,
            registrationNumberOverride,
            factoryNumberOverride,
            ofdProviderOverride
        )
        val result = storage.inTransaction { exchange.send(kkm, commandType, payloadRef, overrides, updateToken) }
        if (result.resultCode == INVALID_REQUEST_NUMBER || result.resultCode == INVALID_RETRY_REQUEST) {
            // Номер уже израсходован, и повтор уйдёт со следующим.
            throw ValidationException(CoreStrings.ofdSyncError(), "OFD_RETRY_REQUEST")
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
        authorization.requireRole(
            kkmId,
            pin,
            setOf(UserRole.ADMIN)
        )
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
