package io.github.texport.superkassa.core.domain.impl.usecase.kkm

import io.github.texport.superkassa.core.domain.impl.helper.OfdResponseParser
import io.github.texport.superkassa.core.domain.impl.helper.OfdInfoCountersSnapshotParser
import io.github.texport.superkassa.core.domain.impl.helper.KkmCommonHelper
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftStatus
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.model.auth.StandardPin
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.model.common.CounterKeyFormats
import io.github.texport.superkassa.core.domain.api.model.common.format
import io.github.texport.superkassa.core.domain.api.model.common.CounterScopes
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmMode
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmState
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandStatus
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdServiceInfo
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.internal.IdGeneratorPort
import io.github.texport.superkassa.core.domain.api.port.internal.PinHasherPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.api.port.integration.inTransaction
import io.github.texport.superkassa.core.domain.api.port.internal.TokenCodecPort

import io.github.texport.superkassa.core.domain.impl.logging.getLogger

/**
 * Сценарий начальной инициализации регистрации ККМ.
 *
 * Выполняет сетевой опрос ОФД для проверки связи, инициализирует внутренние счетчики
 * на основе полученных из ОФД сумм (необнуляемые суммы по типам операций), а также
 * создает дефолтных пользователей (Администратор и Кассир) для свежезарегистрированной кассы.
 *
 * @property storage Порт для доступа к хранилищу данных.
 * @property clock Порт для работы с системным временем.
 * @property idGenerator Порт для генерации уникальных идентификаторов.
 * @property tokenCodec Порт для шифрования и дешифрования токенов доступа ОФД.
 * @property pinHasher Порт для хеширования ПИН-кодов пользователей.
 * @property kkmCommonHelper Вспомогательные общие методы для работы с ККМ.
 */
class InitializeKkmRegistrationUseCase(
    private val storage: StoragePort,
    private val clock: ClockPort,
    private val idGenerator: IdGeneratorPort,
    private val tokenCodec: TokenCodecPort,
    private val pinHasher: PinHasherPort,
    private val kkmCommonHelper: KkmCommonHelper
) {
    private val logger = getLogger(InitializeKkmRegistrationUseCase::class)

    private val registeredMode = KkmMode.REGISTRATION.name
    private val registeredState = KkmState.ACTIVE.name
    private val defaultAdminName = "Администратор"

    /**
     * Параметры инициализации ККМ.
     *
     * @property baseInfo Базовые сведения о кассе.
     * @property ofdToken Начальный строковый токен ОФД.
     * @property registrationNumber Регистрационный номер кассы.
     * @property factoryNumber Заводской номер кассы (опционально).
     * @property ofdTag Тег провайдера ОФД.
     * @property okedOverride Код ОКЭД, переопределяющий ответ ОФД (опционально).
     * @property updateKkm Callback для сохранения или обновления кассы в БД.
     */
    data class KkmInitializationParams(
        val baseInfo: KkmInfo,
        val ofdToken: String,
        val registrationNumber: String,
        val factoryNumber: String?,
        val ofdTag: String,
        val okedOverride: String?,
        /** Пин администратора новой кассы; `null` — прежний стандартный. */
        val adminPin: String? = null,
        val updateKkm: (KkmInfo) -> Unit
    )

    /**
     * Запускает сценарий инициализации регистрации.
     *
     * @param params Параметры инициализации ККМ.
     * @return Инициализированный и записанный объект [KkmInfo].
     * @throws ValidationException Если отсутствует заводской номер кассы или БФД
     * не ответил либо отказал (`OFD_COMMAND_FAILED`): тогда касса не записывается.
     */
    fun execute(params: KkmInitializationParams): KkmInfo {
        logger.info(
            "InitializeKkmRegistrationUseCase.execute: starting for systemId='{}', ofdTag='{}'",
            params.baseInfo.systemId,
            params.ofdTag
        )
        val serviceInfo = params.baseInfo.ofdServiceInfo ?: kkmCommonHelper.defaultServiceInfo()
        val factoryNum = params.factoryNumber ?: params.baseInfo.factoryNumber
            ?: throw ValidationException(CoreStrings.kkmFactoryRequired(), "KKM_FACTORY_REQUIRED")

        // 1. Проверяем связь и получаем актуальную информацию из ОФД
        val infoResult = performOfdSystemAndInfo(
            baseInfo = params.baseInfo,
            initialToken = tokenCodec.parseToken(params.ofdToken),
            serviceInfo = serviceInfo,
            registrationNumber = params.registrationNumber,
            factoryNumber = factoryNum,
            ofdTag = params.ofdTag
        )
        val now = clock.now()
        val rawResolvedServiceInfo = OfdResponseParser.extractServiceInfo(infoResult.responseJson, serviceInfo)

        // Корректируем и валидируем ОКЭД
        val resolvedServiceInfo = if (params.okedOverride != null) {
            rawResolvedServiceInfo.copy(orgOked = params.okedOverride)
        } else if (rawResolvedServiceInfo.orgOked == "00000" || rawResolvedServiceInfo.orgOked.isBlank()) {
            if (serviceInfo.orgOked.isNotBlank() && serviceInfo.orgOked != "00000") {
                rawResolvedServiceInfo.copy(orgOked = serviceInfo.orgOked)
            } else {
                rawResolvedServiceInfo
            }
        } else {
            rawResolvedServiceInfo
        }
        val updatedKkm = applyOfdInitialization(
            baseInfo = params.baseInfo,
            registrationNumber = params.registrationNumber,
            serviceInfo = resolvedServiceInfo,
            ofdTag = params.ofdTag,
            token = infoResult.responseToken ?: tokenCodec.parseToken(params.ofdToken),
            responseJson = infoResult.responseJson,
            updatedAt = now
        )

        // 2. В БД-транзакции создаем кассу, обновляем счетчики и создаем кассиров
        storage.inTransaction {
            params.updateKkm(updatedKkm)
            updateCountersFromOfdInfo(updatedKkm.id, infoResult.responseJson)
            ensureAdministrator(updatedKkm.id, clock.now(), params.adminPin)
        }
        logger.info(
            "InitializeKkmRegistrationUseCase.execute SUCCESS: registered kkmId='{}', systemId='{}'",
            updatedKkm.id,
            updatedKkm.systemId
        )
        return updatedKkm
    }

    /**
     * Выполняет последовательный опрос ОФД: сначала команду SYSTEM, затем INFO.
     *
     * Касса к этому моменту ещё не записана: без ответа БФД она и не
     * записывается, а вызывающий получает отказ с причиной.
     *
     * @return Результат выполнения INFO команды ОФД.
     * @throws ValidationException `OFD_COMMAND_FAILED`, если БФД не ответил или отказал.
     */
    fun performOfdSystemAndInfo(
        baseInfo: KkmInfo,
        initialToken: Long,
        serviceInfo: OfdServiceInfo,
        registrationNumber: String,
        factoryNumber: String,
        ofdTag: String
    ): OfdCommandResult {
        val systemResult = kkmCommonHelper.sendOfdCommand(
            kkm = baseInfo,
            commandType = OfdCommandType.SYSTEM,
            payloadRef = idGenerator.nextId(),
            tokenOverride = initialToken,
            serviceInfoOverride = serviceInfo,
            registrationNumberOverride = registrationNumber,
            factoryNumberOverride = factoryNumber,
            ofdProviderOverride = ofdTag
        )
        if (systemResult.status != OfdCommandStatus.OK) throw refused(baseInfo, OfdCommandType.SYSTEM, systemResult)

        val infoResult = kkmCommonHelper.sendOfdCommand(
            kkm = baseInfo,
            commandType = OfdCommandType.INFO,
            payloadRef = idGenerator.nextId(),
            tokenOverride = systemResult.responseToken ?: initialToken,
            serviceInfoOverride = serviceInfo,
            registrationNumberOverride = registrationNumber,
            factoryNumberOverride = factoryNumber,
            ofdProviderOverride = ofdTag
        )
        if (infoResult.status != OfdCommandStatus.OK) throw refused(baseInfo, OfdCommandType.INFO, infoResult)
        return infoResult
    }

    private fun refused(kkm: KkmInfo, command: OfdCommandType, result: OfdCommandResult): ValidationException {
        logger.warn(
            "Registration of systemId='{}' refused: BFD {} ${result.status}, code ${result.resultCode}",
            kkm.systemId,
            command
        )
        return registrationRefused(result)
    }

    /**
     * Применяет полученные из ОФД данные к объекту [KkmInfo].
     */
    fun applyOfdInitialization(
        baseInfo: KkmInfo,
        registrationNumber: String,
        serviceInfo: OfdServiceInfo,
        ofdTag: String,
        token: Long,
        responseJson: JsonObject?,
        updatedAt: Long
    ): KkmInfo {
        val shiftNo = OfdResponseParser.extractShiftNumber(responseJson)
        return baseInfo.copy(
            updatedAt = updatedAt,
            mode = registeredMode,
            state = registeredState,
            ofdProvider = ofdTag,
            registrationNumber = registrationNumber,
            ofdServiceInfo = serviceInfo,
            tokenEncryptedBase64 = tokenCodec.encodeToken(token),
            tokenUpdatedAt = updatedAt,
            lastShiftNo = shiftNo ?: baseInfo.lastShiftNo
        )
    }

    /**
     * Обновляет накопительные счетчики ККМ и состояние смены на основе отчета, полученного из ответа ОФД.
     */
    fun updateCountersFromOfdInfo(kkmId: String, responseJson: JsonObject?) {
        if (responseJson == null) return
        val snapshot = try {
            OfdInfoCountersSnapshotParser.parse(responseJson)
        } catch (_: Exception) {
            null
        }

        if (snapshot != null) {
            val now = clock.now()
            // 1. Сохраняем глобальные накопительные счетчики
            snapshot.globalCounters.forEach { (key, value) ->
                storage.upsertCounter(kkmId, CounterScopes.GLOBAL, null, key, value)
            }

            // 2. Если в ОФД смена открыта, автоматически открываем локальную смену
            if (snapshot.isOpenShift) {
                var localOpenShift = storage.findOpenShift(kkmId)
                if (localOpenShift == null) {
                    val shiftId = idGenerator.nextId()
                    val shiftNo = (snapshot.shiftNumber ?: 1).toLong()
                    val newShift = ShiftInfo(
                        id = shiftId,
                        kkmId = kkmId,
                        shiftNo = shiftNo,
                        status = ShiftStatus.OPEN,
                        openedAt = snapshot.openShiftTimeMillis ?: now,
                        closedAt = null
                    )
                    storage.createShift(newShift)
                    localOpenShift = newShift
                }
                val currentShiftId = localOpenShift.id
                snapshot.shiftCounters.forEach { (key, value) ->
                    storage.upsertCounter(kkmId, CounterScopes.SHIFT, currentShiftId, key, value)
                }
            }
        } else {
            val zxReport = OfdResponseParser.extractZxReport(responseJson) ?: return
            val nonNullable = zxReport["nonNullableSums"] as? JsonArray ?: return
            nonNullable.forEach { entry ->
                val obj = entry as? JsonObject ?: return@forEach
                val operation = obj["operation"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                val sumObj = obj["sum"] as? JsonObject ?: return@forEach
                val bills = sumObj["bills"]?.jsonPrimitive?.longOrNull ?: return@forEach
                val key = CounterKeyFormats.NON_NULLABLE_SUM.format(operation)
                storage.upsertCounter(kkmId, CounterScopes.GLOBAL, null, key, bills)
            }
        }
    }

    /**
     * Заводит администратора новой кассы, если пользователей ещё нет.
     *
     * Кассиров создаёт уже он сам: заведённый здесь кассир мог бы получить
     * только стандартный пин, а с ним узел не даёт ни одной команды —
     * в списке стоял бы человек, которым нельзя работать.
     *
     * @param kkmId Касса, которой нужен администратор.
     * @param now Время создания.
     * @param adminPin Пин администратора; пустой — код начальной настройки.
     */
    fun ensureAdministrator(kkmId: String, now: Long, adminPin: String? = null) {
        val existing = storage.listUsers(kkmId)
        if (existing.isNotEmpty()) return
        // Пин администратора задаёт тот, кто заводит кассу. Со стандартным
        // касса рождалась мёртвой: войти с ним узел не даёт, а сменить его
        // можно только войдя. Если пин не задан — прежнее поведение.
        val admin = adminPin?.takeIf { it.isNotBlank() } ?: StandardPin.BOOTSTRAP
        storage.createUser(
            kkmId = kkmId,
            userId = idGenerator.nextId(),
            name = defaultAdminName,
            role = UserRole.ADMIN,
            pinHash = pinHasher.hash(admin),
            createdAt = now
        )
    }
}
