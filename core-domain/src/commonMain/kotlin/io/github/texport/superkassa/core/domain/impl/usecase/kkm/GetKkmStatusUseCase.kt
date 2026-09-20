package io.github.texport.superkassa.core.domain.impl.usecase.kkm

import io.github.texport.superkassa.core.domain.api.exception.NotFoundException
import io.github.texport.superkassa.core.domain.api.model.kkm.EffectiveKkmStatus
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmMode
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmOperationalStatus
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmState
import io.github.texport.superkassa.core.domain.api.model.kkm.TokenState
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.api.port.internal.OfflineQueuePort
import io.github.texport.superkassa.core.domain.api.port.internal.TokenCodecPort
import io.github.texport.superkassa.core.domain.impl.usecase.ofd.GenerateRequestNumberUseCase
import io.github.texport.superkassa.core.string.api.CoreStrings

/**
 * Единый сценарий (Use Case) получения и агрегации полного операционного состояния ККМ.
 *
 * Является единственным источником правды для определения всех статусов ККМ,
 * включая валидность токена, текущий порядковый номер запроса (req_num),
 * состояние смены, очередей и эффективный статус кассы.
 */
class GetKkmStatusUseCase(
    private val storage: StoragePort,
    private val tokenCodec: TokenCodecPort,
    private val queuePort: OfflineQueuePort,
    private val generateRequestNumberUseCase: GenerateRequestNumberUseCase
) {
    /**
     * Возвращает агрегированный статус указанной ККМ.
     *
     * @param kkmId Уникальный идентификатор ККМ.
     * @return [KkmOperationalStatus] Полное операционное состояние ККМ.
     * @throws NotFoundException если ККМ не найдена в системе.
     */
    fun execute(kkmId: String): KkmOperationalStatus {
        val kkm = storage.findKkm(kkmId) ?: throw NotFoundException(
            trilingualMessage = CoreStrings.kkmNotFound(),
            code = "KKM_NOT_FOUND"
        )
        return execute(kkm)
    }

    /**
     * Возвращает агрегированный статус ККМ на основе объекта [KkmInfo].
     *
     * @param kkm Объект информации о ККМ.
     * @return [KkmOperationalStatus] Полное операционное состояние ККМ.
     */
    fun execute(kkm: KkmInfo): KkmOperationalStatus {
        val decodedToken = tokenCodec.decodeToken(kkm.tokenEncryptedBase64)
        val tokenState = when {
            kkm.tokenEncryptedBase64.isNullOrBlank() -> TokenState.MISSING
            decodedToken == null -> TokenState.INVALID
            else -> TokenState.VALID
        }

        val currentReqNum = generateRequestNumberUseCase.execute(kkm.id, persist = false)

        val openShift = storage.findOpenShift(kkm.id)
        val isShiftOpen = openShift != null
        val shiftOpenedAt = openShift?.openedAt

        val queueStatus = try {
            storage.listQueueTasksByCashbox(kkm.id, "OFFLINE", 100)
        } catch (e: Exception) {
            emptyList()
        }
        val offlineQueueCount = queueStatus.count { it.status != "SENT" }

        val hasPendingDocs = storage.listFiscalDocumentsByPeriod(
            kkmId = kkm.id,
            fromInclusive = 0L,
            toExclusive = 4102444800000L,
            limit = 10,
            offset = 0
        ).any { it.ofdStatus != "SENT" && it.ofdStatus != "DELIVERED" }

        val lastSyncError = queueStatus.firstOrNull { it.status == "FAILED" }?.lastError

        val isProgramming = kkm.mode == KkmMode.PROGRAMMING.name || kkm.state == KkmState.PROGRAMMING.name
        val isBlocked = kkm.state == KkmState.BLOCKED.name

        var updatedKkm = kkm
        if (offlineQueueCount == 0 && kkm.autonomousSince != null && !isBlocked) {
            updatedKkm = kkm.copy(updatedAt = storage.findOpenShift(kkm.id)?.openedAt ?: 0L, autonomousSince = null)
            storage.updateKkm(updatedKkm)
        }

        val effectiveStatus = when {
            isBlocked -> EffectiveKkmStatus.BLOCKED
            isProgramming -> EffectiveKkmStatus.PROGRAMMING
            tokenState == TokenState.MISSING || tokenState == TokenState.INVALID -> EffectiveKkmStatus.NO_TOKEN
            offlineQueueCount > 0 -> EffectiveKkmStatus.OFFLINE_QUEUE
            else -> EffectiveKkmStatus.ONLINE
        }

        return KkmOperationalStatus(
            kkm = kkm,
            tokenState = tokenState,
            currentReqNum = currentReqNum,
            isShiftOpen = isShiftOpen,
            shiftOpenedAt = shiftOpenedAt,
            offlineQueueCount = offlineQueueCount,
            hasPendingDocuments = hasPendingDocs,
            lastSyncError = lastSyncError,
            effectiveStatus = effectiveStatus
        )
    }
}
