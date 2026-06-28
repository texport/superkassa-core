package kz.mybrain.superkassa.core.domain.port

/**
 * Порт для работы с системным и фискальным временем (источник времени).
 * Используется для изоляции логики предметной области от системных вызовов даты/времени.
 *
 * Обеспечивает детерминированность бизнес-логики и облегчает её тестирование, позволяя
 * подменять реальные часы тестовыми заглушками (Mock/Stub).
 */
interface ClockPort {

    /**
     * Возвращает текущее системное время в миллисекундах (Unix Epoch millis).
     *
     * @return текущее время в миллисекундах.
     */
    fun now(): Long

    /**
     * Возвращает текущий год.
     *
     * @return текущий год как целое число (например, 2026).
     */
    fun currentYear(): Int

    /**
     * Преобразует разложенные компоненты даты и времени в миллисекунды (Unix Epoch millis).
     *
     * @param year год.
     * @param month месяц (1-12).
     * @param day день месяца (1-31).
     * @param hour час (0-23).
     * @param minute минута (0-59).
     * @param second секунда (0-59).
     * @return время в миллисекундах с начала эпохи.
     */
    fun parseDateTimeToMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Long
}
