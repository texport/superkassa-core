package io.github.texport.superkassa.core.data.impl.ofd.strategy

import io.github.texport.superkassa.core.data.impl.ofd.OfdConfig
import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandRequest
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdServiceInfo
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftStatus
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.usecase.shift.RecalculateShiftCountersUseCase
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Досылка X-отчёта, снятого при оборванной связи.
 *
 * Отчёт собирался из открытой смены, поэтому после её закрытия запрос
 * не собирался вовсе: задача уходила в отбраковку, документ навсегда
 * оставался неотправленным, а очередь при этом показывала ноль. Смена
 * берётся из самого документа, и закрытие смены досылке больше не мешает.
 */
class ReportRequestBuilderStrategyTest {

    private val storage = mockk<StoragePort>(relaxed = true)
    private val counters = mockk<RecalculateShiftCountersUseCase>(relaxed = true)
    private val strategy = ReportRequestBuilderStrategy(storage, counters)

    @Test
    fun `отчёт закрытой смены собирается по своей смене`() {
        every { storage.findFiscalDocumentById(DOCUMENT) } returns document(SHIFT)
        every { storage.findShiftById(SHIFT) } returns shift(ShiftStatus.CLOSED)
        // Открытой смены нет: её закрыли, пока связи не было.
        every { storage.findOpenShift(KKM) } returns null
        every { counters.execute(KKM, any()) } returns mapOf("operation.OPERATION_SELL.sum" to 30_000L)

        assertNotNull(strategy.build(command(), config()))
    }

    @Test
    fun `отчёт без своей смены и без открытой не собирается`() {
        every { storage.findFiscalDocumentById(DOCUMENT) } returns document(shiftId = "0")
        every { storage.findOpenShift(KKM) } returns null

        assertNull(strategy.build(command(), config()))
    }

    private fun document(shiftId: String) = FiscalDocumentSnapshot(
        id = DOCUMENT,
        cashboxId = KKM,
        shiftId = shiftId,
        docType = "X_REPORT",
        shiftNo = 1,
        createdAt = 1_789_000_000_000L,
        docNo = 7,
        totalAmount = 0,
        currency = "KZT",
        fiscalSign = null,
        autonomousSign = "1789000000000",
        ofdStatus = "PENDING",
        deliveredAt = null,
        isAutonomous = true
    )

    private fun shift(status: ShiftStatus) = ShiftInfo(
        id = SHIFT,
        kkmId = KKM,
        shiftNo = 1,
        status = status,
        openedAt = 1_788_999_000_000L,
        closedAt = 1_789_000_500_000L
    )

    private fun command() = OfdCommandRequest(
        kkmId = KKM,
        commandType = OfdCommandType.REPORT,
        payloadRef = DOCUMENT,
        ofdProviderId = "bfd",
        ofdEnvironmentId = "DEV",
        deviceId = 5_000_021L,
        token = 777L,
        reqNum = 9,
        registrationNumber = "260940000021",
        factoryNumber = "SK-2026-0002",
        ofdSystemId = "5000021",
        serviceInfo = serviceInfo()
    )

    private fun serviceInfo() = OfdServiceInfo(
        orgTitle = "ИП ИВАНОВ СЕРГЕЙ НИКОЛАЕВИЧ",
        orgAddress = "Алматы, Медеуский, Достык, 10",
        orgAddressKz = "Алматы, Медеу, Достық, 10",
        orgIinOrBin = "920313351246",
        orgOked = "47111",
        geoLatitude = 432_223,
        geoLongitude = 769_580,
        geoSource = "UNKNOWN"
    )

    private fun config() = OfdConfig(protocolVersion = "203")

    private companion object {
        const val KKM = "4166498c-d0c1-406e-863d-20458dfd3040"
        const val SHIFT = "shift-1"
        const val DOCUMENT = "document-x"
    }
}
