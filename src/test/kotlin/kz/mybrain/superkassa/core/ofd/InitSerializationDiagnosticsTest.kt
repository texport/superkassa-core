package kz.mybrain.superkassa.core.ofd

import kotlin.test.Test
import kotlin.test.assertTrue
import kz.mybrain.superkassa.core.data.adapter.ServiceRequestBuilderStrategy
import kz.mybrain.superkassa.core.data.ofd.OfdCodecService
import kz.mybrain.superkassa.core.data.ofd.OfdConfig
import kz.mybrain.superkassa.core.domain.model.OfdCommandRequest
import kz.mybrain.superkassa.core.domain.model.OfdCommandType
import kz.mybrain.superkassa.core.domain.model.OfdServiceInfo

class InitSerializationDiagnosticsTest {

    @Test
    fun `command system payload serializes for init`() {
        val command = baseServiceCommand(OfdCommandType.SYSTEM)
        val json = ServiceRequestBuilderStrategy().build(command, OfdConfig("203"))
            ?: error("Service request json is null")
        val bytes = OfdCodecService().encode(json)
        assertTrue(bytes.isNotEmpty())
    }

    @Test
    fun `command info payload serializes for init`() {
        val command = baseServiceCommand(OfdCommandType.INFO)
        val json = ServiceRequestBuilderStrategy().build(command, OfdConfig("203"))
            ?: error("Service request json is null")
        val bytes = OfdCodecService().encode(json)
        assertTrue(bytes.isNotEmpty())
    }

    private fun baseServiceCommand(type: OfdCommandType): OfdCommandRequest {
        return OfdCommandRequest(
            kkmId = "kkm-test",
            commandType = type,
            payloadRef = "payload-1",
            ofdProviderId = "KAZAKHTELECOM",
            ofdEnvironmentId = "TEST",
            deviceId = 201873L,
            token = 208627316L,
            reqNum = 1,
            registrationNumber = "TEMP_REG_12345678",
            factoryNumber = "TEMP_FACTORY_12345678",
            ofdSystemId = "201873",
            serviceInfo = OfdServiceInfo(
                orgTitle = "UNKNOWN",
                orgAddress = "UNKNOWN",
                orgAddressKz = "UNKNOWN",
                orgInn = "000000000000",
                orgOkved = "00000",
                geoLatitude = 0,
                geoLongitude = 0,
                geoSource = "UNKNOWN"
            ),
            offlineBeginMillis = 1_700_000_000_000L,
            offlineEndMillis = 1_700_000_000_000L
        )
    }
}
