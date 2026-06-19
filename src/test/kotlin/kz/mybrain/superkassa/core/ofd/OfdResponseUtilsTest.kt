package kz.mybrain.superkassa.core.ofd

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kz.mybrain.superkassa.core.data.ofd.OfdResponseUtils
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class OfdResponseUtilsTest {

    @Test
    fun `extractReceiptUrl decodes ticket qrCodeBase64`() {
        val response = Json.parseToJsonElement(
            """
            {
              "payload": {
                "ticket": {
                  "qrCodeBase64": "aHR0cHM6Ly9vZmQuZXhhbXBsZS9yLzEyMw=="
                }
              }
            }
            """.trimIndent()
        ).jsonObject

        val receiptUrl = OfdResponseUtils.extractReceiptUrl(response)

        assertEquals("https://ofd.example/r/123", receiptUrl)
    }

    @Test
    fun `extractFiscalSign reads sign from ticket and report variants`() {
        val ticketResponse = Json.parseToJsonElement(
            """
            {
              "payload": {
                "ticket": {
                  "fiscalSign": "FS-TICKET-1"
                }
              }
            }
            """.trimIndent()
        ).jsonObject
        val reportResponse = Json.parseToJsonElement(
            """
            {
              "payload": {
                "report": {
                  "zxReport": {
                    "fiscalSignature": "FS-REPORT-1"
                  }
                }
              }
            }
            """.trimIndent()
        ).jsonObject

        assertEquals("FS-TICKET-1", OfdResponseUtils.extractFiscalSign(ticketResponse))
        assertEquals("FS-REPORT-1", OfdResponseUtils.extractFiscalSign(reportResponse))
        assertNull(OfdResponseUtils.extractFiscalSign(null))
    }

    @Test
    fun `extract uses protocol ticket_number and qr_code`() {
        val response = Json.parseToJsonElement(
            """
            {
              "payload": {
                "ticket": {
                  "ticket_number": "TICKET-777",
                  "qr_code": "https://ofd.example/receipt/777"
                }
              }
            }
            """.trimIndent()
        ).jsonObject

        assertEquals("TICKET-777", OfdResponseUtils.extractFiscalSign(response))
        assertEquals("https://ofd.example/receipt/777", OfdResponseUtils.extractReceiptUrl(response))
    }
}
