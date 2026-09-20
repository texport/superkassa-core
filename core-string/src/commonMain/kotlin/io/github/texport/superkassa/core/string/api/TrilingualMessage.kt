package io.github.texport.superkassa.core.string.api

/**
 * Трехъязычное сообщение для мультиязычных ответов клиенту.
 *
 * Используется для локализованного вывода сообщений об ошибках, предупреждений или статусов
 * на русском, казахском и английском языках.
 *
 * @property ru Текст сообщения на русском языке.
 * @property kk Текст сообщения на казахском языке.
 * @property en Текст сообщения на английском языке.
 */
data class TrilingualMessage(
    val ru: String,
    val kk: String,
    val en: String
) {
    /**
     * Форматирует сообщения на всех трех языках с использованием переданных аргументов.
     *
     * Поведение аналогично стандартной функции [String.format].
     *
     * @param args Аргументы форматирования.
     * @return Новый объект [TrilingualMessage] с отформатированными строками.
     */
    fun format(vararg args: Any): TrilingualMessage = TrilingualMessage(
        ru = ru.format(*args),
        kk = kk.format(*args),
        en = en.format(*args)
    )

    /**
     * Возвращает компактную строку, объединяющую все три языка.
     */
    fun compact(): String = "RU: $ru | KK: $kk | EN: $en"

    /**
     * Оборачивает причину в это сообщение, не смешивая языки.
     *
     * Причина часто приходит уже трёхъязычной — строкой [compact]. Вставлять
     * её целиком в каждый из трёх языков нельзя: за несколько повторов
     * сообщение вырастало в стену вида «RU: … RU: … | KK: … | EN: …»,
     * где каждый язык нёс в себе все три.
     */
    fun wrapping(cause: String): TrilingualMessage {
        val parsed = ofCompact(cause) ?: return TrilingualMessage(
            ru = ru.replace(PLACEHOLDER, cause),
            kk = kk.replace(PLACEHOLDER, cause),
            en = en.replace(PLACEHOLDER, cause)
        )
        return TrilingualMessage(
            ru = ru.replace(PLACEHOLDER, parsed.ru),
            kk = kk.replace(PLACEHOLDER, parsed.kk),
            en = en.replace(PLACEHOLDER, parsed.en)
        )
    }

    /**
     * Возвращает локализованное сообщение на основе переданного языка.
     */
    fun localize(lang: String): String = when (lang.lowercase()) {
        "kk" -> kk
        "en" -> en
        else -> ru
    }

    companion object {
        /**
         * Создает сообщение с одинаковым текстом на всех трех языках.
         */
        fun mono(message: String): TrilingualMessage = TrilingualMessage(
            ru = message,
            kk = message,
            en = message
        )

        /** Место причины в заготовке сообщения. */
        const val PLACEHOLDER: String = "{}"

        /**
         * Разбирает строку [compact] обратно на три языка.
         *
         * @return `null`, если строка трёхъязычной не является.
         */
        fun ofCompact(text: String): TrilingualMessage? {
            val ru = text.substringAfter("RU: ", "").substringBefore(" | KK: ")
            val kk = text.substringAfter(" | KK: ", "").substringBefore(" | EN: ")
            val en = text.substringAfter(" | EN: ", "")
            if (ru.isEmpty() || kk.isEmpty() || en.isEmpty()) return null
            return TrilingualMessage(ru = ru, kk = kk, en = en)
        }
    }
}
