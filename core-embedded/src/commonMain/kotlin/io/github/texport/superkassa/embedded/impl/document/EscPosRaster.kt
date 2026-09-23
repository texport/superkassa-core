package io.github.texport.superkassa.embedded.impl.document

/**
 * Чек для термопринтера растром ESC/POS.
 *
 * Растр, а не текст: казахских букв нет в кодовых страницах принтеров,
 * и текстом чек печатался бы с вопросами вместо «ә», «ғ», «қ». Картинка
 * печатается одинаково на любом принтере, понимающем `GS v 0`.
 */
internal object EscPosRaster {
    /** Точек печати на ширину ленты: 72 мм у ленты 80 мм и 48 мм у ленты 58 мм, по 8 точек на миллиметр. */
    fun printableDots(paperWidthMm: Int): Int = if (paperWidthMm >= WIDE_PAPER_MM) WIDE_DOTS else NARROW_DOTS

    /**
     * Кодирует изображение.
     *
     * @param width ширина в точках.
     * @param height высота в точках.
     * @param dark тёмная ли точка `(x, y)`.
     * @return команды принтеру: сброс, растр полосами, прогон и отрез.
     */
    fun encode(width: Int, height: Int, dark: (Int, Int) -> Boolean): ByteArray {
        val rowBytes = (width + BITS - 1) / BITS
        val out = ArrayList<Byte>(rowBytes * height + TAIL.size + INIT.size + height / BAND * HEADER)
        INIT.forEach { out += it }
        var top = 0
        while (top < height) {
            val rows = minOf(BAND, height - top)
            header(rowBytes, rows).forEach { out += it }
            for (y in top until top + rows) row(y, width, rowBytes, dark).forEach { out += it }
            top += rows
        }
        TAIL.forEach { out += it }
        return out.toByteArray()
    }

    private fun header(rowBytes: Int, rows: Int): ByteArray = byteArrayOf(
        GS,
        'v'.code.toByte(),
        '0'.code.toByte(),
        0,
        (rowBytes and LOW).toByte(),
        (rowBytes shr BYTE).toByte(),
        (rows and LOW).toByte(),
        (rows shr BYTE).toByte()
    )

    private fun row(y: Int, width: Int, rowBytes: Int, dark: (Int, Int) -> Boolean): ByteArray {
        val bytes = ByteArray(rowBytes)
        for (x in 0 until width) {
            if (dark(x, y)) {
                val index = x / BITS
                bytes[index] = (bytes[index].toInt() or (HIGH_BIT shr (x % BITS))).toByte()
            }
        }
        return bytes
    }

    private const val GS: Byte = 0x1D
    private const val BITS = 8
    private const val BYTE = 8
    private const val LOW = 0xFF
    private const val HIGH_BIT = 0x80
    private const val HEADER = 8
    private const val BAND = 256
    private const val WIDE_PAPER_MM = 80
    private const val WIDE_DOTS = 576
    private const val NARROW_DOTS = 384
    private val INIT = byteArrayOf(0x1B, '@'.code.toByte())
    private val TAIL = byteArrayOf(0x1B, 'd'.code.toByte(), 4, GS, 'V'.code.toByte(), 66, 0)
}
