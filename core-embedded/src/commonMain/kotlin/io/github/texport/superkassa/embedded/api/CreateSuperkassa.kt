package io.github.texport.superkassa.embedded.api

import io.github.texport.superkassa.embedded.impl.EmbeddedSuperkassa

/**
 * Поднимает кассу в процессе приложения.
 *
 * Отказывает, а не начинает с чистого листа, если каталог занят другим
 * экземпляром, если база не открывается или если настройки на месте,
 * а базы нет: последнее значит перенесённый или неверно указанный
 * каталог, и пустая база спрятала бы потерю смен и чеков.
 *
 * @throws IllegalStateException если каталог занят или база не найдена там, где её ждали.
 */
fun createSuperkassa(platform: SuperkassaPlatform, config: SuperkassaConfig): Superkassa =
    EmbeddedSuperkassa.open(platform, config, ofdTransport = null)

/**
 * Поднимает кассу так же, как [createSuperkassa], но на внешних системах
 * проверки: БФД, часы и проверка часов берутся из [externals].
 *
 * Сборка, база, очередь и автозакрытие — те же, что в рабочем пути;
 * подменяется только то, что уходит за пределы процесса.
 *
 * @throws IllegalStateException если каталог занят, база не найдена там, где её ждали,
 *   или [Externals.timeGuard] не принял часы [Externals.clock].
 */
@ReplacedExternals
fun createSuperkassa(platform: SuperkassaPlatform, config: SuperkassaConfig, externals: Externals): Superkassa =
    EmbeddedSuperkassa.open(platform, config, externals.bfd, externals.timeGuard, externals.clock)
