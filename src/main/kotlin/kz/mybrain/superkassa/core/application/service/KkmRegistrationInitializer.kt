package kz.mybrain.superkassa.core.application.service

import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.security.SecureRandom
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kz.mybrain.superkassa.core.application.error.ErrorMessages
import kz.mybrain.superkassa.core.application.error.ValidationException
import kz.mybrain.superkassa.core.application.policy.CounterKeyFormats
import kz.mybrain.superkassa.core.application.policy.CounterScopes
import kz.mybrain.superkassa.core.domain.model.KkmInfo
import kz.mybrain.superkassa.core.domain.model.KkmMode
import kz.mybrain.superkassa.core.domain.model.KkmState
import kz.mybrain.superkassa.core.domain.model.OfdCommandResult
import kz.mybrain.superkassa.core.domain.model.OfdCommandStatus
import kz.mybrain.superkassa.core.domain.model.OfdCommandType
import kz.mybrain.superkassa.core.domain.model.OfdServiceInfo
import kz.mybrain.superkassa.core.domain.model.UserRole
import kz.mybrain.superkassa.core.domain.port.ClockPort
import kz.mybrain.superkassa.core.domain.port.IdGenerator
import kz.mybrain.superkassa.core.domain.port.PinHasherPort
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.core.domain.port.TokenCodecPort

/**
 * Вспомогательный инициализатор для ККМ при регистрации.
 * Выделен из KkmRegistrationService для соблюдения лимитов на размер классов.
 */
class KkmRegistrationInitializer(
    private val storage: StoragePort,
    private val clock: ClockPort,
    private val idGenerator: IdGenerator,
    private val tokenCodec: TokenCodecPort,
    private val pinHasher: PinHasherPort,
    private val kkmCommonHelper: KkmCommonHelper
) {
    private val registeredMode = KkmMode.REGISTRATION.name
    private val registeredState = KkmState.ACTIVE.name
    private val defaultAdminPin = "0000"
    private val defaultAdminName = "Администратор"
    private val defaultCashierName = "Кассир"

    data class KkmInitializationParams(
        val baseInfo: KkmInfo,
        val ofdToken: String,
        val registrationNumber: String,
        val factoryNumber: String?,
        val ofdTag: String,
        val okvedOverride: String?,
        val updateKkm: (KkmInfo) -> Unit
    )

    fun performKkmInitialization(params: KkmInitializationParams): KkmInfo {
        val serviceInfo = params.baseInfo.ofdServiceInfo ?: kkmCommonHelper.defaultServiceInfo()
        val factoryNum = params.factoryNumber ?: params.baseInfo.factoryNumber
            ?: throw ValidationException(ErrorMessages.kkmFactoryRequired(), "KKM_FACTORY_REQUIRED")
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

        storage.inTransaction {
            params.updateKkm(updatedKkm)
            updateCountersFromOfdInfo(updatedKkm.id, infoResult.responseJson)
            ensureDefaultUsers(updatedKkm.id, clock.now())
        }
        return updatedKkm
    }

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

    fun generateFactoryNumber(): String {
        val year = currentYear() % 100
        val random = SecureRandom()
        val bytes = ByteArray(8)
        random.nextBytes(bytes)
        val hex = bytes.joinToString(separator = "") { "%02X".format(it) }.take(10)
        return "KZT%02d%s".format(year, hex)
    }

    fun currentYear(): Int {
        val now = clock.now()
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneOffset.UTC).year
    }
}
