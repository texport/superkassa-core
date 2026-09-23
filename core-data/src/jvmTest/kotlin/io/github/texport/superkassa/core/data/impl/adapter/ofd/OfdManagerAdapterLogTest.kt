package io.github.texport.superkassa.core.data.impl.adapter.ofd

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.github.texport.superkassa.core.data.impl.ofd.OfdConfig
import io.github.texport.superkassa.core.data.impl.ofd.OfdProtocolCodec
import io.github.texport.superkassa.core.data.impl.ofd.strategy.OfdRequestBuilderStrategy
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandRequest
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandStatus
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kz.mybrain.network.OfdNetworkClient
import org.slf4j.LoggerFactory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Журнал обмена с БФД: команда, номер запроса, код ответа и длительность.
 *
 * Пакет чека несёт покупателя, товары и суммы, заголовок — токен, ответ —
 * фискальный признак. Ничего из этого в журнал не попадает ни на каком
 * уровне, включая отладочный.
 */
class OfdManagerAdapterLogTest {
    private val journal = ListAppender<ILoggingEvent>().apply { start() }
    private val logger = (LoggerFactory.getLogger(OfdManagerAdapter::class.java) as Logger).apply {
        level = Level.TRACE
        addAppender(journal)
    }
    private val codec: OfdProtocolCodec = mockk()
    private val network: OfdNetworkClient = mockk()
    private val builder: OfdRequestBuilderStrategy = mockk()
    private val adapter = OfdManagerAdapter(OfdConfig("203"), codec, network, listOf(builder), timeoutSeconds = 1L)

    @AfterTest
    fun detach() {
        logger.detachAppender(journal)
        logger.level = null
    }

    @Test
    fun `принятый чек - в журнале команда, номер запроса, код и длительность, без пакета`() {
        answer(code = 0)

        assertEquals(OfdCommandStatus.OK, adapter.send(ticket()).status)

        assertNothingSecret()
        assertTrue(lines().any { "TICKET" in it && "reqNum=10" in it && "code 0" in it && " ms" in it }, lines().toString())
    }

    @Test
    fun `отказ БФД - в журнале код, без текста отказа и пакета`() {
        answer(code = 11)

        assertEquals(OfdCommandStatus.FAILED, adapter.send(ticket()).status)

        assertNothingSecret()
        assertTrue(lines().any { "TICKET" in it && "code 11" in it }, lines().toString())
    }

    @Test
    fun `пакет не собрался - в журнале нет ни пакета, ни разбора ошибки`() {
        every { builder.canHandle(OfdCommandType.TICKET) } returns true
        every { builder.build(any(), any()) } returns PACKET
        every { codec.encode(any()) } throws IllegalStateException("bad field in $PACKET")

        adapter.send(ticket())

        assertNothingSecret()
        assertTrue(lines().any { "TICKET" in it && "IllegalStateException" in it }, lines().toString())
    }

    private fun answer(code: Int) {
        every { builder.canHandle(OfdCommandType.TICKET) } returns true
        every { builder.build(any(), any()) } returns PACKET
        every { codec.encode(any()) } returns byteArrayOf(1)
        coEvery { network.sendAndReceive(any(), any()) } returns Result.success(byteArrayOf(2))
        every { codec.decode(any()) } returns response(code)
    }

    private fun lines(): List<String> = journal.list.map { it.formattedMessage }

    private fun assertNothingSecret() {
        val secrets = listOf(ITEM, CUSTOMER, TOKEN.toString(), ISSUED_TOKEN.toString(), FISCAL_SIGN, REFUSAL)
        val leaked = lines().filter { line -> secrets.any { it in line } }
        assertEquals(emptyList(), leaked, "journal lines with packet data")
    }

    private fun ticket() = OfdCommandRequest(
        kkmId = "kkm-log", ofdProviderId = "KAZAKHTELECOM", ofdEnvironmentId = "PROD",
        commandType = OfdCommandType.TICKET, payloadRef = "doc-1", token = TOKEN, reqNum = 10, deviceId = 1L
    )

    private fun response(code: Int) = Json.parseToJsonElement(
        """{"header": {"token": $ISSUED_TOKEN, "reqNum": 10},
            "payload": {"result": {"resultCode": $code, "resultText": "$REFUSAL"},
                        "ticket": {"fiscalSign": "$FISCAL_SIGN"}}}"""
    ) as JsonObject

    private companion object {
        const val TOKEN = 3_900_000_123L
        const val ISSUED_TOKEN = 3_900_000_124L
        const val ITEM = "Қымыз сүті"
        const val CUSTOMER = "870412300415"
        const val FISCAL_SIGN = "778899001"
        const val REFUSAL = "Отказ по чеку покупателя"
        val PACKET = Json.parseToJsonElement(
            """{"header": {"token": $TOKEN, "reqNum": 10},
                "payload": {"ticket": {"items": [{"commodity": {"name": "$ITEM"}}],
                            "extensionOptions": {"customerIinOrBin": "$CUSTOMER"}}}}"""
        ) as JsonObject
    }
}
