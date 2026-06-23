package kz.mybrain.superkassa.core.application.service

import kz.mybrain.superkassa.core.application.error.ConflictException
import kz.mybrain.superkassa.core.application.error.ErrorMessages
import kz.mybrain.superkassa.core.application.error.ValidationException
import kz.mybrain.superkassa.core.application.model.OfdAuthInfoResponse
import kz.mybrain.superkassa.core.application.policy.CounterScopes
import kz.mybrain.superkassa.core.domain.port.TimeValidatorPort
import kz.mybrain.superkassa.core.domain.model.KkmInfo
import kz.mybrain.superkassa.core.domain.model.KkmState
import kz.mybrain.superkassa.core.domain.model.OfdCommandResult
import kz.mybrain.superkassa.core.domain.model.OfdCommandStatus
import kz.mybrain.superkassa.core.domain.model.OfdCommandType
import kz.mybrain.superkassa.core.domain.model.OfdServiceInfo
import kz.mybrain.superkassa.core.domain.model.UserRole
import kz.mybrain.superkassa.core.domain.port.ClockPort
import kz.mybrain.superkassa.core.domain.port.IdGenerator
import kz.mybrain.superkassa.core.domain.port.OfdManagerPort
import kz.mybrain.superkassa.core.domain.port.OfflineQueuePort
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kotlinx.serialization.json.JsonObject

/**
 * Сервис синхронизации с ОФД.
 * Выделен из KkmService для соблюдения SRP.
 */
class OfdSyncService(
    private val storage: StoragePort,
    private val queue: OfflineQueuePort,
    private val ofd: OfdManagerPort,
    private val idGenerator: IdGenerator,
    private val clock: ClockPort,
    private val authorization: AuthorizationService,
    private val ofdCommandRequestBuilder: OfdCommandRequestBuilder,
    private val tokenCodec: kz.mybrain.superkassa.core.domain.port.TokenCodecPort,
    private val autonomousModeService: AutonomousModeService,
    private val reqNumService: ReqNumService,
    private val timeValidator: TimeValidatorPort
) {


    /**
     * Отправляет фискальную команду (REPORT, CLOSE_SHIFT и т.п.) в ОФД.
     * Используется когда offline-очередь пуста.
     */
    fun sendFiscalCommand(kkmId: String, commandType: OfdCommandType, payloadRef: String): OfdCommandResult {
        val kkm = authorization.requireKkm(kkmId)
        return sendOfdCommand(kkm = kkm, commandType = commandType, payloadRef = payloadRef)
    }

    /**
     * Проверяет связь с ОФД (COMMAND_SYSTEM).
     */
    fun checkOfdConnection(kkmId: String): OfdCommandResult {
        val kkm = authorization.requireKkm(kkmId)
        return sendOfdCommand(
            kkm = kkm,
            commandType = OfdCommandType.SYSTEM,
            payloadRef = idGenerator.nextId()
        )
    }

    /**
     * Запрашивает информацию о кассе в ОФД (COMMAND_INFO).
     */
    fun getOfdInfo(kkmId: String): OfdCommandResult {
        val kkm = authorization.requireKkm(kkmId)
        return sendOfdCommand(
            kkm = kkm,
            commandType = OfdCommandType.INFO,
            payloadRef = idGenerator.nextId()
        )
    }

    /**
     * Синхронизирует сервисную информацию о ККМ с ОФД.
     * Обновляет также регистрационный номер и заводской номер из ответа ОФД.
     */
    fun syncOfdServiceInfo(kkmId: String, pin: String): OfdCommandResult {
        return syncFromOfdInfo(kkmId, pin) { kkm, responseJson ->
            val updatedServiceInfo = OfdResponseParser.extractServiceInfo(
                responseJson,
                kkm.ofdServiceInfo ?: defaultServiceInfo()
            )
            val shiftNo = OfdResponseParser.extractShiftNumber(responseJson) ?: kkm.lastShiftNo
            
            // Извлекаем регистрационный номер и заводской номер из ответа ОФД
            val registrationNumber = OfdResponseParser.extractRegistrationNumber(responseJson) ?: kkm.registrationNumber
            val factoryNumber = OfdResponseParser.extractFactoryNumber(responseJson) ?: kkm.factoryNumber
            
            storage.updateKkm(
                kkm.copy(
                    updatedAt = clock.now(),
                    lastShiftNo = shiftNo,
                    ofdServiceInfo = updatedServiceInfo,
                    registrationNumber = registrationNumber,
                    factoryNumber = factoryNumber
                )
            )
        }
    }

    /**
     * Синхронизирует счетчики ККМ с ОФД.
     */
    fun syncOfdCounters(kkmId: String, pin: String): OfdCommandResult {
        return syncFromOfdInfo(kkmId, pin, allowOpenShift = true) { kkm, responseJson ->
            if (responseJson != null) {
                val snapshot = OfdInfoCountersSnapshotParser.parse(responseJson)
                val now = clock.now()

                // 1. Update global counters
                snapshot.globalCounters.forEach { (key, value) ->
                    storage.upsertCounter(kkmId, CounterScopes.GLOBAL, null, key, value)
                }

                val shiftNo = snapshot.shiftNumber ?: kkm.lastShiftNo ?: 0

                // 2. Handle shift status based on snapshot
                if (snapshot.isOpenShift) {
                    var localOpenShift = storage.findOpenShift(kkmId)
                    if (localOpenShift == null) {
                        val shiftId = idGenerator.nextId()
                        val newShift = kz.mybrain.superkassa.core.domain.model.ShiftInfo(
                            id = shiftId,
                            kkmId = kkmId,
                            shiftNo = shiftNo.toLong(),
                            status = kz.mybrain.superkassa.core.domain.model.ShiftStatus.OPEN,
                            openedAt = snapshot.openShiftTimeMillis ?: now,
                            closedAt = null
                        )
                        storage.createShift(newShift)
                        localOpenShift = newShift
                    }

                    // Update shift counters for the open shift
                    val currentShiftId = localOpenShift.id
                    snapshot.shiftCounters.forEach { (key, value) ->
                        storage.upsertCounter(kkmId, CounterScopes.SHIFT, currentShiftId, key, value)
                    }
                } else {
                    // Closed shift from OFD
                    val localOpenShift = storage.findOpenShift(kkmId)
                    if (localOpenShift != null) {
                        storage.closeShift(
                            shiftId = localOpenShift.id,
                            status = kz.mybrain.superkassa.core.domain.model.ShiftStatus.CLOSED,
                            closedAt = snapshot.closeShiftTimeMillis ?: now,
                            closeDocumentId = null
                        )

                        // Update shift counters for this closed shift
                        snapshot.shiftCounters.forEach { (key, value) ->
                            storage.upsertCounter(kkmId, CounterScopes.SHIFT, localOpenShift.id, key, value)
                        }
                    }
                }

                storage.updateKkm(kkm.copy(updatedAt = now, lastShiftNo = shiftNo))
            }
        }
    }

    /**
     * Общая логика синхронизации с ОФД через COMMAND_INFO.
     * Вынесена для устранения дублирования между syncOfdServiceInfo и syncOfdCounters.
     */
    private fun syncFromOfdInfo(
        kkmId: String,
        pin: String,
        allowOpenShift: Boolean = false,
        processResult: (KkmInfo, JsonObject?) -> Unit
    ): OfdCommandResult {
        val kkm = requireSyncAllowed(kkmId, pin, allowOpenShift)
        val result = sendOfdCommand(
            kkm = kkm,
            commandType = OfdCommandType.INFO,
            payloadRef = idGenerator.nextId()
        )
        if (result.status == OfdCommandStatus.OK) {
            storage.inTransaction {
                processResult(kkm, result.responseJson)
            }
        }
        return result
    }

    /**
     * Получает информацию об авторизации в ОФД (токен и следующий reqNum).
     */
    fun getOfdAuthInfo(kkmId: String, pin: String): OfdAuthInfoResponse {
        authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN))
        val kkm = authorization.requireKkm(kkmId)
        val token = tokenCodec.decodeToken(kkm.tokenEncryptedBase64)?.toString()
        val nextReq = reqNumService.nextReqNumPreview(kkmId)
        return OfdAuthInfoResponse(token = token, nextReqNum = nextReq)
    }

    /**
     * Обновляет токен ОФД.
     */
    fun updateOfdToken(kkmId: String, pin: String, token: String): Boolean {
        authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN))
        val parsed = tokenCodec.parseToken(token)
        return storage.updateKkmToken(kkmId, tokenCodec.encodeToken(parsed), clock.now())
    }

    private fun requireSyncAllowed(kkmId: String, pin: String, allowOpenShift: Boolean = false): KkmInfo {
        ensureSystemTimeValid()
        authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN))
        // Проверка выполняется в транзакции для атомарности операций с состоянием ККМ
        return storage.inTransaction {
            val kkm = authorization.requireKkm(kkmId)
            enforceAutonomousLimits(kkm)
            if (!allowOpenShift) {
                val openShift = storage.findOpenShift(kkmId)
                if (openShift != null) {
                    throw ConflictException(ErrorMessages.kkmSyncShiftOpen(), "KKM_SYNC_SHIFT_OPEN")
                }
            }
            if (!queue.canSendDirectly(kkmId)) {
                throw ConflictException(
                    ErrorMessages.kkmSyncQueueNotEmpty(),
                    "KKM_SYNC_QUEUE_NOT_EMPTY"
                )
            }
            kkm
        }
    }

    private fun sendOfdCommand(
        kkm: KkmInfo,
        commandType: OfdCommandType,
        payloadRef: String
    ): OfdCommandResult {
        val token = tokenCodec.decodeToken(kkm.tokenEncryptedBase64)
            ?: throw ValidationException(ErrorMessages.ofdTokenRequired(), "OFD_TOKEN_REQUIRED")
        val reqNum = reqNumService.nextReqNum(kkm.id)
        val now = clock.now()
        val request = ofdCommandRequestBuilder.build(
            kkm = kkm,
            commandType = commandType,
            payloadRef = payloadRef,
            token = token,
            reqNum = reqNum,
            now = now,
            defaultServiceInfo = ::defaultServiceInfo
        )
        val result = ofd.send(request)
        if (result.responseToken != null) {
            storage.updateKkmToken(kkm.id, tokenCodec.encodeToken(result.responseToken), now)
        }
        updateKkmBlockedStateFromOfd(kkm, result, now)
        return result
    }

    /**
     * Обновляет состояние блокировки ККМ в зависимости от resultCode ОФД.
     *
     * - При resultCode == 15 касса переводится в BLOCKED.
     * - При resultCode == 0, если касса была BLOCKED, переводится в ACTIVE.
     */
    private fun updateKkmBlockedStateFromOfd(kkm: KkmInfo, result: OfdCommandResult, now: Long) {
        val code = result.resultCode ?: return
        if (code == 15 && kkm.state != KkmState.BLOCKED.name) {
            storage.updateKkm(kkm.copy(updatedAt = now, state = KkmState.BLOCKED.name))
        } else if (code == 0 && kkm.state == KkmState.BLOCKED.name) {
            storage.updateKkm(kkm.copy(updatedAt = now, state = KkmState.ACTIVE.name))
        }
    }

    private fun enforceAutonomousLimits(kkm: KkmInfo) {
        autonomousModeService.enforceAutonomousLimits(kkm)
    }

    private fun ensureSystemTimeValid() {
        val result = timeValidator.validate(clock)
        if (!result.ok) {
            val msg = result.trilingualMessage() ?: ErrorMessages.systemTimeInvalid()
            throw ValidationException(msg, "SYSTEM_TIME_INVALID")
        }
    }


    private fun defaultServiceInfo(): OfdServiceInfo {
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


}
