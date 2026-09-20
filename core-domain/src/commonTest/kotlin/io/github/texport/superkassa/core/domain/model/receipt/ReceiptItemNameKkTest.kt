package io.github.texport.superkassa.core.domain.model.receipt

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReceiptItemNameKkTest {

    @Test
    fun `позиция хранит казахское наименование рядом с русским`() {
        val item = item(nameKk = "Қой еті")

        assertEquals("Баранина", item.name)
        assertEquals("Қой еті", item.nameKk)
    }

    @Test
    fun `без казахского наименования позиция остаётся прежней`() {
        assertNull(item(nameKk = null).nameKk)
    }

    private fun item(nameKk: String?) = ReceiptItem(
        name = "Баранина",
        nameKk = nameKk,
        sectionCode = "001",
        quantity = 1000,
        price = Money.fromTenge(Decimal.parse("407.41")),
        sum = Money.fromTenge(Decimal.parse("407.41"))
    )
}
