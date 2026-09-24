package io.github.texport.superkassa.testing.api.kassa

import io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.testing.api.kassa.BenchDirectory.Companion.ADMIN_PIN
import io.github.texport.superkassa.testing.api.kassa.BenchDirectory.Companion.NOT_PAYER
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * Отказ БФД в ответе на чек, деньги и отчёты: каждый язык в своём поле,
 * причина и что делать — из таблицы кодов, код БФД — отдельным полем.
 *
 * Прежде в каждое поле уходила строка обмена целиком: три языка сразу
 * и английский текст сетевой ошибки.
 */
class BfdRefusalWordsTest {
    private val directory = BenchDirectory()
    private val bench = directory.open()
    private val kassa = bench.registerKassa(NOT_PAYER).also { it.openShift() }
    private val refusal = TrilingualMessageResponse.from(CoreStrings.bfdRefusal(INCORRECT_REQUEST_DATA))

    @AfterTest
    fun tearDown() {
        bench.close()
        directory.close()
    }

    @Test
    fun `отказ чеку назван словами таблицы и кодом БФД`() {
        val sale = kassa.rejectedSale(INCORRECT_REQUEST_DATA)

        assertRefused(sale.deliveryStatus, sale.deliveryError, sale.bfdResultCode)
    }

    @Test
    fun `отказ внесению назван словами таблицы и кодом БФД`() {
        bench.bfd.refuseNext(INCORRECT_REQUEST_DATA)

        val cashIn = kassa.cashIn()

        assertRefused(cashIn.deliveryStatus, cashIn.deliveryError, cashIn.bfdResultCode)
    }

    @Test
    fun `отказ X-отчёту назван словами таблицы и кодом БФД`() {
        bench.bfd.refuseNext(INCORRECT_REQUEST_DATA)

        val report = kassa.xReport()

        assertRefused(report.deliveryStatus, report.deliveryError, report.bfdResultCode)
    }

    @Test
    fun `отказ Z-отчёту назван словами таблицы и кодом БФД`() {
        bench.bfd.refuseNext(INCORRECT_REQUEST_DATA)

        val report = kassa.closeShift()

        assertRefused(report.deliveryStatus, report.deliveryError, report.bfdResultCode)
    }

    @Test
    fun `чек без связи с БФД назван словами без текста сетевой ошибки`() {
        val sale = kassa.offlineSale()

        assertEquals(DeliveryStatus.OFFLINE_QUEUED, sale.deliveryStatus)
        assertEquals(TrilingualMessageResponse.from(CoreStrings.bfdNoAnswer()), sale.deliveryError)
        assertOwnLanguages(sale.deliveryError)
        assertNull(sale.bfdResultCode)
    }

    @Test
    fun `задача очереди, отвергнутая БФД, хранит код БФД рядом со словами`() {
        kassa.offlineSale()
        bench.bfd.refuseNext(INCORRECT_REQUEST_DATA)

        kassa.resendQueue()

        val item = bench.superkassa.queue.listQueue(kassa.kkmId, ADMIN_PIN).single()
        assertEquals("REJECTED" to INCORRECT_REQUEST_DATA, item.status to item.bfdResultCode)
    }

    private fun assertRefused(status: DeliveryStatus, error: TrilingualMessageResponse?, code: Int?) {
        assertEquals(DeliveryStatus.ONLINE_ERROR, status)
        assertEquals(refusal, error)
        assertOwnLanguages(error)
        assertEquals(INCORRECT_REQUEST_DATA, code)
    }

    /** Ни в одном поле нет чужого языка и английского технического текста. */
    private fun assertOwnLanguages(error: TrilingualMessageResponse?) {
        val words = requireNotNull(error)
        assertFalse(LATIN_WORD.containsMatchIn(words.ru), words.ru)
        assertFalse(KAZAKH_LETTER.containsMatchIn(words.ru), words.ru)
        assertFalse(LATIN_WORD.containsMatchIn(words.kk), words.kk)
        assertFalse(CYRILLIC.containsMatchIn(words.en), words.en)
        assertFalse(TECHNICAL.containsMatchIn(words.en), words.en)
    }

    private companion object {
        /** RESULT_TYPE_INCORRECT_REQUEST_DATA: БФД не принял данные документа. */
        const val INCORRECT_REQUEST_DATA = 13

        /** Латинское слово: одиночные «X» и «Z» — названия отчётов, они есть и в русском тексте. */
        val LATIN_WORD = Regex("[A-Za-z]{2,}")
        val KAZAKH_LETTER = Regex("[әғқңөұүһіӘҒҚҢӨҰҮҺІ]")
        val CYRILLIC = Regex("[А-Яа-яЁё]")
        val TECHNICAL = Regex("RU:|KK:|EN:|Exception|request failed|timeout|code", RegexOption.IGNORE_CASE)
    }
}
