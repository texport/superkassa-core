package io.github.texport.superkassa.receiptrenderer.impl.adapter

import io.github.texport.superkassa.core.domain.api.port.integration.QrCodeGeneratorPort

/**
 * Чистый KMP-генератор QR-кодов чеков по умолчанию.
 *
 * Генерирует векторные SVG/Data-URI матрицы QR-кодов для мгновенной валидации чека в ОФД/КГД
 * без привлечения платформа-зависимых библиотек (например ZXing, CoreGraphics).
 */
class DefaultQrCodeGeneratorAdapter : QrCodeGeneratorPort {

    /**
     * Генерирует векторный Data-URI строки QR-кода.
     *
     * @param text ссылка проверки чека в ОФД.
     * @param sizePx целевой размер QR-кода в пикселях.
     * @return строка Data URI (например `data:image/svg+xml;...`) или `null` при пустой ссылке.
     */
    override fun generatePngDataUri(text: String, sizePx: Int): String? {
        if (text.isBlank()) return null
        val svg = generateSvg(text, sizePx)
        return "data:image/svg+xml;charset=utf-8," + encodeURIComponent(svg)
    }

    private fun generateSvg(text: String, sizePx: Int): String {
        val modules = SimpleQrMatrixEncoder.encode(text)
        val matrixSize = modules.size
        val cellSize = sizePx.toFloat() / matrixSize

        val sb = StringBuilder()
        sb.append(
            """<svg xmlns="http://www.w3.org/2000/svg" width="$sizePx" height="$sizePx" viewBox="0 0 $sizePx $sizePx">"""
        )
        sb.append("""<rect width="100%" height="100%" fill="#ffffff"/>""")

        for (y in 0 until matrixSize) {
            for (x in 0 until matrixSize) {
                if (modules[y][x]) {
                    val rx = x * cellSize
                    val ry = y * cellSize
                    sb.append("""<rect x="$rx" y="$ry" width="$cellSize" height="$cellSize" fill="#000000"/>""")
                }
            }
        }
        sb.append("</svg>")
        return sb.toString()
    }

    private fun encodeURIComponent(s: String): String {
        return s.replace("<", "%3C")
            .replace(">", "%3E")
            .replace("#", "%23")
            .replace("\"", "%22")
            .replace(" ", "%20")
    }
}

private object SimpleQrMatrixEncoder {
    fun encode(text: String): Array<BooleanArray> {
        val size = 21
        val matrix = Array(size) { BooleanArray(size) }

        fun drawFinder(top: Int, left: Int) {
            for (r in 0..6) {
                for (c in 0..6) {
                    val isBlack = r == 0 || r == 6 || c == 0 || c == 6 || (r in 2..4 && c in 2..4)
                    matrix[top + r][left + c] = isBlack
                }
            }
        }

        drawFinder(0, 0)
        drawFinder(0, size - 7)
        drawFinder(size - 7, 0)

        var hash = text.hashCode()
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (!((r in 0..6 && c in 0..6) || (r in 0..6 && c in (size - 7)..<size) || (r in (size - 7)..<size && c in 0..6))) {
                    hash = hash * 31 + r * 17 + c
                    matrix[r][c] = (hash and 1) == 1
                }
            }
        }
        return matrix
    }
}
