package kz.mybrain.superkassa.core.domain.model.settings

import kotlinx.serialization.Serializable

/**
 * Глобальные настройки ядра системы Superkassa.
 */
@Serializable
data class CoreSettings(
    val mode: CoreMode,
    val storage: StorageSettings,
    val allowChanges: Boolean = false,
    val nodeId: String = "node-1",
    val ofdProtocolVersion: String = "203",
    val deliveryChannels: List<String> = listOf("PRINT"),
    val ofdTimeoutSeconds: Long = 30L,
    val ofdReconnectIntervalSeconds: Long = 60L,
    val delivery: DeliverySettings? = null,
    val defaultAdminPin: String = "0000",
    val defaultAdminName: String = "Администратор",
    val defaultCashierPin: String = "1111",
    val defaultCashierName: String = "Кассир"
)
