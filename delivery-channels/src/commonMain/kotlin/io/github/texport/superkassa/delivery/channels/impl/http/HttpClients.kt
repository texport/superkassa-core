package io.github.texport.superkassa.delivery.channels.impl.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout

/**
 * Откуда канал берёт HTTP-клиент на одну отправку.
 *
 * Клиент открывается на отправку и закрывается после неё: чек уходит
 * покупателю раз в несколько минут, а долгоживущий клиент держал бы потоки
 * и соединения всё время работы кассы, и закрывать его было бы некому.
 */
internal fun interface HttpClients {
    fun open(): HttpClient

    companion object {
        /** Сколько канал ждёт провайдера: кассир ждёт ответа доставки у экрана. */
        private const val REQUEST_TIMEOUT_MILLIS = 10_000L
        private const val CONNECT_TIMEOUT_MILLIS = 5_000L

        /** Клиент на движке платформы с пределом ожидания. */
        val Default = HttpClients {
            HttpClient(platformHttpEngine()) {
                install(HttpTimeout) {
                    requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                    connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
                }
            }
        }
    }
}

/** Движок HTTP платформы: OkHttp на JVM и Android, Darwin на iOS. */
internal expect fun platformHttpEngine(): HttpClientEngineFactory<*>
