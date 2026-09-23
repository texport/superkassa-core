package io.github.texport.superkassa.embedded

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptItemRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptPaymentRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptSellRequest
import io.github.texport.superkassa.embedded.TestCashRegister.ADMIN_PIN
import io.github.texport.superkassa.embedded.TestCashRegister.CASHIER_PIN
import io.github.texport.superkassa.embedded.TestCashRegister.KKM_ID
import org.apache.fontbox.ttf.OTFParser
import org.apache.pdfbox.Loader
import org.apache.pdfbox.io.RandomAccessReadBuffer
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Шрифты печатных форм: каждый нужный знак есть в каждом шрифте сборки,
 * и PDF чека с казахским текстом и знаком тенге не содержит пустых глифов.
 */
class FontsTest {
    private val dir: File = createTempDirectory("kassa-fonts-").toFile()

    @AfterTest
    fun cleanUp() {
        dir.deleteRecursively()
    }

    @Test
    fun `в каждом шрифте есть казахский алфавит, кириллица, латиница, цифры, тенге и номер`() {
        FONTS.forEach { file ->
            val stream = checkNotNull(javaClass.getResourceAsStream(FONT_DIR + file)) { "Font $file is missing" }
            val font = stream.use { OTFParser().parse(RandomAccessReadBuffer(it)) }
            val cmap = font.getUnicodeCmapLookup(true)
            val missing = REQUIRED.filter { cmap.getGlyphId(it.code) == 0 }
            assertEquals("", missing, "$file has no glyphs for these characters")
        }
    }

    @Test
    fun `шрифт форм пропорциональный - узкая буква уже широкой`() {
        FONTS.forEach { file ->
            val stream = checkNotNull(javaClass.getResourceAsStream(FONT_DIR + file)) { "Font $file is missing" }
            val font = stream.use { OTFParser().parse(RandomAccessReadBuffer(it)) }
            val cmap = font.getUnicodeCmapLookup(true)
            val width = { c: Char -> font.getAdvanceWidth(cmap.getGlyphId(c.code)) }
            assertTrue(width('і') < width('Ш'), "$file: і ${width('і')}, Ш ${width('Ш')}")
        }
    }

    @Test
    fun `рядом с каждым шрифтом лежит его лицензия`() {
        listOf("NotoSans-OFL.txt").forEach { file ->
            val text = checkNotNull(javaClass.getResourceAsStream(FONT_DIR + file)).use { String(it.readBytes()) }
            assertContains(text, "SIL OPEN FONT LICENSE Version 1.1")
        }
    }

    @Test
    fun `PDF чека с казахским текстом и тенге рисуется без пустых глифов`() {
        TestCashRegister.open(dir).use { kassa ->
            TestCashRegister.registerKkm(kassa)
            kassa.api.openShift(KKM_ID, ADMIN_PIN)
            val receipt = kassa.api.createSellReceipt(KKM_ID, CASHIER_PIN, kazakhSale())

            val pdf = kassa.print.getDocumentPrintPdf(KKM_ID, receipt.documentId, CASHIER_PIN)
            val documents = File("build/documents").apply { mkdirs() }
            File(documents, "kazakh-receipt.pdf").writeBytes(pdf)
            File(documents, "kazakh-receipt.png").writeBytes(kassa.print.getDocumentPrintPng(KKM_ID, receipt.documentId, CASHIER_PIN))

            val drawn = GlyphCheck(pdf)
            assertTrue(drawn.characters > 0, "the receipt draws text")
            assertEquals(emptyList(), drawn.empty, "characters drawn with the empty glyph")
            assertTrue(drawn.fonts.all { it is PDType0Font && "NotoSans" in it.name }, "fonts: ${drawn.fonts.map { it.name }}")
            // Строки ленты переносятся по ширине букв: текст сверяется без переносов.
            val text = drawn.text.replace(Regex("\\s+"), " ")
            assertContains(text, "Нан $KAZAKH Bread №5")
            assertContains(text, "₸")
        }
    }

    /** Текст страницы вместе со знаками, для которых у шрифта не нашлось глифа. */
    private class GlyphCheck(pdf: ByteArray) : PDFTextStripper() {
        val empty = mutableListOf<String>()
        val fonts = mutableSetOf<PDFont>()
        var characters = 0
        val text: String = Loader.loadPDF(pdf).use { getText(it) }

        override fun processTextPosition(text: TextPosition) {
            super.processTextPosition(text)
            characters++
            fonts += text.font
            val font = text.font as? PDType0Font ?: return
            if (text.characterCodes.any { font.codeToGID(it) == 0 }) empty += text.unicode
        }
    }

    private fun kazakhSale() = ReceiptSellRequest(
        idempotencyKey = "kazakh-1",
        items = listOf(ReceiptItemRequest(name = KAZAKH_NAME, price = Decimal.parse("500.00"), quantity = Decimal.parse("1"))),
        payments = listOf(ReceiptPaymentRequest(type = "CASH", sum = Decimal.parse("500.00")))
    )

    private companion object {
        const val FONT_DIR = "/io/github/texport/superkassa/embedded/fonts/"
        val FONTS = listOf("NotoSans-Regular.ttf", "NotoSans-Bold.ttf")
        const val KAZAKH = "ӘәҒғҚқҢңӨөҰұҮүҺһІі"
        const val KAZAKH_NAME = "Нан $KAZAKH Bread №5"
        val REQUIRED: String = KAZAKH + "₸№" + ('А'..'я').joinToString("") + "Ёё" +
            ('A'..'Z').joinToString("") + ('a'..'z').joinToString("") + "0123456789"
    }
}
