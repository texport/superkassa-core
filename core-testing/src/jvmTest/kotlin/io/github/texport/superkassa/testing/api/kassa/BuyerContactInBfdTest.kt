package io.github.texport.superkassa.testing.api.kassa

import io.github.texport.superkassa.core.presentation.api.model.receipt.CustomerContactRequest
import io.github.texport.superkassa.testing.api.bfd.FakeBfd
import io.github.texport.superkassa.testing.api.clock.MovableClock
import io.github.texport.superkassa.testing.api.kassa.BenchDirectory.Companion.NOT_PAYER
import kz.kazakhtelecom.proto.v203.TicketRequest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Контакт покупателя из чека доходит до БФД в `TicketRequest.extension_options`:
 * телефон в `customer_phone`, почта в `customer_email` — по CPCR 2.0.3 и 2.0.4,
 * онлайн и после досылки из очереди. Для Telegram поля в протоколе нет.
 */
class BuyerContactInBfdTest {
    private val directory = BenchDirectory()
    private var bench: TestBench? = null

    @AfterTest
    fun tearDown() {
        bench?.close()
        directory.close()
    }

    @Test
    fun `чек по CPCR 2_0_3 несёт в БФД телефон и почту покупателя`() {
        val kassa = open203().also { it.openShift() }

        kassa.sell(buyer = CONTACT)

        assertContact(kassa.bfd.countedTickets().single())
    }

    @Test
    fun `чек по CPCR 2_0_4 несёт в БФД телефон и почту покупателя`() {
        val bfd = FakeBfd()
        val kassa = directory.reopen(bfd, MovableClock(), appSuperkassaConfig()).also { bench = it }
            .registerKassa(NOT_PAYER.copy(ofdProvider = KassaSetup.BFD)).also { it.openShift() }

        kassa.sell(buyer = CONTACT)

        assertEquals(setOf(V204), bfd.exchanges.map { it.version }.toSet())
        assertContact(bfd.countedTickets().single())
    }

    @Test
    fun `досланный из очереди чек несёт контакт покупателя`() {
        val kassa = open203().also { it.openShift() }
        kassa.offlineSale(buyer = CONTACT)

        kassa.resendQueue()

        assertContact(kassa.bfd.countedTickets().single())
    }

    @Test
    fun `чек без контакта покупателя расширений не несёт`() {
        val kassa = open203().also { it.openShift() }

        kassa.sell()

        assertNull(kassa.bfd.countedTickets().single().extension_options)
    }

    private fun open203(): ReadyKassa = directory.open().also { bench = it }.registerKassa(NOT_PAYER)

    private fun assertContact(ticket: TicketRequest) {
        val options = ticket.extension_options
        assertEquals(CONTACT_PHONE to CONTACT_EMAIL, options?.customer_phone to options?.customer_email)
        assertEquals(emptyList(), options?.auxiliary.orEmpty(), "Telegram has no field in CPCR")
    }

    private companion object {
        const val CONTACT_PHONE = "+77017654321"
        const val CONTACT_EMAIL = "buyer@mail.kz"
        const val V204 = 204
        val CONTACT = CustomerContactRequest(phone = CONTACT_PHONE, email = CONTACT_EMAIL, telegram = "123456789")
    }
}
