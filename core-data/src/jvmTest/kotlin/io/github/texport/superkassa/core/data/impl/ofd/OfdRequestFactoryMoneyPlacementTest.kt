package io.github.texport.superkassa.core.data.impl.ofd

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class OfdRequestFactoryMoneyPlacementTest {

    @Test
    fun `изъятие в разрыве связи помечено автономным`() {
        val placement = moneyPlacement(isOffline = true)
        assertEquals(true, placement["isOffline"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun `сетевое внесение помечено сетевым, а не оставлено без признака`() {
        val placement = moneyPlacement(isOffline = false)
        assertEquals(false, placement["isOffline"]!!.jsonPrimitive.boolean)
    }

    private fun moneyPlacement(isOffline: Boolean): JsonObject =
        OfdRequestFactory.buildMoneyPlacementRequest(
            ofdId = "bfd",
            protocolVersion = "204",
            deviceId = 1L,
            token = 555L,
            reqNum = 1,
            docType = "CASH_OUT",
            amountBills = 250L,
            amountCoins = 50,
            createdAtMillis = 1_788_000_000_000L,
            serviceBlock = buildJsonObject { },
            isOffline = isOffline
        )["payload"]!!.jsonObject["moneyPlacement"]!!.jsonObject
}
