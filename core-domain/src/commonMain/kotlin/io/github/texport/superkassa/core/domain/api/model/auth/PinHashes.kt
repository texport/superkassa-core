package io.github.texport.superkassa.core.domain.api.model.auth

/** Сравнение хешей пина, по времени которого нельзя угадать, сколько символов совпало. */
object PinHashes {
    /**
     * Равны ли хеши; время зависит только от длины, а не от места первого расхождения.
     *
     * Обычное `==` обрывается на первом несовпавшем символе, и по времени
     * ответа перебирающий узнаёт, насколько близок его хеш к хешу кассира.
     */
    fun same(stored: String, offered: String): Boolean {
        var difference = stored.length xor offered.length
        for (i in 0 until maxOf(stored.length, offered.length)) {
            difference = difference or (stored.charAt(i).code xor offered.charAt(i).code)
        }
        return difference == 0
    }

    private fun String.charAt(i: Int): Char = if (i < length) this[i] else '\u0000'
}
