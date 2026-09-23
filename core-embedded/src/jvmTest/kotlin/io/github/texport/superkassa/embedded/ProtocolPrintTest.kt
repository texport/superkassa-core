package io.github.texport.superkassa.embedded

import io.github.texport.superkassa.core.domain.api.exception.ForbiddenException
import io.github.texport.superkassa.core.domain.api.exception.NotFoundException
import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptLayoutType
import io.github.texport.superkassa.embedded.TestCashRegister.CASHIER_PIN
import io.github.texport.superkassa.embedded.TestCashRegister.KKM_ID
import io.github.texport.superkassa.embedded.impl.EmbeddedSuperkassa
import org.apache.pdfbox.Loader
import java.io.File
import javax.imageio.ImageIO
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Печать документа по пакету протокола без узла: кабинет присылает пакет,
 * приложение рисует его рисовальщиком ядра и отдаёт HTML, PDF и PNG.
 */
class ProtocolPrintTest {
    private val dir: File = createTempDirectory("kassa-packet-").toFile()
    private val out = File("build/documents").apply { mkdirs() }

    @AfterTest
    fun cleanUp() {
        dir.deleteRecursively()
    }

    private fun withKassa(block: (EmbeddedSuperkassa) -> Unit) = TestCashRegister.open(dir).use { kassa ->
        TestCashRegister.registerKkm(kassa)
        block(kassa)
    }

    @Test
    fun `чек рисуется реквизитами документа, а не рисующей кассы`() = withKassa { kassa ->
        val html = kassa.print.getProtocolPrintHtml(KKM_ID, CASHIER_PIN, ProtocolPackets.receipt)

        assertContains(html, ProtocolPackets.REGISTRATION_NUMBER)
        assertContains(html, ProtocolPackets.TAXPAYER)
        assertContains(html, "Хлеб")
        assertContains(html, ProtocolPackets.FISCAL_SIGN)
        assertFalse(html.contains("010101012345"), "the drawing register's number must not appear")
    }

    @Test
    fun `отчёты и кассовый ордер рисуются тем же рисовальщиком`() = withKassa { kassa ->
        listOf(ProtocolPackets.report("REPORT_X"), ProtocolPackets.report("REPORT_Z")).forEach { packet ->
            assertContains(kassa.print.getProtocolPrintHtml(KKM_ID, CASHIER_PIN, packet), ProtocolPackets.REGISTRATION_NUMBER)
        }
        val order = kassa.print.getProtocolPrintHtml(KKM_ID, CASHIER_PIN, ProtocolPackets.placement)
        assertContains(order, "1 000,50 ₸")
    }

    @Test
    fun `ширина ленты берётся из запроса`() = withKassa { kassa ->
        val narrow = kassa.print.getProtocolPrintHtml(KKM_ID, CASHIER_PIN, ProtocolPackets.receipt, ReceiptLayoutType.TAPE_58MM)

        assertContains(narrow, "tape-58mm")
    }

    @Test
    fun `PDF и PNG рисуются конвертером приложения без браузера`() = withKassa { kassa ->
        val pdf = kassa.print.getProtocolPrintPdf(KKM_ID, CASHIER_PIN, ProtocolPackets.receipt)
        val png = kassa.print.getProtocolPrintPng(KKM_ID, CASHIER_PIN, ProtocolPackets.receipt)
        File(out, "protocol-receipt.pdf").writeBytes(pdf)
        File(out, "protocol-receipt.png").writeBytes(png)

        Loader.loadPDF(pdf).use { assertEquals(1, it.numberOfPages) }
        assertTrue(ImageIO.read(png.inputStream()).height > 0)
    }

    @Test
    fun `неизвестная касса, чужой пин, нечитаемый пакет и команда без документа отвечают отказом`() = withKassa { kassa ->
        assertFailsWith<NotFoundException> { kassa.print.getProtocolPrintHtml("другая", CASHIER_PIN, ProtocolPackets.receipt) }
        assertFailsWith<ForbiddenException> { kassa.print.getProtocolPrintHtml(KKM_ID, "9999", ProtocolPackets.receipt) }
        assertFailsWith<ValidationException> { kassa.print.getProtocolPrintHtml(KKM_ID, CASHIER_PIN, "не JSON вовсе") }
        assertFailsWith<ValidationException> {
            kassa.print.getProtocolPrintHtml(KKM_ID, CASHIER_PIN, """{"request": {"command": "COMMAND_INFO"}}""")
        }
    }
}
