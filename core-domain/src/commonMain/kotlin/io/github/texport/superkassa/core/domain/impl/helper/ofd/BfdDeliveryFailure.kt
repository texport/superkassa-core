package io.github.texport.superkassa.core.domain.impl.helper.ofd

import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandStatus
import io.github.texport.superkassa.core.domain.impl.logging.getLogger
import io.github.texport.superkassa.core.string.api.TrilingualMessage

/**
 * Почему документ не дошёл до БФД — в ответе кассиру на чек, деньги и отчёт.
 *
 * Кассиру — причина и что делать, на каждом языке свой текст, из таблицы
 * кодов БФД; код ответа БФД — отдельным полем; сетевая ошибка и текст
 * обмена — только журналу, по-английски. Прежде в ответ уходила строка
 * обмена целиком: «RU: … | KK: … | EN: BFD request failed: …» —
 * три языка в одном поле и английский текст исключения.
 */
internal object BfdDeliveryFailure {
    private val logger = getLogger(BfdDeliveryFailure::class)

    /**
     * Причина для кассира или `null`, если отказа не было.
     *
     * Документ, поставленный в очередь без попытки обмена (очередь
     * не пуста), отказом не считается: БФД его ещё не видел.
     *
     * @param result ответ БФД или сбой обмена.
     */
    fun reason(result: OfdCommandResult): TrilingualMessage? {
        if (!failed(result)) return null
        logger.warn("BFD did not accept the document: {}", describeFailure(result))
        return bfdFailureReason(result)
    }

    /** Код ответа БФД при отказе; `null`, если БФД принял документ или не ответил вовсе. */
    fun code(result: OfdCommandResult): Int? = result.resultCode?.takeIf { failed(result) && it != RESULT_OK }

    private fun failed(result: OfdCommandResult): Boolean = when (result.status) {
        OfdCommandStatus.OK -> false
        OfdCommandStatus.FAILED -> true
        OfdCommandStatus.TIMEOUT -> result.resultCode != null || result.errorMessage != null
    }

    private const val RESULT_OK = 0
}
