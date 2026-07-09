package io.github.texport.superkassa.core.data.util

import java.util.concurrent.ConcurrentHashMap

/**
 * Создает потокобезопасную ассоциативную карту (Map) для многопоточной среды.
 * Возвращает [ConcurrentHashMap], оптимизированный для JVM.
 *
 * @param K Тип ключей в карте.
 * @param V Тип значений в карте.
 * @return Потокобезопасная мутабельная карта ConcurrentHashMap.
 */
actual fun <K, V> createConcurrentMap(): MutableMap<K, V> {
    return ConcurrentHashMap()
}
