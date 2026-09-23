package io.github.texport.superkassa.embedded.impl.document

import kotlin.math.roundToLong

/**
 * Печатная форма, подготовленная к рисованию без браузера.
 *
 * Лента занимает всю ширину бумаги на белом: покупателю достаётся чек,
 * а не экран кассы с фоном и тенью. Правила `@media print` выключены —
 * документ показывает то же, что владелец видит на экране.
 */
internal object PrintForm {
    /** Точек CSS в миллиметре: 96 точек на дюйм. */
    const val PX_PER_MM: Double = 96 / 25.4

    private const val TAPE_80_MM = 80.0
    private const val TAPE_58_MM = 58.0
    private const val FULLSCREEN_PX = 900
    private const val HUNDREDTHS = 100.0

    private val NARROW_TAPE = Regex("""class="[^"]*\btape-58mm\b""")
    private val WIDE_FORM = Regex("""class="[^"]*\btape-fullscreen\b""")

    /**
     * Ширина бумаги по классу корневого элемента формы.
     *
     * Класс ищется на элементе, а не в стилях: таблица стилей описывает
     * все ленты сразу, и подстрока «58mm» нашлась бы в любом чеке.
     */
    fun paperWidthMm(html: String): Double = when {
        NARROW_TAPE.containsMatchIn(html) -> TAPE_58_MM
        WIDE_FORM.containsMatchIn(html) -> FULLSCREEN_PX / PX_PER_MM
        else -> TAPE_80_MM
    }

    /**
     * Страница печати: форма во всю ширину бумаги.
     *
     * @param pageHeightMm высота листа; `null` — лист не задаётся.
     */
    fun printable(html: String, paperWidthMm: Double, pageHeightMm: Double?): String {
        val paper = mm(paperWidthMm)
        val page = pageHeightMm?.let { "@page { size: ${paper}mm ${mm(it)}mm; margin: 0; }" }.orEmpty()
        val style = """
            <style>
            $page
            html, body { width: ${paper}mm !important; margin: 0 !important; padding: 0 !important;
                background: #ffffff !important; }
            .receipt { margin: 0 auto !important; width: ${paper}mm !important; max-width: ${paper}mm !important;
                box-shadow: none !important; }
            .receipt::after { display: none !important; }
            </style>
        """.trimIndent()
        return html.replace("@media print", "@media print_disabled").withHead(style)
    }

    private fun mm(value: Double): String = ((value * HUNDREDTHS).roundToLong() / HUNDREDTHS).toString()

    /** Обрывок без `<head>` оборачивается страницей — с кодировкой, иначе кириллица портится. */
    private fun String.withHead(inject: String): String =
        if (contains("</head>")) {
            replace("</head>", "$inject\n</head>")
        } else {
            """<!DOCTYPE html><html><head><meta charset="UTF-8"/>$inject</head><body>$this</body></html>"""
        }
}
