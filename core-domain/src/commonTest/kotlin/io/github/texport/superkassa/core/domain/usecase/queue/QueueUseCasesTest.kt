package io.github.texport.superkassa.core.domain.impl.usecase.queue

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.github.texport.superkassa.core.domain.api.exception.ConflictException
import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmMode
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmState
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandStatus
import io.github.texport.superkassa.core.domain.api.model.queue.QueueDispatchStatus
import io.github.texport.superkassa.core.domain.api.model.queue.QueueTask
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.api.port.integration.inTransaction
import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.ofd.SendFiscalCommandUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.queue.GetQueueStatusUseCase
import io.github.texport.superkassa.core.string.api.CoreStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class QueueUseCasesTest {

    private val authorizeUserUseCase = mockk<AuthorizeUserUseCase>()
    private val storage = mockk<StoragePort>(relaxed = true)
    private val clock = mockk<ClockPort>()
    private val sendFiscalCommand = mockk<SendFiscalCommandUseCase>()

    private val listQueueItems = ListQueueItemsUseCase(storage, authorizeUserUseCase)
    private val processQueueCommand = ProcessQueueCommandUseCase(sendFiscalCommand, storage, clock, mockk(relaxed = true))
    private val retryFailedQueueItems = RetryFailedQueueItemsUseCase(storage, authorizeUserUseCase)

    private val kkmActive = KkmInfo(id = "kkm-1", createdAt = 0, updatedAt = 0, mode = "ACTIVE", state = "ACTIVE")
    private val kkmProg = KkmInfo(id = "kkm-1", createdAt = 0, updatedAt = 0, mode = KkmMode.PROGRAMMING.name, state = KkmState.PROGRAMMING.name)

    init {
    }

    @Test
    fun testListQueueItemsSuccess() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkmActive
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()

        val mockCommand = QueueTask(
            id = "cmd-1",
            cashboxId = "kkm-1",
            lane = "OFFLINE",
            type = "TICKET",
            payloadRef = "payload-1",
            status = "FAILED",
            attempt = 1,
            nextAttemptAt = 1000L,
            lastError = "RU: Ошибка | KK: Қате | EN: Error",
            createdAt = 1000L
        )
        every { storage.listQueueTasksByCashbox("kkm-1", "OFFLINE", 100, 0) } returns listOf(mockCommand)

        val list = listQueueItems.execute("kkm-1", "1234")
        assertEquals(1, list.size)
        assertEquals("cmd-1", list[0].id)
        assertEquals("RU: Ошибка | KK: Қате | EN: Error", list[0].lastError)
        assertEquals("Ошибка", list[0].errorRu)
        assertEquals("Қате", list[0].errorKk)
        assertEquals("Error", list[0].errorEn)
    }

    @Test
    fun testProcessQueueCommandSuccess() {
        val mockCommand = QueueTask(
            id = "cmd-1",
            cashboxId = "kkm-1",
            lane = "OFFLINE",
            type = "TICKET",
            payloadRef = "payload-1",
            status = "PENDING",
            attempt = 1,
            nextAttemptAt = null,
            lastError = null,
            createdAt = 1000L
        )
        val ofdResult = OfdCommandResult(
            status = OfdCommandStatus.OK,
            fiscalSign = "fs-123",
            autonomousSign = "as-123",
            resultCode = 0
        )
        every { sendFiscalCommand.execute("kkm-1", any(), "payload-1") } returns ofdResult
        every { storage.findFiscalDocumentById("payload-1") } returns mockk(relaxed = true)
        every { storage.findFiscalDocumentWithReceiptPayload("payload-1") } returns null
        every { clock.now() } returns 2000L

        val res = processQueueCommand.execute(mockCommand)
        assertEquals(QueueDispatchStatus.SENT, res.status)
        verify {
            storage.updateReceiptStatus("payload-1", "fs-123", any(), "SENT", null, 2000L, any())
        }
    }

    @Test
    fun testDeliveredCloseShiftLeavesTheQueueState() {
        // Закрытие смены и отчёты досылаются той же очередью, что и чеки.
        // Пока здесь стоял список из двух типов, Z-отчёт после досылки
        // навсегда оставался «в очереди», хотя ОФД его принял.
        val command = QueueTask(
            id = "cmd-z", cashboxId = "kkm-1", lane = "OFFLINE", type = "CLOSE_SHIFT",
            payloadRef = "payload-z", status = "PENDING", attempt = 1,
            nextAttemptAt = null, lastError = null, createdAt = 1000L
        )
        every { sendFiscalCommand.execute("kkm-1", any(), "payload-z") } returns
            OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 0)
        every { storage.findFiscalDocumentById("payload-z") } returns mockk(relaxed = true)
        every { clock.now() } returns 2000L

        val res = processQueueCommand.execute(command)

        assertEquals(QueueDispatchStatus.SENT, res.status)
        verify {
            storage.updateReceiptStatus("payload-z", any(), any(), "SENT", null, 2000L, any())
        }
    }

    @Test
    fun testDrainRefusalBlocksTheKkm() {
        // Спецификация CPCR, «Работа в автономном режиме»: при досылке
        // накопленной очереди любой ответ, кроме OK, временной недоступности
        // и неизвестной ошибки, обязан перевести кассу в блокировку.
        // Код 13 раньше просто снимал документ с очереди.
        val command = QueueTask(
            id = "cmd-13", cashboxId = "kkm-1", lane = "OFFLINE", type = "TICKET",
            payloadRef = "payload-13", status = "PENDING", attempt = 1,
            nextAttemptAt = null, lastError = null, createdAt = 1000L
        )
        every { sendFiscalCommand.execute("kkm-1", any(), "payload-13") } returns
            OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 13)
        every { clock.now() } returns 2000L
        every { storage.findKkmForUpdate("kkm-1") } returns kkmActive
        val updated = slot<KkmInfo>()
        every { storage.updateKkm(capture(updated)) } returns true

        processQueueCommand.execute(command)

        assertEquals(KkmState.BLOCKED.name, updated.captured.state)
    }

    @Test
    fun testDrainMarksRejectedDocumentInsteadOfLeavingItPending() {
        // Отказ ОФД при досылке снимал документ только с точки зрения кассы:
        // задача повторялась дальше, а сам документ оставался «ожидает
        // отправки» в журнале — навсегда, потому что ответ был окончательным.
        val command = QueueTask(
            id = "cmd-16", cashboxId = "kkm-1", lane = "OFFLINE", type = "TICKET",
            payloadRef = "payload-16", status = "PENDING", attempt = 1,
            nextAttemptAt = null, lastError = null, createdAt = 1000L
        )
        every { sendFiscalCommand.execute("kkm-1", any(), "payload-16") } returns
            OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 16)
        every { clock.now() } returns 2000L
        every { storage.findKkmForUpdate("kkm-1") } returns kkmActive
        every { storage.updateKkm(any()) } returns true
        every { storage.findFiscalDocumentById("payload-16") } returns mockk(relaxed = true)
        var status: String? = null
        var recordedCode: Int? = null
        every {
            storage.updateReceiptStatus(any(), any(), any(), any(), any(), any(), any())
        } answers {
            status = arg<String>(3)
            recordedCode = arg<Int?>(4)
            true
        }

        val res = processQueueCommand.execute(command)

        // Задача снимается с повтора, а документ помечается отвергнутым:
        // код 16 — отказ по существу, и повторять его нечего.
        assertEquals(QueueDispatchStatus.REJECTED, res.status)
        assertEquals("FAILED", status)
        assertEquals(16, recordedCode)
        assertEquals("BFD returned code 16", res.errorMessage)
        assertEquals(CoreStrings.bfdRefusal(16).ru, res.errorRu)
        assertEquals(16, res.bfdResultCode)
    }

    @Test
    fun testDrainKeepsWorkingOnTemporaryUnavailability() {
        // Обратная сторона того же правила: 254 и 255 блокировать нельзя,
        // иначе обычная недоступность сервиса остановила бы кассу.
        val command = QueueTask(
            id = "cmd-254", cashboxId = "kkm-1", lane = "OFFLINE", type = "TICKET",
            payloadRef = "payload-254", status = "PENDING", attempt = 1,
            nextAttemptAt = null, lastError = null, createdAt = 1000L
        )
        every { sendFiscalCommand.execute("kkm-1", any(), "payload-254") } returns
            OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 254)
        every { clock.now() } returns 2000L
        every { storage.findKkmForUpdate("kkm-1") } returns kkmActive

        val res = processQueueCommand.execute(command)

        assertEquals(QueueDispatchStatus.FAILED, res.status)
        verify(exactly = 0) { storage.updateKkm(any()) }
        // Занятость БФД названа словами таблицы, а не «нет связи», и код остаётся задаче.
        assertEquals(CoreStrings.bfdRefusal(254).en to 254, res.errorEn to res.bfdResultCode)
    }

    @Test
    fun testProcessQueueCommandOfdFailed() {
        val mockCommand = QueueTask(
            id = "cmd-1",
            cashboxId = "kkm-1",
            lane = "OFFLINE",
            type = "TICKET",
            payloadRef = "payload-1",
            status = "PENDING",
            attempt = 1,
            nextAttemptAt = null,
            lastError = null,
            createdAt = 1000L
        )
        val ofdResult = OfdCommandResult(
            status = OfdCommandStatus.FAILED,
            errorMessage = "Server error"
        )
        every { sendFiscalCommand.execute("kkm-1", any(), "payload-1") } returns ofdResult
        every { clock.now() } returns 2000L

        val res = processQueueCommand.execute(mockCommand)
        // Обмена не было: запрос не собрался и до ОФД не дошёл. Задача
        // остаётся в очереди: причина бывает во состоянии кассы, а не
        // в самом документе, и отбраковка теряла фискальный документ
        // молча. Повтор ограничен числом попыток.
        assertEquals(QueueDispatchStatus.FAILED, res.status)
        assertEquals("Server error", res.errorMessage)
        val notSent = CoreStrings.bfdRequestNotSent()
        assertEquals(Triple(notSent.ru, notSent.kk, notSent.en), Triple(res.errorRu, res.errorKk, res.errorEn))
    }

    @Test
    fun testProcessQueueCommandOfdTimeout() {
        val mockCommand = QueueTask(
            id = "cmd-1",
            cashboxId = "kkm-1",
            lane = "OFFLINE",
            type = "MONEY_PLACEMENT",
            payloadRef = "payload-1",
            status = "PENDING",
            attempt = 1,
            nextAttemptAt = null,
            lastError = null,
            createdAt = 1000L
        )
        val ofdResult = OfdCommandResult(
            status = OfdCommandStatus.TIMEOUT,
            resultCode = 408
        )
        every { sendFiscalCommand.execute("kkm-1", any(), "payload-1") } returns ofdResult
        every { clock.now() } returns 2000L

        val res = processQueueCommand.execute(mockCommand)
        assertEquals(QueueDispatchStatus.FAILED, res.status)
        assertEquals("BFD timeout", res.errorMessage)
        assertEquals(32000L, res.retryAt)
        assertEquals("Тайм-аут ожидания ответа от БФД", res.errorRu)
        assertEquals("БФД жауабын күту уақыты бітті", res.errorKk)
        assertEquals("BFD connection timeout", res.errorEn)
    }

    @Test
    fun testProcessQueueCommandVariousTypes() {
        every { clock.now() } returns 1000L
        val types = listOf(
            "REPORT_X",
            "REPORT_Z",
            "CLOSE_SHIFT",
            "INFO",
            "SYSTEM"
        )
        types.forEach { type ->
            val cmd = QueueTask(
                id = "cmd-1",
                cashboxId = "kkm-1",
                lane = "OFFLINE",
                type = type,
                payloadRef = "payload-1",
                status = "PENDING",
                attempt = 1,
                nextAttemptAt = null,
                lastError = null,
                createdAt = 1000L
            )
            val ofdResult = OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 0)
            every { sendFiscalCommand.execute("kkm-1", any(), "payload-1") } returns ofdResult
            val res = processQueueCommand.execute(cmd)
            assertEquals(QueueDispatchStatus.SENT, res.status)
        }
    }

    @Test
    fun testRetryFailedQueueItemsValidationFailed() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkmActive // mode ACTIVE
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()

        assertFailsWith<ValidationException> {
            retryFailedQueueItems.execute("kkm-1", "1234")
        }
    }

    @Test
    fun testRetryFailedQueueItemsConflictShiftOpen() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkmProg // mode PROGRAMMING
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findOpenShift("kkm-1") } returns mockk()

        assertFailsWith<ConflictException> {
            retryFailedQueueItems.execute("kkm-1", "1234")
        }
    }

    @Test
    fun testRetryFailedQueueItemsSuccessEmpty() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkmProg // mode PROGRAMMING
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findOpenShift("kkm-1") } returns null
        every { storage.listQueueTasksByCashbox("kkm-1", "OFFLINE", 100, 0) } returns emptyList()

        val count = retryFailedQueueItems.execute("kkm-1", "1234")
        assertEquals(0, count)
    }

    @Test
    fun testRetryFailedQueueItemsSuccessWithItems() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkmProg // mode PROGRAMMING
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findOpenShift("kkm-1") } returns null

        val failedCmd = QueueTask(
            id = "cmd-1",
            cashboxId = "kkm-1",
            lane = "OFFLINE",
            type = "TICKET",
            payloadRef = "payload-1",
            status = "FAILED",
            attempt = 2,
            nextAttemptAt = 1000L,
            lastError = "error",
            createdAt = 1000L
        )
        val pendingCmd = QueueTask(
            id = "cmd-2",
            cashboxId = "kkm-1",
            lane = "OFFLINE",
            type = "TICKET",
            payloadRef = "payload-2",
            status = "PENDING",
            attempt = 1,
            nextAttemptAt = null,
            lastError = null,
            createdAt = 1000L
        )
        every { storage.listQueueTasksByCashbox("kkm-1", "OFFLINE", 100, 0) } returns listOf(failedCmd, pendingCmd)
        every { storage.updateQueueTaskStatus("cmd-1", "PENDING", 2, null, null) } returns true

        val count = retryFailedQueueItems.execute("kkm-1", "1234")
        assertEquals(1, count)
    }

    /**
     * Отказ ОФД по существу повторять бессмысленно.
     *
     * Спецификация делит ответы кодом: 0 — принято, 254 и 255 — повторить,
     * всё прочее — документ негоден. Пока терминального состояния не было,
     * такая задача повторялась вечно: на стенде счётчик дошёл до 530.
     */
    @Test
    fun `отвергнутый ОФД документ снимается с повтора`() {
        val task = QueueTask(
            id = "cmd-refused",
            cashboxId = "kkm-1",
            lane = "OFFLINE",
            type = "TICKET",
            payloadRef = "payload-1",
            status = "PENDING",
            attempt = 7,
            nextAttemptAt = null,
            lastError = null,
            createdAt = 1000L
        )
        every { sendFiscalCommand.execute("kkm-1", any(), "payload-1") } returns
            OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 16)
        every { clock.now() } returns 2000L

        val result = processQueueCommand.execute(task)

        assertEquals(QueueDispatchStatus.REJECTED, result.status)
        assertEquals(null, result.retryAt)
    }

    @Test
    fun `состояние очереди показывает документы, отправка которых прекращена`() {
        // Отбракованная задача из очереди уходит, а документ остаётся
        // неотправленным: без отдельного числа касса выглядела чистой.
        val rejected = QueueTask(
            id = "cmd-rejected", cashboxId = "kkm-1", lane = "OFFLINE", type = "REPORT",
            payloadRef = "payload-x", status = "REJECTED", attempt = 1,
            nextAttemptAt = null, lastError = "no request built", createdAt = 1000L
        )
        val pending = QueueTask(
            id = "cmd-pending", cashboxId = "kkm-1", lane = "OFFLINE", type = "TICKET",
            payloadRef = "payload-1", status = "PENDING", attempt = 0,
            nextAttemptAt = null, lastError = null, createdAt = 1100L
        )
        every { storage.listQueueTasksByCashbox("kkm-1", "OFFLINE", 500, 0) } returns listOf(rejected, pending)

        val status = GetQueueStatusUseCase(storage).execute("kkm-1")

        assertEquals(1, status.pendingCount)
        assertEquals(1, status.rejectedCount)
    }

    @Test
    fun `несобранный запрос остаётся в очереди, а не уходит в отбраковку`() {
        // X-отчёт, снятый без связи, терялся молча: запрос собирался
        // из открытой смены, после её закрытия не собирался вовсе,
        // и задача уходила в отбраковку навсегда.
        val task = QueueTask(
            id = "cmd-unbuilt",
            cashboxId = "kkm-1",
            lane = "OFFLINE",
            type = "REPORT",
            payloadRef = "payload-x",
            status = "PENDING",
            attempt = 1,
            nextAttemptAt = null,
            lastError = null,
            createdAt = 1000L
        )
        every { sendFiscalCommand.execute("kkm-1", any(), "payload-x") } returns
            OfdCommandResult(status = OfdCommandStatus.FAILED, errorMessage = "no request built")
        every { clock.now() } returns 2000L

        val result = processQueueCommand.execute(task)

        assertEquals(QueueDispatchStatus.FAILED, result.status)
    }

    @Test
    fun `временная недоступность ОФД остаётся поводом для повтора`() {
        val task = QueueTask(
            id = "cmd-busy",
            cashboxId = "kkm-1",
            lane = "OFFLINE",
            type = "TICKET",
            payloadRef = "payload-1",
            status = "PENDING",
            attempt = 1,
            nextAttemptAt = null,
            lastError = null,
            createdAt = 1000L
        )
        every { sendFiscalCommand.execute("kkm-1", any(), "payload-1") } returns
            OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 254)
        every { clock.now() } returns 2000L

        val result = processQueueCommand.execute(task)

        assertEquals(QueueDispatchStatus.FAILED, result.status)
        assertEquals(32000L, result.retryAt)
    }
}
