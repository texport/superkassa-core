package kz.mybrain.superkassa.core.domain.model.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SettingsModelTest {

    @Test
    fun testCoreModeEnum() {
        for (item in CoreMode.entries) {
            val value = CoreMode.valueOf(item.name)
            assertEquals(item, value)
        }
    }

    @Test
    fun testStorageSettings() {
        val s1 = StorageSettings("engine1", "url1", "user1", "pass1")
        val s1Copy = s1.copy()
        val sSame = StorageSettings("engine1", "url1", "user1", "pass1")
        val sDiffEngine = s1.copy(engine = "engine2")
        val sDiffUrl = s1.copy(jdbcUrl = "url2")
        val sDiffUser = s1.copy(user = "user2")
        val sDiffPass = s1.copy(password = "pass2")

        assertEquals(s1, s1)
        assertEquals(s1, s1Copy)
        assertEquals(s1, sSame)
        assertEquals(s1.hashCode(), sSame.hashCode())

        assertNotEquals(s1, Any())
        assertFalse(s1.equals(null))
        assertNotEquals(s1, sDiffEngine)
        assertNotEquals(s1, sDiffUrl)
        assertNotEquals(s1, sDiffUser)
        assertNotEquals(s1, sDiffPass)

        assertTrue(s1.toString().contains("engine=engine1"))
    }

    @Test
    fun testPrintConnectionSettings() {
        val p1 = PrintConnectionSettings("type1", "host1", 9100)
        val p1Copy = p1.copy()
        val pSame = PrintConnectionSettings("type1", "host1", 9100)
        val pDiffType = p1.copy(type = "type2")
        val pDiffHost = p1.copy(host = "host2")
        val pDiffPort = p1.copy(port = 9200)

        assertEquals(p1, p1)
        assertEquals(p1, p1Copy)
        assertEquals(p1, pSame)
        assertEquals(p1.hashCode(), pSame.hashCode())

        assertNotEquals(p1, Any())
        assertFalse(p1.equals(null))
        assertNotEquals(p1, pDiffType)
        assertNotEquals(p1, pDiffHost)
        assertNotEquals(p1, pDiffPort)

        assertTrue(p1.toString().contains("type=type1"))
    }

    @Test
    fun testPrintDeliverySettings() {
        val pConn = PrintConnectionSettings("type1", "host1", 9100)
        val p1 = PrintDeliverySettings(true, 58, pConn)
        val p1Copy = p1.copy()
        val pSame = PrintDeliverySettings(true, 58, pConn)
        val pDiffEnabled = p1.copy(enabled = false)
        val pDiffWidth = p1.copy(paperWidthMm = 80)
        val pDiffConn = p1.copy(connection = null)

        assertEquals(p1, p1)
        assertEquals(p1, p1Copy)
        assertEquals(p1, pSame)
        assertEquals(p1.hashCode(), pSame.hashCode())

        assertNotEquals(p1, Any())
        assertFalse(p1.equals(null))
        assertNotEquals(p1, pDiffEnabled)
        assertNotEquals(p1, pDiffWidth)
        assertNotEquals(p1, pDiffConn)

        assertTrue(p1.toString().contains("paperWidthMm=58"))
    }

    @Test
    fun testEmailProviderSettings() {
        val e1 = EmailProviderSettings("host1", 587, "user1", "pass1", "from1")
        val e1Copy = e1.copy()
        val eSame = EmailProviderSettings("host1", 587, "user1", "pass1", "from1")
        val eDiffHost = e1.copy(host = "host2")
        val eDiffPort = e1.copy(port = 465)
        val eDiffUser = e1.copy(user = "user2")
        val eDiffPass = e1.copy(password = "pass2")
        val eDiffFrom = e1.copy(from = "from2")

        assertEquals(e1, e1)
        assertEquals(e1, e1Copy)
        assertEquals(e1, eSame)
        assertEquals(e1.hashCode(), eSame.hashCode())

        assertNotEquals(e1, Any())
        assertFalse(e1.equals(null))
        assertNotEquals(e1, eDiffHost)
        assertNotEquals(e1, eDiffPort)
        assertNotEquals(e1, eDiffUser)
        assertNotEquals(e1, eDiffPass)
        assertNotEquals(e1, eDiffFrom)

        assertTrue(e1.toString().contains("host=host1"))
    }

    @Test
    fun testSmsProviderSettings() {
        val s1 = SmsProviderSettings("url1", "key1")
        val s1Copy = s1.copy()
        val sSame = SmsProviderSettings("url1", "key1")
        val sDiffUrl = s1.copy(providerUrl = "url2")
        val sDiffKey = s1.copy(apiKey = "key2")

        assertEquals(s1, s1)
        assertEquals(s1, s1Copy)
        assertEquals(s1, sSame)
        assertEquals(s1.hashCode(), sSame.hashCode())

        assertNotEquals(s1, Any())
        assertFalse(s1.equals(null))
        assertNotEquals(s1, sDiffUrl)
        assertNotEquals(s1, sDiffKey)

        assertTrue(s1.toString().contains("providerUrl=url1"))
    }

    @Test
    fun testTelegramProviderSettings() {
        val t1 = TelegramProviderSettings("token1")
        val t1Copy = t1.copy()
        val tSame = TelegramProviderSettings("token1")
        val tDiffToken = t1.copy(botToken = "token2")

        assertEquals(t1, t1)
        assertEquals(t1, t1Copy)
        assertEquals(t1, tSame)
        assertEquals(t1.hashCode(), tSame.hashCode())

        assertNotEquals(t1, Any())
        assertFalse(t1.equals(null))
        assertNotEquals(t1, tDiffToken)

        assertTrue(t1.toString().contains("botToken=token1"))
    }

    @Test
    fun testWhatsAppProviderSettings() {
        val w1 = WhatsAppProviderSettings("token1", "id1")
        val w1Copy = w1.copy()
        val wSame = WhatsAppProviderSettings("token1", "id1")
        val wDiffToken = w1.copy(accessToken = "token2")
        val wDiffId = w1.copy(phoneNumberId = "id2")

        assertEquals(w1, w1)
        assertEquals(w1, w1Copy)
        assertEquals(w1, wSame)
        assertEquals(w1.hashCode(), wSame.hashCode())

        assertNotEquals(w1, Any())
        assertFalse(w1.equals(null))
        assertNotEquals(w1, wDiffToken)
        assertNotEquals(w1, wDiffId)

        assertTrue(w1.toString().contains("accessToken=token1"))
    }

    @Test
    fun testDeliveryChannelSettings() {
        val d1 = DeliveryChannelSettings("EMAIL", true, "DOCUMENT", "PDF", "dest1")
        val d1Copy = d1.copy()
        val dSame = DeliveryChannelSettings("EMAIL", true, "DOCUMENT", "PDF", "dest1")
        val dDiffChannel = d1.copy(channel = "SMS")
        val dDiffEnabled = d1.copy(enabled = false)
        val dDiffPayload = d1.copy(payloadType = "LINK")
        val dDiffFormat = d1.copy(documentFormat = "HTML")
        val dDiffDest = d1.copy(destination = "dest2")

        assertEquals(d1, d1)
        assertEquals(d1, d1Copy)
        assertEquals(d1, dSame)
        assertEquals(d1.hashCode(), dSame.hashCode())

        assertNotEquals(d1, Any())
        assertFalse(d1.equals(null))
        assertNotEquals(d1, dDiffChannel)
        assertNotEquals(d1, dDiffEnabled)
        assertNotEquals(d1, dDiffPayload)
        assertNotEquals(d1, dDiffFormat)
        assertNotEquals(d1, dDiffDest)

        assertTrue(d1.toString().contains("channel=EMAIL"))
    }

    @Test
    fun testDeliverySettings() {
        val print = PrintDeliverySettings()
        val channels = listOf(DeliveryChannelSettings("SMS"))
        val email = EmailProviderSettings()
        val sms = SmsProviderSettings()
        val tg = TelegramProviderSettings()
        val wa = WhatsAppProviderSettings()

        val d1 = DeliverySettings(print, channels, email, sms, tg, wa)
        val d1Copy = d1.copy()
        val dSame = DeliverySettings(print, channels, email, sms, tg, wa)
        val dDiffPrint = d1.copy(print = null)
        val dDiffChannels = d1.copy(channels = emptyList())
        val dDiffEmail = d1.copy(email = null)
        val dDiffSms = d1.copy(sms = null)
        val dDiffTg = d1.copy(telegram = null)
        val dDiffWa = d1.copy(whatsapp = null)

        assertEquals(d1, d1)
        assertEquals(d1, d1Copy)
        assertEquals(d1, dSame)
        assertEquals(d1.hashCode(), dSame.hashCode())

        assertNotEquals(d1, Any())
        assertFalse(d1.equals(null))
        assertNotEquals(d1, dDiffPrint)
        assertNotEquals(d1, dDiffChannels)
        assertNotEquals(d1, dDiffEmail)
        assertNotEquals(d1, dDiffSms)
        assertNotEquals(d1, dDiffTg)
        assertNotEquals(d1, dDiffWa)

        assertTrue(d1.toString().contains("print="))
    }

    @Test
    fun testCoreSettings() {
        val storage = StorageSettings("sqlite", "jdbc:sqlite")
        val delivery = DeliverySettings()
        val c1 = CoreSettings(
            mode = CoreMode.DESKTOP,
            storage = storage,
            allowChanges = true,
            nodeId = "node-1",
            ofdProtocolVersion = "203",
            deliveryChannels = listOf("SMS"),
            ofdTimeoutSeconds = 15L,
            ofdReconnectIntervalSeconds = 45L,
            delivery = delivery,
            defaultAdminPin = "1111",
            defaultAdminName = "Admin",
            defaultCashierPin = "2222",
            defaultCashierName = "Cashier"
        )
        val c1Copy = c1.copy()
        val cSame = c1.copy()
        val cDiffMode = c1.copy(mode = CoreMode.SERVER)
        val cDiffStorage = c1.copy(storage = StorageSettings("postgres", "jdbc:postgres"))
        val cDiffAllow = c1.copy(allowChanges = false)
        val cDiffNode = c1.copy(nodeId = "node-2")
        val cDiffProto = c1.copy(ofdProtocolVersion = "201")
        val cDiffChannels = c1.copy(deliveryChannels = emptyList())
        val cDiffTimeout = c1.copy(ofdTimeoutSeconds = 30L)
        val cDiffReconnect = c1.copy(ofdReconnectIntervalSeconds = 60L)
        val cDiffDelivery = c1.copy(delivery = null)
        val cDiffAdminPin = c1.copy(defaultAdminPin = "0000")
        val cDiffAdminName = c1.copy(defaultAdminName = "Admin2")
        val cDiffCashierPin = c1.copy(defaultCashierPin = "3333")
        val cDiffCashierName = c1.copy(defaultCashierName = "Cashier2")

        assertEquals(c1, c1)
        assertEquals(c1, c1Copy)
        assertEquals(c1, cSame)
        assertEquals(c1.hashCode(), cSame.hashCode())

        assertNotEquals(c1, Any())
        assertFalse(c1.equals(null))
        assertNotEquals(c1, cDiffMode)
        assertNotEquals(c1, cDiffStorage)
        assertNotEquals(c1, cDiffAllow)
        assertNotEquals(c1, cDiffNode)
        assertNotEquals(c1, cDiffProto)
        assertNotEquals(c1, cDiffChannels)
        assertNotEquals(c1, cDiffTimeout)
        assertNotEquals(c1, cDiffReconnect)
        assertNotEquals(c1, cDiffDelivery)
        assertNotEquals(c1, cDiffAdminPin)
        assertNotEquals(c1, cDiffAdminName)
        assertNotEquals(c1, cDiffCashierPin)
        assertNotEquals(c1, cDiffCashierName)

        assertTrue(c1.toString().contains("nodeId=node-1"))
    }
}
