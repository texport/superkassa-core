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
    }
}
