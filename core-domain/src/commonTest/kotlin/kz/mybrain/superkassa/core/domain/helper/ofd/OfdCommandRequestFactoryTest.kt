package kz.mybrain.superkassa.core.domain.helper.ofd

import io.mockk.every
import io.mockk.mockk
import kz.mybrain.superkassa.core.domain.exception.ValidationException
import kz.mybrain.superkassa.core.domain.model.kkm.KkmInfo
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandType
import kz.mybrain.superkassa.core.domain.model.ofd.OfdServiceInfo
import kz.mybrain.superkassa.core.domain.port.OfdConfigPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OfdCommandRequestFactoryTest {

    private val ofdConfig = mockk<OfdConfigPort>()
    private val factory = OfdCommandRequestFactory(ofdConfig)

    private val defaultService = OfdServiceInfo(
        orgTitle = "Org",
        orgAddress = "Addr",
        orgAddressKz = "AddrKz",
        orgInn = "123",
        orgOkved = "001",
        geoLatitude = 0,
        geoLongitude = 0,
        geoSource = "src"
    )

    @Test
    fun testBuildSuccess() {
        val kkm = KkmInfo(
            id = "kkm-1",
            createdAt = 0L,
            updatedAt = 0L,
            mode = "ACTIVE",
            state = "ACTIVE",
            ofdProvider = "PROVIDER_TAG",
            systemId = "12345",
            registrationNumber = "REG_NUM",
            factoryNumber = "FAC_NUM"
        )
        every { ofdConfig.parseTag("PROVIDER_TAG") } returns ("PROVIDER" to "PRODUCTION")
        every { ofdConfig.validateAndFormatTag("PROVIDER", "PRODUCTION") } returns "PROVIDER:PRODUCTION"

        val request = factory.build(
            kkm = kkm,
            commandType = OfdCommandType.TICKET,
            payloadRef = "payload-1",
            token = 100L,
            reqNum = 1,
            now = 1000L,
            defaultServiceInfo = { defaultService }
        )

        assertEquals("kkm-1", request.kkmId)
        assertEquals(OfdCommandType.TICKET, request.commandType)
        assertEquals("payload-1", request.payloadRef)
        assertEquals("PROVIDER", request.ofdProviderId)
        assertEquals("PRODUCTION", request.ofdEnvironmentId)
        assertEquals(12345L, request.deviceId)
        assertEquals(100L, request.token)
        assertEquals(1, request.reqNum)
        assertEquals("REG_NUM", request.registrationNumber)
        assertEquals("FAC_NUM", request.factoryNumber)
        assertEquals(defaultService, request.serviceInfo)
        assertEquals(1000L, request.offlineBeginMillis)
        assertEquals(1000L, request.offlineEndMillis)
    }

    @Test
    fun testBuildMissingProvider() {
        val kkm = KkmInfo(id = "kkm-1", createdAt = 0L, updatedAt = 0L, mode = "ACTIVE", state = "ACTIVE")
        assertFailsWith<ValidationException> {
            factory.build(
                kkm = kkm,
                commandType = OfdCommandType.TICKET,
                payloadRef = "p",
                token = 1,
                reqNum = 1,
                now = 1,
                defaultServiceInfo = { defaultService }
            )
        }
    }

    @Test
    fun testBuildInvalidSystemId() {
        val kkm = KkmInfo(
            id = "kkm-1",
            createdAt = 0L,
            updatedAt = 0L,
            mode = "ACTIVE",
            state = "ACTIVE",
            ofdProvider = "TAG",
            systemId = "not-a-number"
        )
        every { ofdConfig.parseTag("TAG") } returns ("P" to "E")
        every { ofdConfig.validateAndFormatTag("P", "E") } returns "P:E"

        assertFailsWith<ValidationException> {
            factory.build(
                kkm = kkm,
                commandType = OfdCommandType.TICKET,
                payloadRef = "p",
                token = 1,
                reqNum = 1,
                now = 1,
                defaultServiceInfo = { defaultService }
            )
        }
    }
}
