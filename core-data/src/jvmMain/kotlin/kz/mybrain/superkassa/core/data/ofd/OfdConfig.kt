package kz.mybrain.superkassa.core.data.ofd

/**
 * Конфигурация ОФД для сериализации.
 *
 * @property protocolVersion Версия протокола ОФД, используемая для сериализации и десериализации сообщений.
 */
data class OfdConfig(
    val protocolVersion: String
)
