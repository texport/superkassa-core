package io.github.texport.superkassa.coredatabase

import io.github.texport.superkassa.core.domain.api.model.common.CounterKeyFormats
import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.common.format
import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdServiceInfo
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftStatus
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.usecase.shift.RecalculateShiftCountersUseCase
import io.github.texport.superkassa.coredatabase.RoomFile.Companion.receipt
import io.github.texport.superkassa.coredatabase.api.StorageSnapshot
import io.github.texport.superkassa.coredatabase.api.StoredDocument
import io.github.texport.superkassa.coredatabase.api.restoreRoomStorage
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Смена, начатая на узле и продолженная кассой на Room: документы,
 * перенесённые из узла, и записанные кассой после переноса читаются
 * одинаково, и пересчёт смены складывает их в одних единицах.
 */
class MixedLedgerTest {
    private val room = RoomFile()

    @AfterTest
    fun cleanUp() = room.delete()

    @Test
    fun `документы узла и свои читаются одинаково - тип, единицы, чек и реквизиты кассы`() {
        restoreRoomStorage(room.builder(), nodeShift())

        room.session { storage ->
            continueShift(storage)
            val sales = listOf(storage.findFiscalDocumentById(NODE_SALE), storage.findFiscalDocumentById(OWN_SALE))
            assertEquals(List(2) { "SALE" to SALE_TIYN }, sales.map { it?.docType to it?.totalAmount })
            val deposits = listOf(storage.findFiscalDocumentById(NODE_IN), storage.findFiscalDocumentById(OWN_IN))
            assertEquals(List(2) { "CASH_IN" to DEPOSIT_TIYN }, deposits.map { it?.docType to it?.totalAmount })
            val receipts = listOf(NODE_SALE, OWN_SALE).map { storage.findFiscalDocumentWithReceiptPayload(it)?.second?.total }
            assertEquals(List(2) { Money.fromTiyn(SALE_TIYN) }, receipts)
            val requisites = storage.listFiscalDocumentsByShift(KKM, SHIFT, 100, 0).map { it.taxpayerBin to it.registrationNumber }
            assertEquals(setOf(BIN to KGD_NUMBER), requisites.toSet())
        }
    }

    @Test
    fun `пересчёт смены складывает документы узла и свои в тиынах`() {
        restoreRoomStorage(room.builder(), nodeShift())

        room.session { storage ->
            continueShift(storage)
            val shift = assertNotNull(storage.findShiftById(SHIFT))

            val counters = RecalculateShiftCountersUseCase(storage).rebuildShiftCounters(KKM, shift)

            assertEquals(2 * (SALE_TIYN + DEPOSIT_TIYN), counters[CounterKeyFormats.CASH_SUM])
            assertEquals(2 * DEPOSIT_TIYN, counters[CounterKeyFormats.MONEY_PLACEMENT_SUM.format("MONEY_PLACEMENT_DEPOSIT")])
            assertEquals(2 * SALE_TIYN, counters[CounterKeyFormats.OPERATION_SUM.format("OPERATION_SELL")])
        }
    }

    /** После переноса касса продаёт и вносит сама — через порт, как ядро. */
    private fun continueShift(storage: StoragePort) {
        val sale = receipt(KKM, ReceiptOperationType.SELL, SALE_TIYN, "own-sale").toReceiptRequest()
        storage.saveReceipt(sale, OWN_SALE, SHIFT, createdAt = 40)
        storage.saveCashOperation(KKM, "CASH_IN", Money.fromTiyn(DEPOSIT_TIYN), OWN_IN, SHIFT, createdAt = 41)
        listOf(OWN_SALE, OWN_IN).forEach { storage.updateReceiptStatus(it, "FP-$it", null, "SENT", null, 42, false) }
    }

    /** Смена, какой её переносит узел: внесение и продажа приняты ОФД, итоги в тиынах, тип — `SALE`. */
    private fun nodeShift() = StorageSnapshot(
        kkms = listOf(
            KkmInfo(
                id = KKM, createdAt = 1, updatedAt = 1, mode = "REGISTRATION", state = "ACTIVE",
                registrationNumber = KGD_NUMBER, systemId = "4100", ofdServiceInfo = SERVICE
            )
        ),
        users = emptyList(),
        shifts = listOf(ShiftInfo(SHIFT, KKM, 2, ShiftStatus.OPEN, openedAt = 30)),
        documents = listOf(
            StoredDocument(nodeDocument(NODE_IN, "CASH_IN", DEPOSIT_TIYN, 31), receipt = null),
            StoredDocument(nodeDocument(NODE_SALE, "SALE", SALE_TIYN, 32), receipt(KKM, ReceiptOperationType.SELL, SALE_TIYN, "k-sale"))
        ),
        counters = emptyList(),
        queue = emptyList(),
        idempotencyKeys = emptyList()
    )

    private fun nodeDocument(id: String, type: String, tiyn: Long, time: Long) = FiscalDocumentSnapshot(
        id = id, cashboxId = KKM, shiftId = SHIFT, docType = type, docNo = 2000 + time, shiftNo = 2, createdAt = time,
        totalAmount = tiyn, currency = "KZT", fiscalSign = "FP$time", autonomousSign = null, isAutonomous = false,
        ofdStatus = "SENT", deliveredAt = time + 1
    )

    private companion object {
        const val KKM = "kkm-1"
        const val SHIFT = "shift-2"
        const val KGD_NUMBER = "600300012345"
        const val BIN = "123456789012"
        const val NODE_SALE = "d-sale"
        const val NODE_IN = "d-in"
        const val OWN_SALE = "r-sale"
        const val OWN_IN = "r-in"

        /** 1 500 тенге 50 тиын. */
        const val SALE_TIYN = 150_050L

        /** 10 000 тенге. */
        const val DEPOSIT_TIYN = 1_000_000L

        val SERVICE = OfdServiceInfo("ТОО Дала", "Алматы", "Алматы", BIN, "47111", 43_250_000, 76_900_000, "MANUAL")
    }
}
