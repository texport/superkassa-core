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
