package io.github.texport.superkassa.delivery.channels.impl.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent

/**
 * Провайдер канала внутри проверки: запоминает запросы и отвечает заданным.
 *
 * @param status код ответа.
 * @param answer тело ответа.
 * @param failure сбой связи вместо ответа.
 */
internal class ProviderStub(
    private val status: HttpStatusCode = HttpStatusCode.OK,
    private val answer: String = """{"ok":true}""",
    private val failure: Exception? = null
) {
    val requests = mutableListOf<HttpRequestData>()

    private val engine = MockEngine { request ->
        requests += request
        failure?.let { throw it }
        respond(answer, status)
    }

    val clients = HttpClients { HttpClient(engine) }

    val request: HttpRequestData get() = requests.single()

    val body: String get() = (request.body as TextContent).text
}
