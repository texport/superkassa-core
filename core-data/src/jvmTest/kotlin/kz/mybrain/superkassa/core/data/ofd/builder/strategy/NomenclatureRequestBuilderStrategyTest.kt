package kz.mybrain.superkassa.core.data.ofd.builder.strategy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.int
import kz.mybrain.superkassa.core.data.ofd.OfdConfig
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandRequest
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandType
import kz.mybrain.superkassa.core.domain.model.ofd.OfdServiceInfo

class NomenclatureRequestBuilderStrategyTest {

    @Test
    fun `build constructs valid json request for nomenclature command`() {
        val strategy = NomenclatureRequestBuilderStrategy()
        val serviceInfo = OfdServiceInfo(
            geoLatitude = 43238949,
            geoLongitude = 76889709,
            geoSource = "GPS",
            orgTitle = "Superkassa LLC",
            orgAddress = "Almaty, Abay 10",
            orgAddressKz = "Almaty, Abay 10",
            orgInn = "123456789012",
            orgOkved = "62.01"
        )
        val command = OfdCommandRequest(
            kkmId = "kkm-1",
            commandType = OfdCommandType.NOMENCLATURE,
            payloadRef = "5449000176431",
            ofdProviderId = "kazakhtelecom",
            ofdEnvironmentId = "sandbox",
            deviceId = 203605L,
            token = 2952414999L,
            reqNum = 1042,
            registrationNumber = "99090909090",
            factoryNumber = "SK-100200",
            ofdSystemId = "203605",
            serviceInfo = serviceInfo,
            offlineBeginMillis = 1700000000000L,
            offlineEndMillis = 1700000000000L
        )
        val config = OfdConfig(protocolVersion = "203", prettyPrintJson = false)

        val json = strategy.build(command, config)
        assertNotNull(json)

        assertEquals("kazakhtelecom", json["ofdId"]?.jsonPrimitive?.content)
        assertEquals("203", json["protocolVersion"]?.jsonPrimitive?.content)
        assertEquals("REQUEST", json["messageType"]?.jsonPrimitive?.content)
        assertEquals("COMMAND_NOMENCLATURE", json["commandType"]?.jsonPrimitive?.content)

        val header = json["header"]?.jsonObject
        assertNotNull(header)
        assertEquals(203605L, header["deviceId"]?.jsonPrimitive?.long)
        assertEquals(2952414999L, header["token"]?.jsonPrimitive?.long)
        assertEquals(1042, header["reqNum"]?.jsonPrimitive?.int)

        val payload = json["payload"]?.jsonObject
        assertNotNull(payload)
        val service = payload["service"]?.jsonObject
        assertNotNull(service)

        val nomenclature = payload["nomenclature"]?.jsonObject
        assertNotNull(nomenclature)
        assertEquals(1, nomenclature["currentVersion"]?.jsonPrimitive?.int)
        assertEquals("5449000176431", nomenclature["barcode"]?.jsonPrimitive?.content)
    }
}
