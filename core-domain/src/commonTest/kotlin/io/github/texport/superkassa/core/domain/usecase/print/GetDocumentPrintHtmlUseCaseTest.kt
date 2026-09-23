package io.github.texport.superkassa.core.domain.impl.usecase.print

import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptLayoutType
import io.github.texport.superkassa.core.domain.api.model.report.PrintDocumentType
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftStatus
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Выбор формы по одному идентификатору: тот же, что у узла в печати
 * `GET /kkm/{kkmId}/documents/{documentId}/print.html`.
 */
class GetDocumentPrintHtmlUseCaseTest {

    private val storage = mockk<StoragePort>()
    private val getPrintHtml = mockk<GetPrintHtmlUseCase>()
    private val useCase = GetDocumentPrintHtmlUseCase(storage, getPrintHtml)

    private val shift = ShiftInfo(
        id = "shift-1",
        kkmId = KKM,
        shiftNo = 12,
        status = ShiftStatus.CLOSED,
        openedAt = 1,
        closedAt = 2,
        openDocumentId = "open-doc",
        closeDocumentId = "close-doc"
    )

    init {
        every { storage.listShifts(KKM, 100, 0) } returns listOf(shift)
        every { getPrintHtml.execute(any(), any(), any(), any(), any(), any()) } answers {
            "${secondArg<PrintDocumentType>()}|${thirdArg<String?>()}|${arg<String?>(3)}|${arg<ReceiptLayoutType?>(5)}"
        }
    }

    @Test
    fun `чек и прочие документы журнала рисуются формой документа`() {
        assertEquals("DOCUMENT|doc-7|null|TAPE_58MM", useCase.execute(KKM, "doc-7", PIN, ReceiptLayoutType.TAPE_58MM))
    }

    @Test
    fun `документ открытия смены рисуется формой открытия`() {
        assertEquals("OPEN_SHIFT|null|shift-1|null", useCase.execute(KKM, "open-doc", PIN, null))
    }

    @Test
    fun `документ закрытия и сама смена рисуются Z-отчётом`() {
        assertEquals("CLOSE_SHIFT|null|shift-1|null", useCase.execute(KKM, "close-doc", PIN, null))
        assertEquals("CLOSE_SHIFT|null|shift-1|null", useCase.execute(KKM, "shift-1", PIN, null))
    }

    @Test
    fun `касса без смен рисует документ по идентификатору`() {
        every { storage.listShifts(KKM, 100, 0) } returns emptyList()

        assertEquals("DOCUMENT|open-doc|null|null", useCase.execute(KKM, "open-doc", PIN, null))
    }

    private companion object {
        const val KKM = "kkm-1"
        const val PIN = "4827"
    }
}
