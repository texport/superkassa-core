package kz.mybrain.superkassa.core.domain.exception

import kotlinx.serialization.Serializable
import kz.mybrain.superkassa.core.domain.model.common.format

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
@Serializable
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
}
