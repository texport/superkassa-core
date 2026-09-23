package io.github.texport.superkassa.core.data.impl.adapter.time

/**
 * Эталона на iOS пока нет.
 *
 * Без эталона проверка всё равно отвергает перевод часов назад и скачок
 * вперёд больше двух минут; расхождение с сетью здесь не проверяется.
 */
internal actual fun fetchReferenceTime(): Long? = null
