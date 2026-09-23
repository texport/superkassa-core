package io.github.texport.superkassa.core.data.room

import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.ADMIN_PIN
import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.KKM
import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmListParams
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Страница и срок проверяются на входе фасада: отрицательные и огромные
 * лимиты, отрицательное смещение, перевёрнутый и отрицательный срок
 * получают понятный отказ ядра, а не весь объём и не падение базы.
 */
class PageBoundsRoomTest {
    private val kassa = RoomKassa()

    @AfterTest
    fun cleanUp() = kassa.close()

    @Test
    fun `история смен и документы смены отвергают лимит вне 1-500 и отрицательное смещение`() {
        val shiftId = kassa.api.openShift(KKM, ADMIN_PIN).id
        for ((limit, offset, code) in PAGE_CASES) {
            assertEquals(code, refusal { kassa.api.listShifts(KKM, limit, offset, ADMIN_PIN) }, "shifts $limit/$offset")
            assertEquals(code, refusal { kassa.api.listShiftDocuments(KKM, shiftId, limit, offset, ADMIN_PIN) })
            assertEquals(code, refusal { kassa.api.listFiscalDocumentsByPeriod(KKM, 0, DAY, limit, offset, ADMIN_PIN) })
        }
        assertEquals(1, kassa.api.listShifts(KKM, 500, 0, ADMIN_PIN).size)
        kassa.api.listShiftDocuments(KKM, shiftId, 500, 0, ADMIN_PIN)
    }

    @Test
    fun `перевёрнутый, пустой и отрицательный срок получают отказ`() {
        for ((from, to) in listOf(DAY to 0L, DAY to DAY, -DAY to DAY, -DAY to -1L)) {
            assertEquals("PERIOD_INVALID", refusal { kassa.api.listFiscalDocumentsByPeriod(KKM, from, to, 50, 0, ADMIN_PIN) })
        }
        assertTrue(kassa.api.listFiscalDocumentsByPeriod(KKM, 0, Long.MAX_VALUE, 50, 0, ADMIN_PIN).isEmpty())
    }

    @Test
    fun `список касс отвергает лимит вне 1-1000 и отрицательное смещение`() {
        for ((limit, offset) in listOf(-1 to 0, 0 to 0, 1001 to 0, Int.MAX_VALUE to 0)) {
            assertEquals("PAGE_LIMIT_OUT_OF_RANGE", refusal { kassa.api.listKkms(KkmListParams(limit, offset)) })
        }
        assertEquals("PAGE_OFFSET_NEGATIVE", refusal { kassa.api.listKkms(KkmListParams(50, -1)) })
        assertEquals(1, kassa.api.listKkms(KkmListParams(1000, 0)).items.size)
    }

    @Test
    fun `пакет досылки отвергает лимит вне 1-500`() {
        for (limit in listOf(-1, 0, 501)) {
            assertEquals("PAGE_LIMIT_OUT_OF_RANGE", refusal { kassa.api.queue.processOfflineBatch(KKM, limit) })
        }
    }

    private fun refusal(call: () -> Any): String = assertFailsWith<ValidationException> { call() }.code

    private companion object {
        const val DAY = 86_400_000L

        val PAGE_CASES = listOf(
            Triple(-1, 0, "PAGE_LIMIT_OUT_OF_RANGE"),
            Triple(0, 0, "PAGE_LIMIT_OUT_OF_RANGE"),
            Triple(501, 0, "PAGE_LIMIT_OUT_OF_RANGE"),
            Triple(Int.MAX_VALUE, 0, "PAGE_LIMIT_OUT_OF_RANGE"),
            Triple(50, -1, "PAGE_OFFSET_NEGATIVE"),
            Triple(50, Int.MIN_VALUE, "PAGE_OFFSET_NEGATIVE")
        )
    }
}
