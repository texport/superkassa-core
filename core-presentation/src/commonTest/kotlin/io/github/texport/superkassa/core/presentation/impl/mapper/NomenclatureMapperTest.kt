package io.github.texport.superkassa.core.presentation.impl.mapper

import io.github.texport.superkassa.core.domain.api.model.ofd.OfdNomenclatureItem
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdNomenclatureLookupResult
import io.github.texport.superkassa.core.presentation.api.model.ofd.NomenclatureItemResponse
import io.github.texport.superkassa.core.presentation.api.model.ofd.NomenclatureLookupResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class NomenclatureMapperTest {

    @Test
    fun testToResponseNotFound() {
        val result = OfdNomenclatureLookupResult(
            found = false,
            item = null,
            resultCode = 1,
            resultText = "Not Found"
        )
        val response = OfdMapper.toResponse(result)
        assertEquals(false, response.found)
        assertNull(response.item)
        assertEquals(1, response.resultCode)
        assertEquals("Not Found", response.resultText)
    }

    @Test
    fun testToResponseFound() {
        val item = OfdNomenclatureItem(
            id = 1L,
            barcode = "12345",
            name = "Test Item",
            nameKk = "Test Item Kk",
            ntin = "ntin-123",
            price = 1000.0,
            measureUnitCode = "163",
            vatGroup = "VAT_16"
        )
        val result = OfdNomenclatureLookupResult(
            found = true,
            item = item,
            resultCode = 0,
            resultText = "OK"
        )
        val response = OfdMapper.toResponse(result)
        assertEquals(true, response.found)
        assertEquals(0, response.resultCode)
        assertEquals("OK", response.resultText)
        
        val itemResponse = response.item
        assertNotNull(itemResponse)
        assertEquals(1L, itemResponse.id)
        assertEquals("12345", itemResponse.barcode)
        assertEquals("Test Item", itemResponse.name)
        assertEquals("Test Item Kk", itemResponse.nameKk)
        assertEquals("ntin-123", itemResponse.ntin)
        assertEquals(1000.0, itemResponse.price)
        assertEquals("163", itemResponse.measureUnitCode)
        assertEquals("VAT_16", itemResponse.vatGroup)
    }
}
