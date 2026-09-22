package io.github.texport.superkassa.core.domain.api.model.settings

/**
 * Глобальные настройки ядра системы Superkassa.
 */
data class CoreSettings(
    val mode: CoreMode,
    val storage: StorageSettings,
    val allowChanges: Boolean = false,
    val nodeId: String = "node-1",
    val ofdProtocolVersion: String = "203",
    val ofdProviderId: String = "KAZAKHTELECOM",
    val deliveryChannels: List<String> = listOf("PRINT"),
    /**
     * Сколько касса ждёт ответа БФД, прежде чем счесть связь пропавшей.
     *
     * Ожидание держит кассира у экрана: пока оно идёт, чек не пробит.
     * Семь секунд — предел, после которого касса перестаёт ждать
     * и оформляет чек автономно, а отправку откладывает в очередь.
     * Установка соединения укладывается в это же время: у обмена
     * один бюджет, и отдельного ожидания соединения нет.
     */
    val ofdTimeoutSeconds: Long = 7L,
    val ofdReconnectIntervalSeconds: Long = 60L,
    val kkmFactoryNumberPrefix: String = "KZT",
    val delivery: DeliverySettings? = null
)
