package io.github.texport.superkassa.core.domain.api.model.kkm

/**
 * Статус токена ОФД ККМ.
 */
enum class TokenState {
    VALID,
    MISSING,
    INVALID
}

/**
 * Единый агрегированный эффективный статус ККМ для презентационного слоя и UI.
 */
enum class EffectiveKkmStatus {
    ONLINE,
    OFFLINE_QUEUE,
    NO_TOKEN,
    BLOCKED,
    PROGRAMMING
}

/**
 * Полный агрегат операционного состояния ККМ.
 *
 * Находится в доменном слое и является единым источником правды для состояния кассы.
 */
data class KkmOperationalStatus(
    val kkm: KkmInfo,
    val tokenState: TokenState,
    val currentReqNum: Int,
    val isShiftOpen: Boolean,
    val shiftOpenedAt: Long? = null,
    val offlineQueueCount: Int = 0,
    val hasPendingDocuments: Boolean = false,
    val lastSyncError: String? = null,
    val effectiveStatus: EffectiveKkmStatus
)
