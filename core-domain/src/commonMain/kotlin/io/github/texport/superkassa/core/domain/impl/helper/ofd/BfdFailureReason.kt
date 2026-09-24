package io.github.texport.superkassa.core.domain.impl.helper.ofd

import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandStatus
import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.string.api.TrilingualMessage

/**
 * Почему БФД не выполнил команду — словами для кассира, на каждом языке свой текст.
 *
 * Код ответа БФД и текст сетевой ошибки сюда не входят: кассиру они ничего
 * не говорят, а их место — журнал и код ошибки. Раньше в каждый язык
 * вставлялось всё описание обмена целиком, и кассир видел три языка сразу
 * вместе с английским «BFD response timeout».
 *
 * @param result ответ БФД с отказом или сбой обмена.
 */
internal fun bfdFailureReason(result: OfdCommandResult): TrilingualMessage {
    val code = result.resultCode
    return when {
        code != null && code != 0 -> CoreStrings.bfdRefusal(code)
        result.status == OfdCommandStatus.TIMEOUT -> CoreStrings.bfdNoAnswer()
        else -> CoreStrings.bfdRequestNotSent()
    }
}

/**
 * Складывает описание отказа ОФД из того, что он действительно прислал.
 *
 * Сетевое поле errorMessage пусто, когда ОФД ответил и отказал по существу:
 * причина тогда лежит в коде и тексте результата. Раньше в журнал и
 * оператору уходило пустое место, и отличить отказ стенда от обрыва связи
 * было нельзя. Сетевая ошибка берётся по-английски: журнал ведётся
 * на английском, а трёхъязычная строка раздувала его втрое.
 */
internal fun describeFailure(result: OfdCommandResult): String {
    val parts = listOfNotNull(
        result.status.name,
        result.resultCode?.let { "code=$it" },
        result.resultText?.takeIf { it.isNotBlank() },
        result.errorMessage?.takeIf { it.isNotBlank() }?.let { TrilingualMessage.ofCompact(it)?.en ?: it }
    )
    return parts.joinToString(", ")
}
