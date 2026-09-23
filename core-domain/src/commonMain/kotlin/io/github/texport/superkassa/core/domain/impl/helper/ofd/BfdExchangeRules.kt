package io.github.texport.superkassa.core.domain.impl.helper.ofd

import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmState
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdServiceInfo

/**
 * Команды, несущие документ кассы.
 *
 * Документ без ответа БФД досылается из очереди с тем же номером запроса,
 * поэтому номер остаётся за ним. Служебные команды такого права не имеют.
 */
val DOCUMENT_COMMANDS: Set<OfdCommandType> = setOf(
    OfdCommandType.TICKET,
    OfdCommandType.MONEY_PLACEMENT,
    OfdCommandType.CLOSE_SHIFT,
    OfdCommandType.REPORT
)

/** Номер запроса тот же, что в предыдущем, а токен другой (CPCR, код 8). */
const val INVALID_REQUEST_NUMBER: Int = 8

/** Номер и токен те же, что в предыдущем, а команда другая (CPCR, код 9). */
const val INVALID_RETRY_REQUEST: Int = 9

/** Подмены реквизитов запроса: регистрация кассы шлёт то, чего в хранилище ещё нет. */
data class OfdRequestOverrides(
    val token: Long? = null,
    val serviceInfo: OfdServiceInfo? = null,
    val registrationNumber: String? = null,
    val factoryNumber: String? = null,
    val ofdProvider: String? = null
)

/**
 * Касса после ответа БФД на прямой запрос либо `null`, если менять нечего.
 *
 * Коды 18 и 19 добавлены протоколом 2.0.4: касса снята с учёта и касса
 * отключена от ОФД. Обе означают остановку работы. Код 2 — неверный
 * токен: прежний токен больше не годится, и касса его забывает.
 */
fun kkmStateAfterAnswer(kkm: KkmInfo, code: Int?, now: Long): KkmInfo? {
    val blocked = kkm.state == KkmState.BLOCKED.name
    return when {
        code == null -> null
        code in BLOCKING_CODES && !(blocked && kkm.blockReasonCode == code + BLOCK_REASON_BASE) -> kkm.copy(
            updatedAt = now,
            state = KkmState.BLOCKED.name,
            blockReasonCode = code + BLOCK_REASON_BASE,
            tokenEncryptedBase64 = if (code == INVALID_TOKEN) null else kkm.tokenEncryptedBase64
        )
        code == OK && blocked -> kkm.copy(updatedAt = now, state = KkmState.ACTIVE.name, blockReasonCode = null)
        else -> null
    }
}

private const val OK = 0
private const val INVALID_TOKEN = 2

/** Причина блокировки по ответу БФД — код ответа плюс это основание. */
private const val BLOCK_REASON_BASE = 1000

private val BLOCKING_CODES = setOf(1, 2, 3, 4, 5, 6, 7, 11, 12, 15, 18, 19)
