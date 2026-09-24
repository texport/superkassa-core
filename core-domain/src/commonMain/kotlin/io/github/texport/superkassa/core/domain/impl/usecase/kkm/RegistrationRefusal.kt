package io.github.texport.superkassa.core.domain.impl.usecase.kkm

import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.impl.helper.ofd.bfdFailureReason
import io.github.texport.superkassa.core.string.api.TrilingualMessage

/**
 * Отказ в заведении кассы: БФД не ответил на служебную команду или отказал.
 *
 * Касса без ответа БФД не заводится: без него нет ни токена, ни реквизитов,
 * ни счётчиков, и «успех» оставлял бы владельца с кассой, которой нет в базе.
 *
 * Кассиру — причина и что делать; код БФД и сетевая ошибка остаются
 * журналу: их пишет [describeFailure].
 *
 * @param result ответ БФД или сбой связи.
 */
internal fun registrationRefused(result: OfdCommandResult): ValidationException =
    ValidationException(bfdFailureReason(result), "OFD_COMMAND_FAILED")

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
