package io.github.texport.superkassa.embedded.impl.document

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.roundToInt
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Печатные формы на Android: системный WebView рисует чек вне экрана.
 *
 * Образы складываются во внешний каталог файлов проверки — их можно забрать
 * `adb pull` и посмотреть глазами.
 */
@RunWith(AndroidJUnit4::class)
class WebViewDocumentsTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val documents = WebViewDocuments(context)
    private val out = File(context.getExternalFilesDir(null), "documents").apply { mkdirs() }

    @Test
    fun pngOfKazakhReceiptIsTheTapeByContent() {
        val png = documents.htmlToImage(fixture("kazakh-receipt.html"))
        File(out, "kazakh-receipt.png").writeBytes(png)
        val image = checkNotNull(BitmapFactory.decodeByteArray(png, 0, png.size))

        assertEquals(tapePx(TAPE_80_MM, SCALE), image.width)
        assertTrue(image.height > image.width, "the tape is taller than wide: ${image.width} x ${image.height}")
        assertInkedToTheBottom(image, tapePx(BOTTOM_SLACK_MM, SCALE))
    }

    @Test
    fun pdfOfKazakhReceiptIsOnePageOfTape() {
        val pdf = documents.htmlToPdf(fixture("kazakh-receipt.html"))
        val file = File(out, "kazakh-receipt.pdf").apply { writeBytes(pdf) }
        PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
            assertEquals(1, renderer.pageCount)
            renderer.openPage(0).use { page ->
                assertEquals((TAPE_80_MM * POINTS_PER_MM).roundToInt(), page.width)
                assertTrue(page.height > page.width, "the page is the tape: ${page.width} x ${page.height}")
                val image = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                image.eraseColor(Color.WHITE)
                page.render(image, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                assertInkedToTheBottom(image, (BOTTOM_SLACK_MM * POINTS_PER_MM).roundToInt())
            }
        }
    }

    @Test
    fun tapeLongerThanTheWindowIsDrawnToTheEnd() {
        val lines = (1..LONG_LINES).joinToString("") { "<p>Жол $it — 100,00 ₸</p>" }
        val png = documents.htmlToImage("""<div class="receipt tape-58mm">$lines<p>СОҢЫ</p></div>""")
        File(out, "long-tape.png").writeBytes(png)
        val image = checkNotNull(BitmapFactory.decodeByteArray(png, 0, png.size))

        assertEquals(tapePx(TAPE_58_MM, SCALE), image.width)
        assertTrue(image.height > image.width * LONG_RATIO, "the whole tape is drawn: ${image.width} x ${image.height}")
        assertInkedToTheBottom(image, tapePx(BOTTOM_SLACK_MM, SCALE))
    }

    @Test
    fun xReportRendersToEscPos() {
        val escPos = documents.htmlToEscPos(fixture("x-report.html"), ESC_POS_TAPE_MM)
        File(out, "x-report.escpos").writeBytes(escPos)

        assertContentEquals(byteArrayOf(ESC, '@'.code.toByte()), escPos.copyOfRange(0, 2))
        assertContentEquals(byteArrayOf(GS, 'v'.code.toByte(), '0'.code.toByte()), escPos.copyOfRange(2, 5))
    }

    @Test
    fun formFamiliesAreDrawnWithTheBundledFont() {
        val text = "Нан ӘәҒғҚқҢңӨөҰұҮүҺһІі Bread №5 — 500,00 ₸"
        val page = { family: String ->
            """<div class="receipt tape-80mm"><p style="font-family: $family">$text</p></div>"""
        }
        val bundled = documents.htmlToImage(page("'Inter'"))
        val named = documents.htmlToImage(page("'Noto Sans'"))
        // Родовое имя без кавычек — системный шрифт устройства: правила @font-face его не трогают.
        val system = documents.htmlToImage(page("sans-serif"))
        File(out, "font-bundled.png").writeBytes(bundled)
        File(out, "font-system.png").writeBytes(system)

        assertContentEquals(pixels(named), pixels(bundled), "Inter and Noto Sans are the same bundled font")
        assertTrue(!pixels(system).contentEquals(pixels(bundled)), "the bundled font is not the system sans-serif")
    }

    @Test
    fun renderingOnTheMainThreadIsRefused() {
        var failure: Throwable? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            failure = runCatching { documents.htmlToImage(fixture("kazakh-receipt.html")) }.exceptionOrNull()
        }
        assertFailsWith<IllegalStateException> { throw checkNotNull(failure) }
    }

    private fun pixels(png: ByteArray): IntArray {
        val image = checkNotNull(BitmapFactory.decodeByteArray(png, 0, png.size))
        val pixels = IntArray(image.width * image.height)
        image.getPixels(pixels, 0, image.width, 0, 0, image.width, image.height)
        return pixels
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/io/github/texport/superkassa/embedded/document/$name")) {
            "Fixture $name is missing"
        }.use { String(it.readBytes()) }

    /** Внизу нет длинного пустого хвоста: лента кончается там, где кончается форма. */
    private fun assertInkedToTheBottom(image: Bitmap, slackPx: Int) {
        val lastInked = (image.height - 1 downTo 0).firstOrNull { y ->
            (0 until image.width).any { x -> drawn(image, x, y) }
        }
        checkNotNull(lastInked) { "the image is blank" }
        assertTrue(image.height - lastInked <= slackPx, "blank tail ${image.height - lastInked}px of ${image.height}px")
    }

    /** Не бумага: как на JVM, светлые разделители формы тоже считаются нарисованным. */
    private fun drawn(image: Bitmap, x: Int, y: Int): Boolean {
        val color = image.getPixel(x, y)
        return Color.red(color) + Color.green(color) + Color.blue(color) < PAPER_SUM
    }

    private fun tapePx(mm: Double, scale: Double): Int = (mm * PrintForm.PX_PER_MM * scale).roundToInt()

    private companion object {
        const val TAPE_80_MM = 80.0
        const val TAPE_58_MM = 58.0
        const val SCALE = 3.0
        const val POINTS_PER_MM = 72 / 25.4
        const val BOTTOM_SLACK_MM = 5.0
        const val LONG_LINES = 400
        const val LONG_RATIO = 10
        const val ESC_POS_TAPE_MM = 80
        const val ESC: Byte = 0x1B
        const val GS: Byte = 0x1D
        const val PAPER_SUM = 3 * 250
    }
}
