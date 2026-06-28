package kz.mybrain.superkassa.core.domain.usecase.kkm

import kz.mybrain.superkassa.core.domain.helper.OfdResponseParser
import kz.mybrain.superkassa.core.domain.helper.KkmCommonHelper
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kz.mybrain.superkassa.core.domain.exception.ErrorMessages
import kz.mybrain.superkassa.core.domain.exception.ValidationException
import kz.mybrain.superkassa.core.domain.model.auth.UserRole
import kz.mybrain.superkassa.core.domain.model.common.CounterKeyFormats
import kz.mybrain.superkassa.core.domain.model.common.format
import kz.mybrain.superkassa.core.domain.model.common.CounterScopes
import kz.mybrain.superkassa.core.domain.model.kkm.KkmInfo
import kz.mybrain.superkassa.core.domain.model.kkm.KkmMode
import kz.mybrain.superkassa.core.domain.model.kkm.KkmState
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandResult
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandStatus
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandType
import kz.mybrain.superkassa.core.domain.model.ofd.OfdServiceInfo
import kz.mybrain.superkassa.core.domain.port.ClockPort
import kz.mybrain.superkassa.core.domain.port.IdGeneratorPort
import kz.mybrain.superkassa.core.domain.port.PinHasherPort
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.core.domain.port.TokenCodecPort

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
    private val registeredMode = KkmMode.REGISTRATION.name
    private val registeredState = KkmState.ACTIVE.name
    private val defaultAdminPin = "0000"
    private val defaultAdminName = "Администратор"
    private val defaultCashierName = "Кассир"

    /**
     * Параметры инициализации ККМ.
     *
     * @property baseInfo Базовые сведения о кассе.
     * @property ofdToken Начальный строковый токен ОФД.
     * @property registrationNumber Регистрационный номер кассы.
     * @property factoryNumber Заводской номер кассы (опционально).
     * @property ofdTag Тег провайдера ОФД.
     * @property okvedOverride Код ОКВЭД, переопределяющий ответ ОФД (опционально).
     * @property updateKkm Callback для сохранения или обновления кассы в БД.
     */
    data class KkmInitializationParams(
        val baseInfo: KkmInfo,
        val ofdToken: String,
        val registrationNumber: String,
        val factoryNumber: String?,
        val ofdTag: String,
        val okvedOverride: String?,
        val updateKkm: (KkmInfo) -> Unit
    )

    /**
     * Запускает сценарий инициализации регистрации.
     *
     * @param params Параметры инициализации ККМ.
     * @return Инициализированный объект [KkmInfo].
     * @throws ValidationException Если отсутствует заводской номер кассы.
     */
    fun execute(params: KkmInitializationParams): KkmInfo {
        val serviceInfo = params.baseInfo.ofdServiceInfo ?: kkmCommonHelper.defaultServiceInfo()
        val factoryNum = params.factoryNumber ?: params.baseInfo.factoryNumber
            ?: throw ValidationException(ErrorMessages.kkmFactoryRequired(), "KKM_FACTORY_REQUIRED")
        
        // 1. Проверяем связь и получаем актуальную информацию из ОФД
        val infoResult = performOfdSystemAndInfo(
            baseInfo = params.baseInfo,
            initialToken = tokenCodec.parseToken(params.ofdToken),
            serviceInfo = serviceInfo,
            registrationNumber = params.registrationNumber,
            factoryNumber = factoryNum,
            ofdTag = params.ofdTag
        ) ?: return params.baseInfo

        val now = clock.now()
        val rawResolvedServiceInfo = OfdResponseParser.extractServiceInfo(infoResult.responseJson, serviceInfo)
        
        // Корректируем и валидируем ОКВЭД
        val resolvedServiceInfo = if (params.okvedOverride != null) {
            rawResolvedServiceInfo.copy(orgOkved = params.okvedOverride)
        } else if (rawResolvedServiceInfo.orgOkved == "00000" || rawResolvedServiceInfo.orgOkved.isBlank()) {
            if (serviceInfo.orgOkved.isNotBlank() && serviceInfo.orgOkved != "00000") {
                rawResolvedServiceInfo.copy(orgOkved = serviceInfo.orgOkved)
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
            ensureDefaultUsers(updatedKkm.id, clock.now())
        }
        return updatedKkm
    }

    /**
     * Выполняет последовательный опрос ОФД: сначала команду SYSTEM, затем INFO.
     *
     * @return Результат выполнения INFO команды ОФД или null в случае ошибки.
     */
    fun performOfdSystemAndInfo(
        baseInfo: KkmInfo,
        initialToken: Long,
        serviceInfo: OfdServiceInfo,
        registrationNumber: String,
        factoryNumber: String,
        ofdTag: String
    ): OfdCommandResult? {
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
        if (systemResult.status != OfdCommandStatus.OK) return null
        
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
        if (infoResult.status != OfdCommandStatus.OK) {
            infoResult.responseToken?.let {
                storage.updateKkmToken(baseInfo.id, tokenCodec.encodeToken(it), clock.now())
            }
            return null
        }
        return infoResult
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
     * Обновляет накопительные счетчики ККМ на основе ZX-отчета, полученного из ответа ОФД.
     */
    fun updateCountersFromOfdInfo(kkmId: String, responseJson: JsonObject?) {
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

    /**
     * Создает в БД пользователей по умолчанию (Администратор и Кассир), если список пользователей пуст.
     */
    fun ensureDefaultUsers(kkmId: String, now: Long) {
        val existing = storage.listUsers(kkmId)
        if (existing.isNotEmpty()) return
        storage.createUser(
            kkmId = kkmId,
            userId = idGenerator.nextId(),
            name = defaultAdminName,
            role = UserRole.ADMIN,
            pin = defaultAdminPin,
            pinHash = pinHasher.hash(defaultAdminPin),
            createdAt = now
        )
        storage.createUser(
            kkmId = kkmId,
            userId = idGenerator.nextId(),
            name = defaultCashierName,
            role = UserRole.CASHIER,
            pin = "1111",
            pinHash = pinHasher.hash("1111"),
            createdAt = now
        )
    }
}
