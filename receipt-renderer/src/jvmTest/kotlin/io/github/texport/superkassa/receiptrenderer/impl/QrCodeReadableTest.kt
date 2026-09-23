package io.github.texport.superkassa.receiptrenderer.impl

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import io.github.texport.superkassa.receiptrenderer.impl.adapter.DefaultQrCodeGeneratorAdapter
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * QR-код чека читается сканером и ведёт туда же, куда ссылка.
 *
 * Прежний генератор рисовал угловые метки и шум: картинка походила
 * на QR, но не читалась ничем. Проверка — чтением, а не видом.
 */
class QrCodeReadableTest {
    @Test
    fun `код читается обратно в ссылку проверки чека`() {
        val link = "https://consumer.oofd.kz/r/12345?r=010101012345&i=1&f=987654321012&s=1500.00&t=20260923T194515"

        val uri = assertNotNull(DefaultQrCodeGeneratorAdapter().generatePngDataUri(link, 180))
        val png = Base64.getDecoder().decode(uri.substringAfter("base64,"))
        val image = assertNotNull(ImageIO.read(png.inputStream()))
        val pixels = IntArray(image.width * image.height).also { image.getRGB(0, 0, image.width, image.height, it, 0, image.width) }

        val read = QRCodeReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(image.width, image.height, pixels))))

        assertEquals(link, read.text)
    }
}
