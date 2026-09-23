package io.github.texport.superkassa.embedded.api

import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.integration.TimeValidatorPort
import kz.mybrain.network.OfdNetworkClient

/**
 * Сборка кассы с подменёнными внешними системами: БФД, часами и эталоном
 * времени в сети. Документы такой кассы уходят не в БФД, а туда, куда
 * укажет проверка, поэтому фискальной силы у них нет.
 *
 * Вызов требует явного `@OptIn(ReplacedExternals::class)` и без него не
 * компилируется: рабочий код приложения подмену случайно не включит,
 * а переключателей по переменным окружения у сборки нет.
 */
@RequiresOptIn(
    message = "Replaces the BFD, the clock and the reference time of the cash register. For tests only.",
    level = RequiresOptIn.Level.ERROR
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class ReplacedExternals

/**
 * Внешние системы кассы, которые проверка подставляет вместо настоящих.
 *
 * @property bfd транспорт до БФД вместо TCP-клиента.
 * @property clock часы кассы вместо системных.
 * @property timeGuard проверка часов вместо той, что сверяется с эталоном в сети.
 */
@ReplacedExternals
class Externals(
    val bfd: OfdNetworkClient,
    val clock: ClockPort,
    val timeGuard: TimeValidatorPort
)
