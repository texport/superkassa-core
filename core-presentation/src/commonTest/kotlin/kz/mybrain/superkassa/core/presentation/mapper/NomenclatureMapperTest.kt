package kz.mybrain.superkassa.core.presentation.mapper

import kz.mybrain.superkassa.core.domain.model.ofd.OfdNomenclatureItem
import kz.mybrain.superkassa.core.domain.model.ofd.OfdNomenclatureLookupResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class NomenclatureMapperTest {

    @Test
    fun testToDtoNotFound() {
        val result = OfdNomenclatureLookupResult(
            found = false,
            item = null,
            resultCode = 1,
            resultText = "Not Found"
        )
        val response = NomenclatureMapper.toDto(result)
        assertEquals(false, response.found)
        assertNull(response.item)
        assertEquals(1, response.resultCode)
        assertEquals("Not Found", response.resultText)
    }

    @Test
    fun testToDtoFound() {
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
        val response = NomenclatureMapper.toDto(result)
        assertEquals(true, response.found)
        assertEquals(0, response.resultCode)
        assertEquals("OK", response.resultText)
        
        val dto = response.item
        assertNotNull(dto)
        assertEquals(1L, dto.id)
        assertEquals("12345", dto.barcode)
        assertEquals("Test Item", dto.name)
        assertEquals("Test Item Kk", dto.nameKk)
        assertEquals("ntin-123", dto.ntin)
        assertEquals(1000.0, dto.price)
        assertEquals("163", dto.measureUnitCode)
        assertEquals("VAT_16", dto.vatGroup)
    }
}
