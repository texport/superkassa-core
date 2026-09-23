package io.github.texport.superkassa.testing.api.bfd

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmListParams
import io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus
import io.github.texport.superkassa.embedded.api.SuperkassaPlatform
import io.github.texport.superkassa.testing.api.kassa.BenchDirectory
import io.github.texport.superkassa.testing.api.kassa.BenchDirectory.Companion.ADMIN_PIN
import io.github.texport.superkassa.testing.api.kassa.BenchDirectory.Companion.NOT_PAYER
import io.github.texport.superkassa.testing.api.kassa.createTestSuperkassa
import kz.kazakhtelecom.proto.v203.CommandTypeEnum
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Сбои связи, заказанные у БФД, на кассе встраиваемой сборки. */
class FakeBfdFaultsTest {
    private val directory = BenchDirectory()
    private val bench = directory.open()
    private val kassa = bench.registerKassa(NOT_PAYER).also { it.openShift() }
    private val bfd = bench.bfd

    @AfterTest
    fun tearDown() {
        bench.close()
        directory.close()
    }

    @Test
    fun `без связи чеки копятся в очереди и досылаются после её восстановления`() {
        bfd.disconnect()

        val offline = listOf(kassa.sell(), kassa.sell())
        kassa.resendQueue()

        assertEquals(listOf(DeliveryStatus.OFFLINE_QUEUED, DeliveryStatus.OFFLINE_QUEUED), offline.map { it.deliveryStatus })
        assertEquals(2, bfd.countedTickets().size)
        assertEquals(0, kassa.info().offlineQueueCount)
    }

    @Test
    fun `потерянный ответ - чек учтён, касса досылает его, и БФД узнаёт повтор`() {
        bfd.loseNextAnswer()

        val lost = kassa.sell()
        kassa.resendQueue()

        assertEquals(DeliveryStatus.OFFLINE_QUEUED, lost.deliveryStatus)
        assertEquals(1, bfd.countedTickets().size)
        assertEquals(2, bfd.exchanges.count { it.request.ticket != null })
    }

    @Test
    fun `задержанный ответ, так и не дошедший, ведёт себя как потерянный`() {
        bfd.holdNextAnswerThenLose(maxMillis = HOLD_MILLIS)

        val held = kassa.sell()
        kassa.resendQueue()

        assertEquals(DeliveryStatus.OFFLINE_QUEUED, held.deliveryStatus)
        assertEquals(1, bfd.countedTickets().size)
    }

    @Test
    fun `отказ командам типа держится, пока его не сняли`() {
        bfd.reject(CommandTypeEnum.COMMAND_TICKET, REFUSAL)
        val refused = listOf(kassa.sell(), kassa.sell())

        bfd.acceptAll()

        assertEquals(listOf(DeliveryStatus.ONLINE_ERROR, DeliveryStatus.ONLINE_ERROR), refused.map { it.deliveryStatus })
        assertEquals(DeliveryStatus.ONLINE_OK, kassa.sell().deliveryStatus)
    }

    @Test
    fun `касса приходит с токеном, выданным ей последним`() {
        kassa.sell()
        val issued = bfd.issuedToken(kassa.systemId)

        kassa.sell()

        assertEquals(issued, bfd.exchanges.last().token)
    }

    @Test
    fun `изъятие при закрытии смены ложится в закрываемую смену БФД`() {
        kassa.api.enterProgramming(kassa.kkmId, ADMIN_PIN)
        kassa.api.updateKkmSettings(kassa.kkmId, ADMIN_PIN, autoCloseShift = false, autoCashout = true)
        kassa.api.exitProgramming(kassa.kkmId, ADMIN_PIN)
        kassa.cashIn("2000.00")
        kassa.sell("500.00", "1")

        kassa.closeShift()

        assertEquals(mapOf(1 to 250_000L), bfd.withdrawnByShift(kassa.systemId))
        assertEquals(1, bfd.moneyPlacements().size)
    }

    @Test
    fun `сборка проверки поднимается на пустом каталоге с часами и настройками по умолчанию`() {
        val other = BenchDirectory()

        val kkms = createTestSuperkassa(SuperkassaPlatform(other.dir.absolutePath), FakeBfd()).use {
            it.api.listKkms(KkmListParams()).total
        }

        other.close()
        assertEquals(0, kkms)
    }

    private companion object {
        const val HOLD_MILLIS = 50L
        const val REFUSAL = 13
    }
}
