package io.github.texport.superkassa.embedded.impl.document

/**
 * Шрифты печатных форм — одни на JVM и на Android.
 *
 * Шрифт лежит в ресурсах модуля, а не берётся из системы: чек выглядит
 * одинаково на любой машине. Noto Sans — пропорциональный, под SIL OFL 1.1,
 * лицензия рядом с файлами; в нём есть все буквы казахского алфавита, знак
 * тенге и «№», и у него настоящее полужирное начертание для итогов. Все
 * семейства, названные в стилях формы, отображаются на него: моноширинных
 * частей в формах нет — моноширинные имена в стилях лишь запасные.
 */
internal object PrintFonts {
    /** Каталог шрифтов в ресурсах модуля. */
    const val DIR: String = "/io/github/texport/superkassa/embedded/fonts/"

    /** Начертания: файл и толщина по CSS. */
    val FACES: List<Face> = listOf(
        Face(file = "NotoSans-Regular.ttf", weight = 400),
        Face(file = "NotoSans-Bold.ttf", weight = 700)
    )

    /** Семейства из стилей форм, которые рисуются шрифтом сборки. */
    val FAMILIES: List<String> = listOf(
        "Noto Sans", "Inter", "Roboto", "DejaVu Sans", "sans-serif", "Arial",
        "DejaVu Sans Mono", "Courier New", "monospace"
    )

    /** Начертание шрифта: файл в [DIR] и толщина `font-weight`. */
    data class Face(val file: String, val weight: Int)
}
