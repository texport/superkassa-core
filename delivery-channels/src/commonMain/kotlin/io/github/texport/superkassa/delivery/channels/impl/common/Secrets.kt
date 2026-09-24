package io.github.texport.superkassa.delivery.channels.impl.common

import io.ktor.http.encodeURLParameter

/**
 * Прячет ключи каналов в тексте, который уходит в журнал или наружу.
 *
 * Ключ канала — такой же ключ, как токен БФД: кто его прочитал, тот шлёт
 * сообщения от имени кассы. Живёт он в адресе запроса (Telegram держит токен
 * бота в пути, SMS-шлюзы — в параметре) и в заголовке, а адрес попадает
 * в ответ провайдера. Поэтому прячутся и сами известные ключи — как есть
 * и в виде параметра адреса, — и всё, что похоже на ключ по месту в тексте.
 *
 * @param values ключи канала; пустые не учитываются.
 */
internal class Secrets(values: List<String?>) {
    private val known = values.filterNotNull()
        .filter { it.isNotBlank() }
        .flatMap { listOf(it, it.encodeURLParameter()) }
        .distinct()
        .sortedByDescending { it.length }

    fun mask(text: String): String {
        var masked = text
        known.forEach { masked = masked.replace(it, HIDDEN) }
        return masked
            .replace(inPath, "/bot$HIDDEN")
            .replace(inQuery) { "${it.groupValues[1]}=$HIDDEN" }
            .replace(inHeader, "Bearer $HIDDEN")
    }
}

private const val HIDDEN = "***"

/** Токен в пути: `/bot<токен>/sendMessage`. */
private val inPath = Regex("""/bot[^/\s"']+""", RegexOption.IGNORE_CASE)

/** Ключ в параметрах запроса: `?api_key=<ключ>&text=…`. */
private val inQuery = Regex(
    """\b(api[_-]?key|access[_-]?token|auth[_-]?token|token|key|secret|password)=[^&\s"'#]+""",
    RegexOption.IGNORE_CASE
)

/** Ключ в заголовке авторизации. */
private val inHeader = Regex("""\bBearer\s+[^\s"']+""", RegexOption.IGNORE_CASE)
