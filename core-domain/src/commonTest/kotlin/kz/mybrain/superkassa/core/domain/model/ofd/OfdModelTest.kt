package kz.mybrain.superkassa.core.domain.model.ofd

import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OfdModelTest {

    @Test
    fun testOfdCommandStatusEnum() {
        for (item in OfdCommandStatus.entries) {
            val value = OfdCommandStatus.valueOf(item.name)
            assertEquals(item, value)
        }
    }

    @Test
    fun testOfdCommandTypeEnum() {
        assertEquals(OfdCommandType.TICKET, OfdCommandType.fromString("COMMAND_TICKET"))
        assertEquals(OfdCommandType.CLOSE_SHIFT, OfdCommandType.fromString("COMMAND_CLOSE_SHIFT"))

        assertFailsWithIllegalArgument {
            OfdCommandType.fromString("UNKNOWN_CMD")
        }

        for (item in OfdCommandType.entries) {
            val value = OfdCommandType.valueOf(item.name)
            assertEquals(item, value)
            assertEquals(item, OfdCommandType.fromString(item.value))
        }
    }

    private fun assertFailsWithIllegalArgument(block: () -> Unit) {
        try {
            block()
            assertTrue(false, "Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {}
    }

    @Test
    fun testOfdEnvironmentEnum() {
        assertEquals(OfdEnvironment.DEV, OfdEnvironment.findEnvironment("dev"))
        assertEquals(OfdEnvironment.TEST, OfdEnvironment.findEnvironment("TEST"))
        assertNull(OfdEnvironment.findEnvironment("UNKNOWN_ENV"))

        for (item in OfdEnvironment.entries) {
            val value = OfdEnvironment.valueOf(item.name)
            assertEquals(item, value)
            assertEquals(item, OfdEnvironment.findEnvironment(item.id))
        }
    }

    @Test
    fun testOfdProviderEnum() {
        assertEquals(OfdProvider.KAZAKHTELECOM, OfdProvider.findProvider("KAZAKHTELECOM"))
        assertEquals(OfdProvider.KAZAKHTELECOM, OfdProvider.findProvider("kazakhtelecom"))
        assertNull(OfdProvider.findProvider("UNKNOWN_PROVIDER"))

        for (item in OfdProvider.entries) {
            val value = OfdProvider.valueOf(item.name)
            assertEquals(item, value)
            assertEquals(item, OfdProvider.findProvider(item.id))
            assertNotNullOrEmpty(item.nameRu)
            assertNotNullOrEmpty(item.nameKk)
            assertNotNullOrEmpty(item.website)
            assertTrue(item.endpoints.isNotEmpty())
        }
    }

    private fun assertNotNullOrEmpty(str: String) {
        assertTrue(str.isNotEmpty())
    }

    @Test
    fun testOfdEndpoint() {
        val ep1 = OfdEndpoint("host1", 1234, "checkDomain1")
        val ep1Copy = ep1.copy()
        val epSame = OfdEndpoint("host1", 1234, "checkDomain1")
        val epDiffHost = ep1.copy(host = "host2")
        val epDiffPort = ep1.copy(port = 4321)
        val epDiffDomain = ep1.copy(checkDomain = "checkDomain2")

        assertEquals(ep1, ep1)
        assertEquals(ep1, ep1Copy)
        assertEquals(ep1, epSame)
        assertEquals(ep1.hashCode(), epSame.hashCode())

        assertNotEquals(ep1, Any())
        assertFalse(ep1.equals(null))
        assertNotEquals(ep1, epDiffHost)
        assertNotEquals(ep1, epDiffPort)
        assertNotEquals(ep1, epDiffDomain)

        assertTrue(ep1.toString().contains("host=host1"))
    }

    @Test
    fun testOfdAuthInfo() {
        val auth1 = OfdAuthInfo("token1", 5)
        val auth1Copy = auth1.copy()
        val authSame = OfdAuthInfo("token1", 5)
        val authDiffToken = auth1.copy(token = "token2")
        val authDiffReq = auth1.copy(nextReqNum = 6)
        val authNullToken = auth1.copy(token = null)

        assertEquals(auth1, auth1)
        assertEquals(auth1, auth1Copy)
        assertEquals(auth1, authSame)
        assertEquals(auth1.hashCode(), authSame.hashCode())

        assertNotEquals(auth1, Any())
        assertFalse(auth1.equals(null))
        assertNotEquals(auth1, authDiffToken)
        assertNotEquals(auth1, authDiffReq)
        assertNotEquals(auth1, authNullToken)
        assertNotEquals(authNullToken, auth1)

        assertTrue(auth1.toString().contains("token=token1"))
    }

    @Test
    fun testOfdProviderConfig() {
        val cfg1 = OfdProviderConfig("ru", "kk", "web", "domain")
        val cfg1Copy = cfg1.copy()
        val cfgSame = OfdProviderConfig("ru", "kk", "web", "domain")
        val cfgDiffRu = cfg1.copy(nameRu = "ru2")
        val cfgDiffKk = cfg1.copy(nameKk = "kk2")
        val cfgDiffWeb = cfg1.copy(website = "web2")
        val cfgDiffDomain = cfg1.copy(checkDomain = "domain2")

        assertEquals(cfg1, cfg1)
        assertEquals(cfg1, cfg1Copy)
        assertEquals(cfg1, cfgSame)
        assertEquals(cfg1.hashCode(), cfgSame.hashCode())

        assertNotEquals(cfg1, Any())
        assertFalse(cfg1.equals(null))
        assertNotEquals(cfg1, cfgDiffRu)
        assertNotEquals(cfg1, cfgDiffKk)
        assertNotEquals(cfg1, cfgDiffWeb)
        assertNotEquals(cfg1, cfgDiffDomain)

        assertTrue(cfg1.toString().contains("nameRu=ru"))
    }

    @Test
    fun testOfdServiceInfo() {
        val s1 = OfdServiceInfo("title", "addr", "addrKz", "inn", "okved", 10, 20, "GPS")
        val s1Copy = s1.copy()
        val sSame = OfdServiceInfo("title", "addr", "addrKz", "inn", "okved", 10, 20, "GPS")
        val sDiffTitle = s1.copy(orgTitle = "title2")
        val sDiffAddr = s1.copy(orgAddress = "addr2")
        val sDiffAddrKz = s1.copy(orgAddressKz = "addrKz2")
        val sDiffInn = s1.copy(orgInn = "inn2")
        val sDiffOkved = s1.copy(orgOkved = "okved2")
        val sDiffLat = s1.copy(geoLatitude = 11)
        val sDiffLong = s1.copy(geoLongitude = 21)
        val sDiffSrc = s1.copy(geoSource = "MANUAL")

        assertEquals(s1, s1)
        assertEquals(s1, s1Copy)
        assertEquals(s1, sSame)
        assertEquals(s1.hashCode(), sSame.hashCode())

        assertNotEquals(s1, Any())
        assertFalse(s1.equals(null))
        assertNotEquals(s1, sDiffTitle)
        assertNotEquals(s1, sDiffAddr)
        assertNotEquals(s1, sDiffAddrKz)
        assertNotEquals(s1, sDiffInn)
        assertNotEquals(s1, sDiffOkved)
        assertNotEquals(s1, sDiffLat)
        assertNotEquals(s1, sDiffLong)
        assertNotEquals(s1, sDiffSrc)

        assertTrue(s1.toString().contains("orgTitle=title"))
    }

    @Test
    fun testOfdCommandRequest() {
        val service = OfdServiceInfo("title", "addr", "addrKz", "inn", "okved", 10, 20, "GPS")
        val req1 = OfdCommandRequest("kkm", OfdCommandType.TICKET, "ref", "telecom", "PROD", 1L, 2L, 3, "reg", "fac", "sys", service, 100L, 200L)
        val req1Copy = req1.copy()
        val reqSame = OfdCommandRequest("kkm", OfdCommandType.TICKET, "ref", "telecom", "PROD", 1L, 2L, 3, "reg", "fac", "sys", service, 100L, 200L)
        val reqDiffKkm = req1.copy(kkmId = "kkm2")
        val reqDiffCmd = req1.copy(commandType = OfdCommandType.SYSTEM)
        val reqDiffRef = req1.copy(payloadRef = "ref2")
        val reqDiffProvider = req1.copy(ofdProviderId = "other")
        val reqDiffEnv = req1.copy(ofdEnvironmentId = "DEV")
        val reqDiffDev = req1.copy(deviceId = 9L)
        val reqDiffToken = req1.copy(token = 9L)
        val reqDiffNum = req1.copy(reqNum = 9)
        val reqDiffReg = req1.copy(registrationNumber = "reg2")
        val reqDiffFac = req1.copy(factoryNumber = "fac2")
        val reqDiffSys = req1.copy(ofdSystemId = "sys2")
        val reqDiffService = req1.copy(serviceInfo = null)
        val reqDiffBegin = req1.copy(offlineBeginMillis = 900L)
        val reqDiffEnd = req1.copy(offlineEndMillis = 900L)

        assertEquals(req1, req1)
        assertEquals(req1, req1Copy)
        assertEquals(req1, reqSame)
        assertEquals(req1.hashCode(), reqSame.hashCode())

        assertNotEquals(req1, Any())
        assertFalse(req1.equals(null))
        assertNotEquals(req1, reqDiffKkm)
        assertNotEquals(req1, reqDiffCmd)
        assertNotEquals(req1, reqDiffRef)
        assertNotEquals(req1, reqDiffProvider)
        assertNotEquals(req1, reqDiffEnv)
        assertNotEquals(req1, reqDiffDev)
        assertNotEquals(req1, reqDiffToken)
        assertNotEquals(req1, reqDiffNum)
        assertNotEquals(req1, reqDiffReg)
        assertNotEquals(req1, reqDiffFac)
        assertNotEquals(req1, reqDiffSys)
        assertNotEquals(req1, reqDiffService)
        assertNotEquals(req1, reqDiffBegin)
        assertNotEquals(req1, reqDiffEnd)

        assertTrue(req1.toString().contains("kkmId=kkm"))
    }

    @Test
    fun testOfdCommandResultEqualsAndHashCode() {
        val bin1 = byteArrayOf(1, 2)
        val bin2 = byteArrayOf(1, 2)
        val bin3 = byteArrayOf(3, 4)
        val json = JsonObject(emptyMap())

        val res1 = OfdCommandResult(OfdCommandStatus.OK, bin1, json, 100L, 5, 200, "text", "fs", "as", "error", "url")
        val res1Copy = res1.copy()
        val resSame = OfdCommandResult(OfdCommandStatus.OK, bin2, json, 100L, 5, 200, "text", "fs", "as", "error", "url")
        val resDiffStatus = res1.copy(status = OfdCommandStatus.FAILED)
        val resDiffBin = res1.copy(responseBin = bin3)
        val resDiffJson = res1.copy(responseJson = null)
        val resDiffToken = res1.copy(responseToken = 900L)
        val resDiffReq = res1.copy(responseReqNum = 9)
        val resDiffCode = res1.copy(resultCode = 9)
        val resDiffText = res1.copy(resultText = "diff")
        val resDiffFs = res1.copy(fiscalSign = "diff")
        val resDiffAs = res1.copy(autonomousSign = "diff")
        val resDiffErr = res1.copy(errorMessage = "diff")
        val resDiffUrl = res1.copy(receiptUrl = "diff")
        val resNullBin = res1.copy(responseBin = null)

        assertEquals(res1, res1)
        assertEquals(res1, res1Copy)
        assertEquals(res1, resSame)
        assertEquals(res1.hashCode(), resSame.hashCode())

        assertNotEquals(res1, Any())
        assertFalse(res1.equals(null))
        assertNotEquals(res1, resDiffStatus)
        assertNotEquals(res1, resDiffBin)
        assertNotEquals(res1, resDiffJson)
        assertNotEquals(res1, resDiffToken)
        assertNotEquals(res1, resDiffReq)
        assertNotEquals(res1, resDiffCode)
        assertNotEquals(res1, resDiffText)
        assertNotEquals(res1, resDiffFs)
        assertNotEquals(res1, resDiffAs)
        assertNotEquals(res1, resDiffErr)
        assertNotEquals(res1, resDiffUrl)
        assertNotEquals(res1, resNullBin)
        assertNotEquals(resNullBin, res1)

        assertTrue(res1.toString().contains("status=OK"))
        assertTrue(resNullBin.toString().contains("responseBin=null"))
    }
}
