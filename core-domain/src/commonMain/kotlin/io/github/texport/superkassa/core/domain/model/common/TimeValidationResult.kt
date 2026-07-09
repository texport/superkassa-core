package io.github.texport.superkassa.core.domain.model.common

import io.github.texport.superkassa.core.string.api.TrilingualMessage

/**
 * Результат проверки корректности системного времени.
 *
 * @property ok Флаг успешности проверки (true, если время корректно).
 * @property reason Текстовая причина ошибки (null, если проверка пройдена).
 * @property trilingualMessage Трехъязычное сообщение об ошибке проверки времени.
 */
data class TimeValidationResult(
    val ok: Boolean,
    val reason: String? = null,
    val trilingualMessage: TrilingualMessage? = null
) {
    /** Сообщение на русском языке. */
    val messageRu: String? get() = trilingualMessage?.ru

    /** Сообщение на казахском языке. */
    val messageKk: String? get() = trilingualMessage?.kk

    /** Сообщение на английском языке. */
    val messageEn: String? get() = trilingualMessage?.en

    /**
     * Возвращает строковое представление сообщения на всех трех языках.
     */
    fun trilingualMessage(): String? {
        val msg = trilingualMessage ?: return null
        return "RU: ${msg.ru} | KK: ${msg.kk} | EN: ${msg.en}"
    }
}
