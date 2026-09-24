package io.github.texport.superkassa.testing.api.kassa

import io.github.texport.superkassa.core.domain.api.model.common.TimeValidationResult
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.integration.TimeValidatorPort
import io.github.texport.superkassa.delivery.api.port.DeliveryPort
import io.github.texport.superkassa.embedded.api.Externals
import io.github.texport.superkassa.embedded.api.ReplacedExternals
import io.github.texport.superkassa.embedded.api.Superkassa
import io.github.texport.superkassa.embedded.api.SuperkassaConfig
import io.github.texport.superkassa.embedded.api.SuperkassaPlatform
import io.github.texport.superkassa.embedded.api.createSuperkassa
import io.github.texport.superkassa.testing.api.bfd.FakeBfd
import io.github.texport.superkassa.testing.api.clock.MovableClock
import kz.mybrain.network.OfdNetworkClient
import kotlin.time.Duration.Companion.hours

/**
 * Настройки кассы для проверок: провайдер `KAZAKHTELECOM`, протокол 2.0.3 —
 * тот, на котором говорит [FakeBfd].
 *
 * Досылка очереди, доставка чеков и автозакрытие смены в фоне заходят раз
 * при открытии и потом раз в час: проверка ведёт их сама ([ReadyKassa.resendQueue],
 * [ReadyKassa.deliverReceipts], `SuperkassaApi.autoCloseShift`), и фон
 * не вмешивается в её шаги.
 *
 * @param channels каналы доставки чека вместо одноимённых из настроек — например,
 *   подменный SMS, который запоминает отправленное.
 */
fun testSuperkassaConfig(channels: List<DeliveryPort> = emptyList()): SuperkassaConfig = SuperkassaConfig(
    ofdProviderId = "KAZAKHTELECOM",
    ofdProtocolVersion = "203",
    ownerId = "testing",
    queueInterval = 1.hours,
    shiftCheckInterval = 1.hours,
    deliveryInterval = 1.hours,
    channels = channels
)

/**
 * Поднимает кассу встраиваемой сборкой, как `createSuperkassa`, но на БФД [bfd]
 * и часах [clock]; проверка часов по эталону в сети не выполняется.
 *
 * @param platform платформа с каталогом данных; каталог может быть пустым или
 *   оставленным прошлым открытием — тогда касса поднимается с его базой.
 * @param bfd транспорт до БФД, обычно [FakeBfd].
 * @param clock часы кассы.
 * @param config настройки кассы; протокол должен совпадать с тем, на котором говорит [bfd].
 * @throws IllegalStateException если каталог занят другим экземпляром или база не найдена
 *   там, где её ждали.
 */
@OptIn(ReplacedExternals::class)
fun createTestSuperkassa(
    platform: SuperkassaPlatform,
    bfd: OfdNetworkClient,
    clock: ClockPort = MovableClock(),
    config: SuperkassaConfig = testSuperkassaConfig()
): Superkassa = createSuperkassa(platform, config, Externals(bfd, clock, TrustedTime))

/** Часы проверки верны всегда: их переводит сама проверка, эталона в сети у неё нет. */
private object TrustedTime : TimeValidatorPort {
    override fun validate(clock: ClockPort) = TimeValidationResult(ok = true)
}
