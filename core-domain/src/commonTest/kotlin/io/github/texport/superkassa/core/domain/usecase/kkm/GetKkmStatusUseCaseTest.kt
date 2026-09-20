package io.github.texport.superkassa.core.domain.usecase.kkm

import io.github.texport.superkassa.core.domain.api.model.kkm.EffectiveKkmStatus
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmMode
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmState
import io.github.texport.superkassa.core.domain.api.model.kkm.TokenState
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.api.port.internal.OfflineQueuePort
import io.github.texport.superkassa.core.domain.api.port.internal.TokenCodecPort
import io.github.texport.superkassa.core.domain.impl.usecase.kkm.GetKkmStatusUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.ofd.GenerateRequestNumberUseCase
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class GetKkmStatusUseCaseTest {
    private val storage = mockk<StoragePort>()
    private val tokenCodec = mockk<TokenCodecPort>()
    private val queuePort = mockk<OfflineQueuePort>()
    private val generateRequestNumberUseCase = mockk<GenerateRequestNumberUseCase>()
    private val useCase = GetKkmStatusUseCase(storage, tokenCodec, queuePort, generateRequestNumberUseCase)

    @Test
    fun testStatusOnlineWhenTokenValidAndQueueEmpty() {
        val kkm = KkmInfo(
            id = "kkm-1",
            createdAt = 100L,
            updatedAt = 100L,
            mode = KkmMode.REGISTRATION.name,
            state = KkmState.ACTIVE.name,
            registrationNumber = "RN1",
            factoryNumber = "FN1",
            tokenEncryptedBase64 = "token"
        )
        every { tokenCodec.decodeToken("token") } returns 12345L
        every { generateRequestNumberUseCase.execute("kkm-1", false) } returns 10
        every { storage.findOpenShift("kkm-1") } returns null
        every { storage.listQueueTasksByCashbox("kkm-1", "OFFLINE", 100) } returns emptyList()
        every { storage.listFiscalDocumentsByPeriod("kkm-1", 0L, 4102444800000L, 10, 0) } returns emptyList()

        val status = useCase.execute(kkm)
        assertEquals(TokenState.VALID, status.tokenState)
        assertEquals(10, status.currentReqNum)
        assertEquals(EffectiveKkmStatus.ONLINE, status.effectiveStatus)
    }

    @Test
    fun testStatusNoTokenWhenTokenNull() {
        val kkm = KkmInfo(
            id = "kkm-1",
            createdAt = 100L,
            updatedAt = 100L,
            mode = KkmMode.REGISTRATION.name,
            state = KkmState.ACTIVE.name,
            registrationNumber = "RN1",
            factoryNumber = "FN1",
            tokenEncryptedBase64 = null
        )
        every { tokenCodec.decodeToken(null) } returns null
        every { generateRequestNumberUseCase.execute("kkm-1", false) } returns 1
        every { storage.findOpenShift("kkm-1") } returns null
        every { storage.listQueueTasksByCashbox("kkm-1", "OFFLINE", 100) } returns emptyList()
        every { storage.listFiscalDocumentsByPeriod("kkm-1", 0L, 4102444800000L, 10, 0) } returns emptyList()

        val status = useCase.execute(kkm)
        assertEquals(TokenState.MISSING, status.tokenState)
        assertEquals(EffectiveKkmStatus.NO_TOKEN, status.effectiveStatus)
    }
}
