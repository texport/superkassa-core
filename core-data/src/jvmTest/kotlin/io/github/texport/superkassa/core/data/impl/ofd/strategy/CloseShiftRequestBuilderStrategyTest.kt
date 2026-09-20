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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Досылка закрытия смены после обрыва связи.
 *
 * Запрос собирался по открытой смене, а не по смене самого документа.
 * Касса, успевшая открыть следующую смену, отправляла ОФД Z-отчёт с её
 * номером и её счётчиками: в ОФД появлялся второй отчёт на новую смену,
 * а прежняя не закрывалась вовсе.
 */
class CloseShiftRequestBuilderStrategyTest {

    private val storage = mockk<StoragePort>(relaxed = true)
    private val counters = mockk<RecalculateShiftCountersUseCase>(relaxed = true)
    private val strategy = CloseShiftRequestBuilderStrategy(storage, counters)

    @Test
    fun `досланное закрытие берёт номер своей смены, а не открытой`() {
        every { storage.findFiscalDocumentById(DOCUMENT) } returns document(CLOSED_SHIFT)
        every { storage.findShiftById(CLOSED_SHIFT) } returns shift(CLOSED_SHIFT, 4, ShiftStatus.CLOSED)
        every { storage.findOpenShift(KKM) } returns shift(OPEN_SHIFT, 5, ShiftStatus.OPEN)
        every { counters.execute(KKM, any()) } returns mapOf("operation.OPERATION_SELL.sum" to 10_000L)

        val request = strategy.build(command(), config())

        val closeShift = request?.get("payload")?.jsonObject?.get("closeShift")?.jsonObject
        assertEquals("4", closeShift?.get("frShiftNumber")?.jsonPrimitive?.content)
    }

    private fun document(shiftId: String) = FiscalDocumentSnapshot(
        id = DOCUMENT,
        cashboxId = KKM,
        shiftId = shiftId,
        docType = "Z_REPORT",
        shiftNo = 4,
        createdAt = 1_789_000_000_000L,
        docNo = 9,
        totalAmount = 0,
        currency = "KZT",
        fiscalSign = null,
        autonomousSign = "1789000000000",
        ofdStatus = "PENDING",
        deliveredAt = null,
        isAutonomous = true
    )

    private fun shift(id: String, number: Long, status: ShiftStatus) = ShiftInfo(
        id = id,
        kkmId = KKM,
        shiftNo = number,
        status = status,
        openedAt = 1_788_999_000_000L,
        closedAt = if (status == ShiftStatus.CLOSED) 1_789_000_500_000L else null
    )

    private fun command() = OfdCommandRequest(
        kkmId = KKM,
        commandType = OfdCommandType.CLOSE_SHIFT,
        payloadRef = DOCUMENT,
        ofdProviderId = "bfd",
        ofdEnvironmentId = "DEV",
        deviceId = 5_000_021L,
        token = 777L,
        reqNum = 11,
        registrationNumber = "260940000021",
        factoryNumber = "SK-2026-0002",
        ofdSystemId = "5000021",
        serviceInfo = serviceInfo()
    )

    private fun serviceInfo() = OfdServiceInfo(
        orgTitle = "ИП ИВАНОВ СЕРГЕЙ НИКОЛАЕВИЧ",
        orgAddress = "Алматы, Медеуский, Достык, 10",
        orgAddressKz = "Алматы, Медеу, Достық, 10",
        orgInn = "920313351246",
        orgOkved = "47111",
        geoLatitude = 432_223,
        geoLongitude = 769_580,
        geoSource = "UNKNOWN"
    )

    private fun config() = OfdConfig(protocolVersion = "203")

    private companion object {
        const val KKM = "4166498c-d0c1-406e-863d-20458dfd3040"
        const val CLOSED_SHIFT = "shift-4"
        const val OPEN_SHIFT = "shift-5"
        const val DOCUMENT = "document-z"
    }
}
