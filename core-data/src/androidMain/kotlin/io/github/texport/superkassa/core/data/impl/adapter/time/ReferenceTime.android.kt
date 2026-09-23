package io.github.texport.superkassa.core.data.impl.adapter.time

import java.net.HttpURLConnection
import java.net.URI

private val REFERENCE_URLS = listOf("https://www.cloudflare.com", "https://www.google.com", "https://www.microsoft.com")
private const val TIMEOUT_MS = 1500

internal actual fun fetchReferenceTime(): Long? = REFERENCE_URLS.firstNotNullOfOrNull(::dateOf)

/** Время из заголовка `Date` одного сайта; отказ сети — `null`, а не ошибка. */
private fun dateOf(url: String): Long? {
    val connection = runCatching { URI.create(url).toURL().openConnection() as? HttpURLConnection }.getOrNull()
        ?: return null
    return try {
        connection.requestMethod = "HEAD"
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.connect()
        connection.getHeaderFieldDate("Date", 0L).takeIf { it > 0L }
    } catch (_: java.io.IOException) {
        null
    } finally {
        connection.disconnect()
    }
}
