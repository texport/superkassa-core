package kz.mybrain.superkassa.core.domain.model.ofd

/**
 * Тип отправляемой в ОФД команды.
 *
 * @property value Строковое представление типа команды в протоколе.
 */
@Suppress("unused") // Элементы перечисления используются внешними сервисами и механизмами сериализации API
enum class OfdCommandType(val value: String) {
    /** Оформление чека продажи/возврата/покупки/возврата покупки. */
    TICKET("COMMAND_TICKET"),

    /** Системная команда (например, инициализация/регистрация). */
    SYSTEM("COMMAND_SYSTEM"),

    /** Запрос справочной информации с сервера ОФД. */
    INFO("COMMAND_INFO"),

    /** Операция внесения или изъятия наличных денег. */
    MONEY_PLACEMENT("COMMAND_MONEY_PLACEMENT"),

    /** Снятие сменного отчета без гашения (X-отчет). */
    REPORT("COMMAND_REPORT"),

    /** Закрытие смены с гашением (Z-отчет). */
    CLOSE_SHIFT("COMMAND_CLOSE_SHIFT");

    companion object {
        /**
         * Получает тип команды по его строковому значению.
         *
         * @throws IllegalArgumentException если тип команды неизвестен.
         */
        fun fromString(value: String): OfdCommandType {
            return entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown OFD command type: $value")
        }
    }
}
