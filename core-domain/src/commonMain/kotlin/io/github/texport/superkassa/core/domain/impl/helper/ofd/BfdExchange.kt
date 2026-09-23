package io.github.texport.superkassa.core.domain.impl.helper.ofd

import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandStatus
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdServiceInfo
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.api.port.internal.OfdManagerPort
import io.github.texport.superkassa.core.domain.api.port.internal.TokenCodecPort
import io.github.texport.superkassa.core.domain.impl.logging.getLogger
import io.github.texport.superkassa.core.domain.impl.usecase.ofd.GenerateRequestNumberUseCase
import io.github.texport.superkassa.core.string.api.CoreStrings

/**
 * Один обмен кассы с БФД: номер запроса, отправка, исход номера,
 * состояние и токен кассы по ответу.
 *
 * Вызывается внутри транзакции: строка кассы блокируется от выбора номера
 * до записи его исхода, и второй обмен той же кассы ждёт первого.
 */
class BfdExchange(
    private val storage: StoragePort,
    private val clock: ClockPort,
    private val tokenCodec: TokenCodecPort,
    private val requestNumbers: GenerateRequestNumberUseCase,
    private val requestFactory: OfdCommandRequestFactory,
    private val ofd: OfdManagerPort,
    private val defaultServiceInfo: () -> OfdServiceInfo
) {
    private val logger = getLogger(BfdExchange::class)

    fun send(
        kkm: KkmInfo,
        commandType: OfdCommandType,
        payloadRef: String,
        overrides: OfdRequestOverrides,
        updateToken: Boolean
    ): OfdCommandResult {
        // Касса из хранилища, а не из аргумента: пока ждали своей очереди,
        // предыдущий обмен мог сменить токен.
        val current = storage.findKkmForUpdate(kkm.id) ?: kkm
        deferredWhileUnanswered(current.id, commandType)?.let { return it }
        val token = overrides.token
            ?: tokenCodec.decodeToken(current.tokenEncryptedBase64)
            ?: throw ValidationException(CoreStrings.ofdTokenRequired(), "OFD_TOKEN_REQUIRED")
        val reqNum = requestNumbers.execute(current.id, persist = false)
        val now = clock.now()
        logger.debug("Sending {} to BFD, reqNum {}", commandType, reqNum)
        val result = ofd.send(buildRequest(kkm, commandType, payloadRef, overrides, token, reqNum, now))
        logger.debug("BFD answered {} with code {}", commandType, result.resultCode)
        settleRequestNumber(current.id, commandType, reqNum, result)
        applyAnswer(current, result, now, updateToken)
        return result
    }

    /**
     * Служебная команда ждёт, пока документ без ответа не будет дослан.
     *
     * Спецификация, п. 5.2: при восстановлении связи первой уходит последняя
     * неуспешная команда. Служебная команда со старым токеном, когда БФД
     * документ уже учёл, получила бы «неверный токен» и заблокировала кассу.
     */
    private fun deferredWhileUnanswered(kkmId: String, commandType: OfdCommandType): OfdCommandResult? {
        if (commandType in DOCUMENT_COMMANDS || requestNumbers.unanswered(kkmId) == null) return null
        logger.info("BFD {} for kkm {} deferred: a document is awaiting its answer", commandType, kkmId)
        return OfdCommandResult(status = OfdCommandStatus.FAILED, errorMessage = CoreStrings.ofdUnansweredDocument())
    }

    private fun buildRequest(
        kkm: KkmInfo,
        commandType: OfdCommandType,
        payloadRef: String,
        overrides: OfdRequestOverrides,
        token: Long,
        reqNum: Int,
        now: Long
    ) = requestFactory.build(
        kkm = kkm, commandType = commandType, payloadRef = payloadRef, token = token, reqNum = reqNum, now = now,
        serviceInfoOverride = overrides.serviceInfo,
        registrationNumberOverride = overrides.registrationNumber,
        factoryNumberOverride = overrides.factoryNumber,
        ofdProviderOverride = overrides.ofdProvider,
        defaultServiceInfo = defaultServiceInfo
    )

    /** Состояние и токен кассы по ответу. Ответ 8 или 9 их не трогает: запрос повторят. */
    private fun applyAnswer(current: KkmInfo, result: OfdCommandResult, now: Long, updateToken: Boolean) {
        val code = result.resultCode
        if (code == INVALID_REQUEST_NUMBER || code == INVALID_RETRY_REQUEST) return
        kkmStateAfterAnswer(current, code, now)?.let { storage.updateKkm(it) }
        if (updateToken) {
            result.responseToken?.let { storage.updateKkmToken(current.id, tokenCodec.encodeToken(it), now) }
        }
    }

    /**
     * Записывает исход номера запроса.
     *
     * Ответ получен — номер израсходован. Документ ушёл без ответа — номер
     * остаётся за ним до повтора (CPCR, п. 5.1). Служебный запрос без
     * ответа номер тоже расходует: повторять его никто не обязан, а занять
     * его номер следующему документу нельзя.
     */
    private fun settleRequestNumber(kkmId: String, commandType: OfdCommandType, reqNum: Int, result: OfdCommandResult) {
        when {
            result.resultCode != null -> requestNumbers.commit(kkmId, reqNum)
            result.status != OfdCommandStatus.TIMEOUT -> Unit
            commandType in DOCUMENT_COMMANDS -> requestNumbers.markUnanswered(kkmId, reqNum)
            else -> requestNumbers.commit(kkmId, reqNum)
        }
    }
}
