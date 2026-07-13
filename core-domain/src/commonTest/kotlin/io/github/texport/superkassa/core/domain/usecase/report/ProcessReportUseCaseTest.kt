package io.github.texport.superkassa.core.domain.impl.usecase.report

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryStatus
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmState
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandStatus
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.api.port.internal.IdGeneratorPort
import io.github.texport.superkassa.core.domain.api.port.internal.OfflineQueuePort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.kkm.RequireOperationalUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.ofd.SendFiscalCommandUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ProcessReportUseCaseTest {

    private val storage = mockk<StoragePort>(relaxed = true)
    private val queue = mockk<OfflineQueuePort>(relaxed = true)
    private val sendFiscalCommandUseCase = mockk<SendFiscalCommandUseCase>()
    private val idGenerator = mockk<IdGeneratorPort>()
    private val authorizeUser = mockk<AuthorizeUserUseCase>()
    private val requireOperational = mockk<RequireOperationalUseCase>(relaxed = true)

    private val useCase = ProcessReportUseCase(
        storage = storage,
        queue = queue,
        sendFiscalCommandUseCase = sendFiscalCommandUseCase,
        idGenerator = idGenerator,
        authorizeUser = authorizeUser,
        requireOperational = requireOperational
    )

    private val kkm = KkmInfo(
        id = "kkm-1",
        createdAt = 0,
        updatedAt = 0,
        mode = "ACTIVE",
        state = KkmState.ACTIVE.name
    )

    init {
        every { storage.inTransaction<Any?>(any()) } answers {
            val block = firstArg<() -> Any?>()
            block()
        }
        every { authorizeUser.requireKkm("kkm-1") } returns kkm
        every { authorizeUser.execute("kkm-1", "1234", setOf(UserRole.ADMIN, UserRole.CASHIER)) } returns mockk()
        every { idGenerator.nextId() } returns "doc-123"
    }

    @Test
    fun testExecuteOnlineSuccess() {
        every { queue.canSendDirectly("kkm-1") } returns true
        every { sendFiscalCommandUseCase.execute("kkm-1", OfdCommandType.REPORT, "doc-123") } returns OfdCommandResult(
            status = OfdCommandStatus.OK
        )

        val result = useCase.execute("kkm-1", "1234")
        assertEquals("doc-123", result.documentId)
        assertEquals(DeliveryStatus.ONLINE_OK, result.deliveryStatus)
    }

    @Test
    fun testExecuteOnlineTimeout() {
        every { queue.canSendDirectly("kkm-1") } returns true
        every { sendFiscalCommandUseCase.execute("kkm-1", OfdCommandType.REPORT, "doc-123") } returns OfdCommandResult(
            status = OfdCommandStatus.TIMEOUT,
            errorMessage = "Timeout error"
        )

        val result = useCase.execute("kkm-1", "1234")
        assertEquals("doc-123", result.documentId)
        assertEquals(DeliveryStatus.OFFLINE_QUEUED, result.deliveryStatus)
        assertEquals("Timeout error", result.deliveryError)
    }

    @Test
    fun testExecuteOnlineFailed() {
        every { queue.canSendDirectly("kkm-1") } returns true
        every { sendFiscalCommandUseCase.execute("kkm-1", OfdCommandType.REPORT, "doc-123") } returns OfdCommandResult(
            status = OfdCommandStatus.FAILED,
            errorMessage = "Server error"
        )

        val result = useCase.execute("kkm-1", "1234")
        assertEquals("doc-123", result.documentId)
        assertEquals(DeliveryStatus.ONLINE_ERROR, result.deliveryStatus)
        assertEquals("Server error", result.deliveryError)
    }

    @Test
    fun testExecuteOfflineQueued() {
        every { queue.canSendDirectly("kkm-1") } returns false

        val result = useCase.execute("kkm-1", "1234")
        assertEquals("doc-123", result.documentId)
        assertEquals(DeliveryStatus.OFFLINE_QUEUED, result.deliveryStatus)
        verify { queue.enqueueOffline(any()) }
    }
}
