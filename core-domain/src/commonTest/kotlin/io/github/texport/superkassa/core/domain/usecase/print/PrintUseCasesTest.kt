package io.github.texport.superkassa.core.domain.impl.usecase.print

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.github.texport.superkassa.core.domain.api.exception.ConflictException
import io.github.texport.superkassa.core.domain.api.exception.NotFoundException
import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.impl.helper.KkmCommonHelper
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.model.common.CounterScopes
import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmState
import io.github.texport.superkassa.core.domain.api.model.queue.QueueTask
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptLayoutType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.api.model.report.PrintDocumentType
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftStatus
import io.github.texport.superkassa.core.domain.api.model.zxreport.ZxReportInput
import io.github.texport.superkassa.core.domain.api.port.integration.DocumentConvertPort
import io.github.texport.superkassa.core.domain.api.port.internal.ReceiptRenderPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PrintUseCasesTest {

    private val storage = mockk<StoragePort>()
    private val receiptRenderPort = mockk<ReceiptRenderPort>()
    private val authorizeUserUseCase = mockk<AuthorizeUserUseCase>()
    private val kkmCommonHelper = mockk<KkmCommonHelper>(relaxed = true)
    private val documentConvertPort = mockk<DocumentConvertPort>()

    private val getReceiptHtml = GetReceiptHtmlUseCase(storage, receiptRenderPort, authorizeUserUseCase)
    private val getPrintHtml = GetPrintHtmlUseCase(storage, receiptRenderPort, authorizeUserUseCase, kkmCommonHelper, getReceiptHtml)
    private val getPrintPdf = GetPrintPdfUseCase(getPrintHtml, documentConvertPort)

    private val kkm = KkmInfo(id = "kkm-1", createdAt = 0, updatedAt = 0, mode = "ACTIVE", state = KkmState.ACTIVE.name)

    private val snapshot = FiscalDocumentSnapshot(
        id = "doc-1",
        cashboxId = "kkm-1",
        shiftId = "shift-1",
        docType = "CHECK",
        docNo = 1L,
        shiftNo = 1L,
        createdAt = 1000L,
        totalAmount = 100L,
        currency = "KZT",
        fiscalSign = "fs",
        autonomousSign = "as",
        isAutonomous = false,
        ofdStatus = "DELIVERED",
        deliveredAt = 1000L
    )

    // --- GetReceiptHtmlUseCase Tests ---

    @Test
    fun testGetReceiptHtmlSuccess() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", setOf(UserRole.CASHIER, UserRole.ADMIN)) } returns mockk()

        val receiptReq = mockk<ReceiptRequest>()
        every { storage.findFiscalDocumentWithReceiptPayload("doc-1") } returns (snapshot to receiptReq)
        every { receiptRenderPort.renderHtml(receiptReq, snapshot, kkm, null) } returns "<html>receipt</html>"

        val res = getReceiptHtml.execute("kkm-1", "doc-1", "1234")
        assertEquals("<html>receipt</html>", res)
    }

    @Test
    fun testGetReceiptHtmlDocumentNotFound() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", setOf(UserRole.CASHIER, UserRole.ADMIN)) } returns mockk()
        every { storage.findFiscalDocumentWithReceiptPayload("doc-1") } returns null

        assertFailsWith<NotFoundException> {
            getReceiptHtml.execute("kkm-1", "doc-1", "1234")
        }
    }

    @Test
    fun testGetReceiptHtmlCashboxMismatch() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", setOf(UserRole.CASHIER, UserRole.ADMIN)) } returns mockk()

        val mismatchedSnapshot = snapshot.copy(cashboxId = "kkm-other")
        val receiptReq = mockk<ReceiptRequest>()
        every { storage.findFiscalDocumentWithReceiptPayload("doc-1") } returns (mismatchedSnapshot to receiptReq)

        assertFailsWith<NotFoundException> {
            getReceiptHtml.execute("kkm-1", "doc-1", "1234")
        }
    }

    // --- GetPrintPdfUseCase Tests ---

    @Test
    fun testGetPrintPdfSuccess() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", setOf(UserRole.CASHIER, UserRole.ADMIN)) } returns mockk()

        val shift = ShiftInfo(id = "shift-1", kkmId = "kkm-1", shiftNo = 1L, status = ShiftStatus.OPEN, openedAt = 100L)
        every { storage.findOpenShift("kkm-1") } returns shift
        every { storage.loadCounters("kkm-1", CounterScopes.SHIFT, "shift-1") } returns emptyMap()
        every { receiptRenderPort.renderXReportHtml(shift, emptyMap(), kkm, null, null, null) } returns "<html>xreport</html>"
        every { documentConvertPort.htmlToPdf("<html>xreport</html>") } returns byteArrayOf(1, 2, 3)

        val pdf = getPrintPdf.execute("kkm-1", PrintDocumentType.X_REPORT, null, null, "1234")
        assertEquals(3, pdf.size)
        verify { documentConvertPort.htmlToPdf("<html>xreport</html>") }
    }

    // --- GetPrintHtmlUseCase Tests ---

    @Test
    fun testGetPrintHtmlDocumentIdRequired() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", setOf(UserRole.CASHIER, UserRole.ADMIN)) } returns mockk()

        assertFailsWith<ValidationException> {
            getPrintHtml.execute("kkm-1", PrintDocumentType.DOCUMENT, null, null, "1234")
        }
    }

    @Test
    fun testGetPrintHtmlShiftIdRequired() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", setOf(UserRole.CASHIER, UserRole.ADMIN)) } returns mockk()

        assertFailsWith<ValidationException> {
            getPrintHtml.execute("kkm-1", PrintDocumentType.CLOSE_SHIFT, null, null, "1234")
        }
    }

    @Test
    fun testGetPrintHtmlShiftNotFound() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", setOf(UserRole.CASHIER, UserRole.ADMIN)) } returns mockk()
        every { storage.findShiftById("shift-1") } returns null

        assertFailsWith<NotFoundException> {
            getPrintHtml.execute("kkm-1", PrintDocumentType.CLOSE_SHIFT, null, "shift-1", "1234")
        }
    }

    @Test
    fun testGetPrintHtmlShiftKkmMismatch() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", setOf(UserRole.CASHIER, UserRole.ADMIN)) } returns mockk()

        val shift = ShiftInfo(id = "shift-1", kkmId = "kkm-other", shiftNo = 1L, status = ShiftStatus.CLOSED, openedAt = 100L)
        every { storage.findShiftById("shift-1") } returns shift

        assertFailsWith<NotFoundException> {
            getPrintHtml.execute("kkm-1", PrintDocumentType.CLOSE_SHIFT, null, "shift-1", "1234")
        }
    }

    @Test
    fun testGetPrintHtmlKkmBlocked() {
        val blockedKkm = kkm.copy(state = KkmState.BLOCKED.name)
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns blockedKkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", setOf(UserRole.CASHIER, UserRole.ADMIN)) } returns mockk()

        assertFailsWith<ValidationException> {
            getPrintHtml.execute("kkm-1", PrintDocumentType.X_REPORT, null, null, "1234")
        }
    }

    @Test
    fun testGetPrintHtmlKkmInProgramming() {
        val progKkm = kkm.copy(state = KkmState.PROGRAMMING.name)
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns progKkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", setOf(UserRole.CASHIER, UserRole.ADMIN)) } returns mockk()

        assertFailsWith<ValidationException> {
            getPrintHtml.execute("kkm-1", PrintDocumentType.X_REPORT, null, null, "1234")
        }
    }

    @Test
    fun testGetPrintHtmlShiftNotOpen() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", setOf(UserRole.CASHIER, UserRole.ADMIN)) } returns mockk()
        every { storage.findOpenShift("kkm-1") } returns null

        assertFailsWith<ConflictException> {
            getPrintHtml.execute("kkm-1", PrintDocumentType.X_REPORT, null, null, "1234")
        }
    }

    @Test
    fun testGetPrintHtmlDocumentCheck() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", setOf(UserRole.CASHIER, UserRole.ADMIN)) } returns mockk()
        every { storage.findFiscalDocumentById("doc-1") } returns snapshot

        // GetReceiptHtmlUseCase flow is tested separately; here we verify the delegation.
        val receiptReq = mockk<ReceiptRequest>()
        every { storage.findFiscalDocumentWithReceiptPayload("doc-1") } returns (snapshot to receiptReq)
        every { receiptRenderPort.renderHtml(receiptReq, snapshot, kkm, null) } returns "<html>receipt</html>"

        val res = getPrintHtml.execute("kkm-1", PrintDocumentType.DOCUMENT, "doc-1", null, "1234")
        assertEquals("<html>receipt</html>", res)
    }

    @Test
    fun testGetPrintHtmlDocumentCashIn() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", setOf(UserRole.CASHIER, UserRole.ADMIN)) } returns mockk()

        val cashInDoc = snapshot.copy(docType = "CASH_IN")
        every { storage.findFiscalDocumentById("doc-1") } returns cashInDoc
        every { receiptRenderPort.renderCashOperationHtml(cashInDoc, kkm, null) } returns "<html>cash_in</html>"

        val res = getPrintHtml.execute("kkm-1", PrintDocumentType.DOCUMENT, "doc-1", null, "1234")
        assertEquals("<html>cash_in</html>", res)
    }

    @Test
    fun testGetPrintHtmlDocumentCashOut() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", setOf(UserRole.CASHIER, UserRole.ADMIN)) } returns mockk()

        val cashOutDoc = snapshot.copy(docType = "CASH_OUT")
        every { storage.findFiscalDocumentById("doc-1") } returns cashOutDoc
        every { receiptRenderPort.renderCashOperationHtml(cashOutDoc, kkm, null) } returns "<html>cash_out</html>"

        val res = getPrintHtml.execute("kkm-1", PrintDocumentType.DOCUMENT, "doc-1", null, "1234")
        assertEquals("<html>cash_out</html>", res)
    }

    @Test
    fun testGetPrintHtmlDocumentUnsupportedTypeRendersFallback() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", setOf(UserRole.CASHIER, UserRole.ADMIN)) } returns mockk()

        val unsupportedDoc = snapshot.copy(docType = "UNSUPPORTED")
        every { storage.findFiscalDocumentById("doc-1") } returns unsupportedDoc
        every { storage.findFiscalDocumentWithReceiptPayload("doc-1") } returns null
        every { receiptRenderPort.renderCashOperationHtml(unsupportedDoc, kkm, null) } returns "<html>unsupported</html>"

        val res = getPrintHtml.execute("kkm-1", PrintDocumentType.DOCUMENT, "doc-1", null, "1234")
        assertEquals("<html>unsupported</html>", res)
    }

    @Test
    fun testGetPrintHtmlDocumentNotFound() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", setOf(UserRole.CASHIER, UserRole.ADMIN)) } returns mockk()
        every { storage.findFiscalDocumentById("doc-1") } returns null

        assertFailsWith<NotFoundException> {
            getPrintHtml.execute("kkm-1", PrintDocumentType.DOCUMENT, "doc-1", null, "1234")
        }
    }

    @Test
    fun testGetPrintHtmlDocumentKkmMismatch() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", setOf(UserRole.CASHIER, UserRole.ADMIN)) } returns mockk()

        val mismatchedDoc = snapshot.copy(cashboxId = "kkm-other")
        every { storage.findFiscalDocumentById("doc-1") } returns mismatchedDoc

        assertFailsWith<NotFoundException> {
            getPrintHtml.execute("kkm-1", PrintDocumentType.DOCUMENT, "doc-1", null, "1234")
        }
    }

    @Test
    fun testGetPrintHtmlOpenShiftSuccess() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", setOf(UserRole.CASHIER, UserRole.ADMIN)) } returns mockk()

        val shift = ShiftInfo(
            id = "shift-1",
            kkmId = "kkm-1",
            shiftNo = 1L,
            status = ShiftStatus.OPEN,
            openedAt = 100L,
            openDocumentId = "open-doc-1"
        )
        every { storage.findOpenShift("kkm-1") } returns shift
        every { storage.listQueueTasksByCashbox("kkm-1", "OFFLINE", 100) } returns emptyList()
        every { storage.findFiscalDocumentById("open-doc-1") } returns snapshot.copy(docNo = 42L)
        every { receiptRenderPort.renderOpenShiftHtml(shift, kkm, "DELIVERED", "42", null) } returns "<html>open_shift</html>"

        val res = getPrintHtml.execute("kkm-1", PrintDocumentType.OPEN_SHIFT, null, null, "1234")
        assertEquals("<html>open_shift</html>", res)
    }

    @Test
    fun testGetPrintHtmlOpenShiftOffline() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", setOf(UserRole.CASHIER, UserRole.ADMIN)) } returns mockk()

        val shift = ShiftInfo(
            id = "shift-1",
            kkmId = "kkm-1",
            shiftNo = 1L,
            status = ShiftStatus.OPEN,
            openedAt = 100L,
            openDocumentId = "open-doc-1"
        )
        every { storage.findOpenShift("kkm-1") } returns shift

        val task = QueueTask(
            id = "task-1",
            cashboxId = "kkm-1",
            lane = "OFFLINE",
            type = "SEND_OFD",
            payloadRef = "open-doc-1",
            createdAt = 1000L,
            status = "PENDING",
            attempt = 1,
            nextAttemptAt = null,
            lastError = null
        )
        every { storage.listQueueTasksByCashbox("kkm-1", "OFFLINE", 100) } returns listOf(task)
        every { storage.findFiscalDocumentById("open-doc-1") } returns snapshot.copy(docNo = 42L)
        every { receiptRenderPort.renderOpenShiftHtml(shift, kkm, "OFFLINE", "42", null) } returns "<html>open_shift_offline</html>"

        val res = getPrintHtml.execute("kkm-1", PrintDocumentType.OPEN_SHIFT, null, null, "1234")
        assertEquals("<html>open_shift_offline</html>", res)
    }

    @Test
    fun testGetPrintHtmlOpenShiftDeliveredWithQueueTaskSent() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", setOf(UserRole.CASHIER, UserRole.ADMIN)) } returns mockk()

        val shift = ShiftInfo(
            id = "shift-1",
            kkmId = "kkm-1",
            shiftNo = 1L,
            status = ShiftStatus.OPEN,
            openedAt = 100L,
            openDocumentId = "open-doc-1"
        )
        every { storage.findOpenShift("kkm-1") } returns shift

        val task = QueueTask(
            id = "task-1",
            cashboxId = "kkm-1",
            lane = "OFFLINE",
            type = "SEND_OFD",
            payloadRef = "open-doc-1",
            createdAt = 1000L,
            status = "SENT",
            attempt = 1,
            nextAttemptAt = null,
            lastError = null
        )
        every { storage.listQueueTasksByCashbox("kkm-1", "OFFLINE", 100) } returns listOf(task)
        every { storage.findFiscalDocumentById("open-doc-1") } returns snapshot.copy(docNo = 42L)
        every { receiptRenderPort.renderOpenShiftHtml(shift, kkm, "DELIVERED", "42", null) } returns "<html>open_shift_delivered</html>"

        val res = getPrintHtml.execute("kkm-1", PrintDocumentType.OPEN_SHIFT, null, null, "1234")
        assertEquals("<html>open_shift_delivered</html>", res)
    }

    @Test
    fun testGetPrintHtmlOpenShiftOpenDocumentIdNull() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", setOf(UserRole.CASHIER, UserRole.ADMIN)) } returns mockk()

        val shift = ShiftInfo(
            id = "shift-1",
            kkmId = "kkm-1",
            shiftNo = 1L,
            status = ShiftStatus.OPEN,
            openedAt = 100L,
            openDocumentId = null
        )
        every { storage.findOpenShift("kkm-1") } returns shift
        every { receiptRenderPort.renderOpenShiftHtml(shift, kkm, "DELIVERED", null, null) } returns "<html>open_shift_null</html>"

        val res = getPrintHtml.execute("kkm-1", PrintDocumentType.OPEN_SHIFT, null, null, "1234")
        assertEquals("<html>open_shift_null</html>", res)
    }

    @Test
    fun testGetPrintHtmlCloseShiftSuccess() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", setOf(UserRole.CASHIER, UserRole.ADMIN)) } returns mockk()

        val shift = ShiftInfo(
            id = "shift-1",
            kkmId = "kkm-1",
            shiftNo = 1L,
            status = ShiftStatus.CLOSED,
            openedAt = 100L,
            closeDocumentId = "close-doc-1"
        )
        every { storage.findShiftById("shift-1") } returns shift
        every { storage.loadCounters("kkm-1", CounterScopes.SHIFT, "shift-1") } returns emptyMap()
        every { storage.listQueueTasksByCashbox("kkm-1", "OFFLINE", 100) } returns emptyList()
        every { storage.findFiscalDocumentById("close-doc-1") } returns snapshot.copy(docNo = 43L)
        every { receiptRenderPort.renderCloseShiftHtml(shift, emptyMap(), kkm, "DELIVERED", "43", ReceiptLayoutType.TAPE_80MM) } returns "<html>close_shift</html>"

        val res = getPrintHtml.execute("kkm-1", PrintDocumentType.CLOSE_SHIFT, null, "shift-1", "1234", ReceiptLayoutType.TAPE_80MM)
        assertEquals("<html>close_shift</html>", res)
    }

    /**
     * Перепечатка сохранённого X-отчёта несёт его номер документа.
     *
     * Перепечатывается уже выданный отчёт, у него номер есть, и в шапке
     * он стоит — как у Z-отчёта. Прежде номер не доходил до формы,
     * и строка «Документ №» на перепечатке отсутствовала вовсе.
     */
    @Test
    fun `перепечатка X-отчёта несёт номер документа`() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", setOf(UserRole.CASHIER, UserRole.ADMIN)) } returns mockk()

        val shift = ShiftInfo(
            id = "shift-1",
            kkmId = "kkm-1",
            shiftNo = 1L,
            status = ShiftStatus.OPEN,
            openedAt = 100L
        )
        val xReportDoc = snapshot.copy(id = "doc-x", docType = "REPORT_X", docNo = 77L)
        every { storage.findFiscalDocumentById("doc-x") } returns xReportDoc
        every { storage.findShiftById("shift-1") } returns shift
        every { storage.loadCounters("kkm-1", CounterScopes.SHIFT, "shift-1") } returns emptyMap()
        every { storage.listFiscalDocumentsByShift("kkm-1", "shift-1", any(), any()) } returns emptyList()
        every { receiptRenderPort.renderXReportHtml(any<ZxReportInput>(), kkm, null, "77", null) } returns "<html>xreport 77</html>"

        val res = getPrintHtml.execute("kkm-1", PrintDocumentType.DOCUMENT, "doc-x", null, "1234")
        assertEquals("<html>xreport 77</html>", res)
        verify { receiptRenderPort.renderXReportHtml(any<ZxReportInput>(), kkm, null, "77", null) }
    }

    /**
     * Перепечатка X-отчёта — копия выданного: итоги по документам до его
     * времени и его время. Прежде рисовались нынешние счётчики смены
     * и нынешнее время.
     */
    @Test
    fun `перепечатка X-отчёта несёт итоги и время документа, а не нынешние`() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", setOf(UserRole.CASHIER, UserRole.ADMIN)) } returns mockk()
        val shift = ShiftInfo(id = "shift-1", kkmId = "kkm-1", shiftNo = 1L, status = ShiftStatus.OPEN, openedAt = 100L)
        val before = snapshot.copy(id = "in-1", docType = "CASH_IN", createdAt = 500L, totalAmount = 10_000L, ofdStatus = "SENT")
        val after = snapshot.copy(id = "in-2", docType = "CASH_IN", createdAt = 1_500L, totalAmount = 70_000L, ofdStatus = "SENT")
        every { storage.findFiscalDocumentById("doc-x") } returns snapshot.copy(id = "doc-x", docType = "REPORT_X", createdAt = 1_000L)
        every { storage.findShiftById("shift-1") } returns shift
        every { storage.loadCounters("kkm-1", CounterScopes.SHIFT, "shift-1") } returns emptyMap()
        every { storage.listFiscalDocumentsByShift("kkm-1", "shift-1", any(), 0) } returns listOf(before, after)
        every { storage.listFiscalDocumentsByShift("kkm-1", "shift-1", any(), neq(0)) } returns emptyList()
        val report = slot<ZxReportInput>()
        every { receiptRenderPort.renderXReportHtml(capture(report), kkm, null, any(), null) } returns "<html>x</html>"

        getPrintHtml.execute("kkm-1", PrintDocumentType.DOCUMENT, "doc-x", null, "1234")

        assertEquals(1_000L to 10_000L, report.captured.dateTimeMillis to report.captured.cashSumTiyn)
    }
}
