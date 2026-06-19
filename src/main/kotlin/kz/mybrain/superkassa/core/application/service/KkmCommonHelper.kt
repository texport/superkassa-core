package kz.mybrain.superkassa.core.application.service

import kz.mybrain.superkassa.core.application.error.ErrorMessages
import kz.mybrain.superkassa.core.application.error.ValidationException
import kz.mybrain.superkassa.core.domain.model.KkmInfo
import kz.mybrain.superkassa.core.domain.model.OfdCommandResult
import kz.mybrain.superkassa.core.domain.model.OfdCommandType
import kz.mybrain.superkassa.core.domain.model.OfdServiceInfo
import kz.mybrain.superkassa.core.domain.port.ClockPort
import kz.mybrain.superkassa.core.domain.port.OfdManagerPort
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.core.domain.port.TimeValidatorPort
import kz.mybrain.superkassa.core.domain.port.TokenCodecPort

/**
 * Вспомогательный класс с общей логикой и утилитами для работы с ККМ и ОФД.
 */
class KkmCommonHelper(
    private val storage: StoragePort,
    private val clock: ClockPort,
    private val timeValidator: TimeValidatorPort,
    private val tokenCodec: TokenCodecPort,
    private val reqNumService: ReqNumService,
    private val ofdCommandRequestBuilder: OfdCommandRequestBuilder,
    private val ofd: OfdManagerPort
) {
    fun ensureSystemTimeValid() {
        val result = timeValidator.validate(clock)
        if (!result.ok) {
            throw ValidationException(ErrorMessages.systemTimeInvalid(), "SYSTEM_TIME_INVALID")
        }
    }

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
            serviceInfoOverride = serviceInfoOverride,
            registrationNumberOverride = registrationNumberOverride,
            factoryNumberOverride = factoryNumberOverride,
            ofdProviderOverride = ofdProviderOverride,
            defaultServiceInfo = ::defaultServiceInfo
        )
        val result = ofd.send(request)
        if (updateToken && result.responseToken != null) {
            storage.updateKkmToken(kkm.id, tokenCodec.encodeToken(result.responseToken), now)
        }
        return result
    }
}
