package io.github.texport.superkassa.core.domain.api.model.settings

/**
 * Ключ канала в описании настроек: есть он или нет, но не он сам.
 *
 * Описание настроек попадает в журнал и в сообщения об ошибках, а ключ
 * канала — такой же ключ, как токен БФД: кто его прочитал, тот шлёт
 * сообщения от имени кассы.
 */
internal fun hiddenSecret(value: String?): String = if (value == null) "null" else "***"
