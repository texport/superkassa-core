package io.github.texport.superkassa.core.domain.impl.usecase.kkm

import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.impl.helper.ofd.bfdFailureReason

/**
 * Отказ в заведении кассы: БФД не ответил на служебную команду или отказал.
 *
 * Касса без ответа БФД не заводится: без него нет ни токена, ни реквизитов,
 * ни счётчиков, и «успех» оставлял бы владельца с кассой, которой нет в базе.
 *
 * Кассиру — причина и что делать; код БФД и сетевая ошибка остаются
 * журналу: их пишет describeFailure.
 *
 * @param result ответ БФД или сбой связи.
 */
internal fun registrationRefused(result: OfdCommandResult): ValidationException =
    ValidationException(bfdFailureReason(result), "OFD_COMMAND_FAILED")
