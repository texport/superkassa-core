package io.github.texport.superkassa.core.data.util

/**
 * Создает потокобезопасную ассоциативную карту (Map) для многопоточной среды.
 * На JVM-платформах возвращает [java.util.concurrent.ConcurrentHashMap],
 * а на платформах без встроенного ConcurrentHashMap (например, iOS) возвращает стандартную мапу.
 *
 * @param K Тип ключей в карте.
 * @param V Тип значений в карте.
 * @return Потокобезопасная мутабельная карта.
 */
expect fun <K, V> createConcurrentMap(): MutableMap<K, V>
