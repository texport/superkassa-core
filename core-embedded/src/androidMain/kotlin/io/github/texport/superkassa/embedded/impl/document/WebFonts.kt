package io.github.texport.superkassa.embedded.impl.document

import android.webkit.WebResourceResponse

/**
 * Шрифты сборки для WebView: правила `@font-face` и выдача файлов странице.
 *
 * WebView не видит ресурсы модуля, поэтому страница грузится от адреса в
 * зарезервированном домене `.invalid`, который никогда не уходит в сеть, а
 * файлы шрифтов по нему отдаются из ресурсов. Иначе форма рисовалась бы
 * системным шрифтом устройства — у каждой модели своим.
 */
internal object WebFonts {
    /** Адрес, от которого загружается страница формы. */
    const val BASE_URL: String = "https://superkassa.invalid/"

    private const val PATH = "fonts/"
    private const val MIME = "font/ttf"
    private const val NOT_FOUND = 404

    /** Страница печати со шрифтами сборки для всех семейств из стилей формы. */
    fun styled(page: String): String = page.replaceFirst("</head>", "<style>\n${faces()}\n</style>\n</head>")

    /**
     * Ответ на запрос страницы: файл шрифта по адресу из [styled]. Прочие адреса
     * страницы (например, значок сайта) — «не найдено» без выхода в сеть; чужой
     * адрес — `null`, его WebView грузит сам (логотип в форме бывает внешним).
     */
    fun response(url: String): WebResourceResponse? {
        if (!url.startsWith(BASE_URL)) return null
        val face = PrintFonts.FACES.firstOrNull { url == BASE_URL + PATH + it.file }
            ?: return WebResourceResponse("text/plain", null, NOT_FOUND, "Not Found", emptyMap(), null)
        val stream = checkNotNull(WebFonts::class.java.getResourceAsStream(PrintFonts.DIR + face.file)) {
            "Font ${face.file} is missing"
        }
        return WebResourceResponse(MIME, null, stream)
    }

    private fun faces(): String = PrintFonts.FAMILIES.flatMap { family ->
        PrintFonts.FACES.map { face ->
            """@font-face { font-family: "$family"; src: url("$PATH${face.file}"); font-weight: ${face.weight}; }"""
        }
    }.joinToString("\n")
}
