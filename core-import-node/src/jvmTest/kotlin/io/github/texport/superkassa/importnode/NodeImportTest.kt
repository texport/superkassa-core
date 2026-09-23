package io.github.texport.superkassa.importnode

import androidx.room.Room
import io.github.texport.superkassa.core.data.impl.util.Sha256
import io.github.texport.superkassa.core.domain.api.model.common.CounterScopes
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptBranding
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptLanguage
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType
import io.github.texport.superkassa.core.domain.api.model.receipt.TicketAd
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.coredatabase.api.openRoomStorage
import io.github.texport.superkassa.coredatabase.impl.db.SuperkassaAppDatabase
import io.github.texport.superkassa.importnode.NodeScenario.KKM
import io.github.texport.superkassa.importnode.api.NodeImportResult
import io.github.texport.superkassa.importnode.api.importNodeData
import java.io.File
import java.util.Base64
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Касса с открытой сменой переносится с узла так, что продолжает ту же
 * смену с теми же номерами, суммами, очередью и кассирами.
 */
class NodeImportTest {
    private val root: File = createTempDirectory("node-import-").toFile()
    private val node = NodeFixture(File(root, "node")).also(NodeScenario::populate).also(NodeFixture::close)
    private val target = File(root, "kassa")

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    @Test
    fun `отчёт называет перенесённое по кассе`() {
        val report = assertIs<NodeImportResult.Imported>(importNodeData(node.home.path, target.path)).report
        val kkm = report.kkms.single()
        assertEquals(10, kkm.documents)
        assertEquals(2002L, kkm.lastDocumentNumber)
        assertEquals(8L, kkm.lastPrintedDocumentNumber)
        assertEquals(2L, kkm.openShiftNumber)
        assertEquals(1_900_050L, kkm.openShiftCashTiyn)
        assertEquals(mapOf("SENT" to 1, "PENDING" to 2, "REJECTED" to 1), kkm.queueByStatus)
        assertEquals(2, kkm.users)
        assertEquals(4, kkm.idempotencyKeys)
        assertEquals(true, kkm.xReportMatches)
        assertTrue(report.settingsTransferred)
    }

    @Test
    fun `касса, смена и документы ложатся как у узла`() = importedStorage { storage ->
        val kkm = checkNotNull(storage.findKkm(KKM))
        assertEquals(NodeScenario.KGD_NUMBER, kkm.registrationNumber)
        assertEquals(Base64.getEncoder().encodeToString(NodeScenario.TOKEN), kkm.tokenEncryptedBase64)
        assertEquals(NodeScenario.BIN, kkm.ofdServiceInfo?.orgInn)
        assertEquals(NodeScenario.OPEN_SHIFT, storage.findOpenShift(KKM)?.id)
        val sale = checkNotNull(storage.findFiscalDocumentById("d-sale"))
        assertEquals("SALE", sale.docType)
        assertEquals(NodeScenario.SALE_TIYN, sale.totalAmount)
        assertEquals(2001L, sale.docNo)
        assertEquals(NodeScenario.BIN, sale.taxpayerBin)
        val rejected = checkNotNull(storage.findFiscalDocumentById("d-rejected"))
        assertNull(rejected.docNo)
        assertEquals("FAILED" to 15, rejected.ofdStatus to rejected.ofdErrorCode)
        val receipt = checkNotNull(storage.findFiscalDocumentWithReceiptPayload("d-return")).second
        assertEquals(ReceiptOperationType.SELL_RETURN, receipt.operation)
        assertEquals(NodeScenario.receipt(ReceiptOperationType.SELL_RETURN, 50_000L, "k-ret").items, receipt.items)
    }

    @Test
    fun `своё оформление чека переходит к кассе`() = importedStorage { storage ->
        val expected = ReceiptBranding(
            language = ReceiptLanguage.KK,
            paperWidthMm = 58,
            themeColor = "emerald",
            headerMsg = "Дала дүкені",
            footerMsg = "Рахмет!",
            ofdTicketAds = listOf(TicketAd("TICKET_AD_OFD", 3, "ОФД хабарламасы")),
            printOfdTicketAds = false
        )
        assertEquals(expected, storage.findKkm(KKM)?.branding)
    }

    @Test
    fun `счётчики, очередь, кассиры и ключи повтора работают как у узла`() = importedStorage { storage ->
        assertEquals(17L, storage.loadCounters(KKM, CounterScopes.GLOBAL)["ofd.req_num"])
        assertEquals(1_000L, storage.loadCounters(KKM, CounterScopes.SHIFT, NodeScenario.OPEN_SHIFT)["start_shift_cash.sum"])
        val pending = storage.getQueueTasksByStatus(KKM, "OFFLINE", setOf("PENDING")).map { it.payloadRef }.toSet()
        assertEquals(setOf("d-out", "d-x"), pending)
        assertEquals("u-cashier", storage.findUserByPin(KKM, Sha256.hash(NodeScenario.CASHIER_PIN))?.id)
        assertEquals("u-admin", storage.findUserByPin(KKM, Sha256.hash(NodeScenario.ADMIN_PIN))?.id)
        assertEquals("d-sale", storage.findIdempotencyResponse(KKM, "k-sale"))
        assertFalse(storage.insertIdempotency(KKM, "k-sale", "CREATE_RECEIPT"), "retry must not issue a second receipt")
        assertNull(storage.findIdempotencyResponse(KKM, "k-flight"))
    }

    @Test
    fun `база узла не меняется ни единым файлом`() {
        val before = node.fingerprint()
        importNodeData(node.home.path, target.path)
        assertEquals(before, node.fingerprint())
    }

    @Test
    fun `повторный перенос ничего не делает`() {
        importNodeData(node.home.path, target.path)
        val database = File(target, "superkassa.db")
        val stamp = database.readBytes().contentHashCode() to database.lastModified()
        assertEquals(NodeImportResult.AlreadyImported, importNodeData(node.home.path, target.path))
        assertEquals(stamp, database.readBytes().contentHashCode() to database.lastModified())
        assertEquals(setOf("superkassa.db", "core-settings.json", "node-import.done", "superkassa.lock"), target.list()!!.toSet())
    }

    @Test
    fun `настройки узла переходят к кассе, а база — своя`() {
        importNodeData(node.home.path, target.path)
        val settings = File(target, "core-settings.json").readText()
        assertTrue("\"ofdProtocolVersion\": \"204\"" in settings)
        assertTrue("jdbc:sqlite:superkassa.db" in settings)
    }

    private fun importedStorage(check: (StoragePort) -> Unit) {
        importNodeData(node.home.path, target.path)
        val storage = openRoomStorage(Room.databaseBuilder<SuperkassaAppDatabase>(name = File(target, "superkassa.db").path))
        try {
            check(storage.storagePort)
        } finally {
            storage.close()
        }
    }
}
