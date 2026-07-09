package io.github.texport.superkassa.core.data.util

/**
 * Создает ассоциативную карту (Map) для iOS.
 * Так как на iOS нет ConcurrentHashMap, возвращает стандартный HashMap.
 *
 * @param K Тип ключей в карте.
 * @param V Тип значений в карте.
 * @return Мутабельная карта HashMap.
 */
actual fun <K, V> createConcurrentMap(): MutableMap<K, V> {
    return HashMap()
}
