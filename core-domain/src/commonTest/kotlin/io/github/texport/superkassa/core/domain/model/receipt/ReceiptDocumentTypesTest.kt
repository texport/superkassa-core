package io.github.texport.superkassa.core.domain.model.receipt

import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptDocumentTypes
import kotlin.test.Test
import kotlin.test.assertEquals

class ReceiptDocumentTypesTest {

    @Test
    fun `x report is stored under the dictionary name`() {
        assertEquals("X_REPORT", ReceiptDocumentTypes.X_REPORT)
    }

    @Test
    fun `legacy x report is renamed on read`() {
        assertEquals(
            ReceiptDocumentTypes.X_REPORT,
            ReceiptDocumentTypes.canonical(ReceiptDocumentTypes.LEGACY_X_REPORT)
        )
    }

    @Test
    fun `other document types are left as they are`() {
        assertEquals(ReceiptDocumentTypes.SALE, ReceiptDocumentTypes.canonical(ReceiptDocumentTypes.SALE))
        assertEquals("SHIFT_CLOSE", ReceiptDocumentTypes.canonical("SHIFT_CLOSE"))
    }
}
