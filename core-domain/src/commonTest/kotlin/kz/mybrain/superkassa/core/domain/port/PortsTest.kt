package kz.mybrain.superkassa.core.domain.port

import kotlin.test.Test
import kotlin.test.assertNull

class PortsTest {

    @Test
    fun testAllPortsExist() {
        val clockPort: ClockPort? = null
        val coreSettingsRepositoryPort: CoreSettingsRepositoryPort? = null
        val deliveryPort: DeliveryPort? = null
        val documentConvertPort: DocumentConvertPort? = null
        val idGeneratorPort: IdGeneratorPort? = null
        val ofdConfigPort: OfdConfigPort? = null
        val ofdManagerPort: OfdManagerPort? = null
        val offlineQueuePort: OfflineQueuePort? = null
        val pinHasherPort: PinHasherPort? = null
        val qrCodeGeneratorPort: QrCodeGeneratorPort? = null
        val receiptRenderPort: ReceiptRenderPort? = null
        val storagePort: StoragePort? = null
        val timeValidatorPort: TimeValidatorPort? = null
        val tokenCodecPort: TokenCodecPort? = null

        assertNull(clockPort)
        assertNull(coreSettingsRepositoryPort)
        assertNull(deliveryPort)
        assertNull(documentConvertPort)
        assertNull(idGeneratorPort)
        assertNull(ofdConfigPort)
        assertNull(ofdManagerPort)
        assertNull(offlineQueuePort)
        assertNull(pinHasherPort)
        assertNull(qrCodeGeneratorPort)
        assertNull(receiptRenderPort)
        assertNull(storagePort)
        assertNull(timeValidatorPort)
        assertNull(tokenCodecPort)
    }
}
