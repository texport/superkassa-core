package io.github.texport.superkassa.testing.api.bfd

import kotlinx.coroutines.runBlocking
import kz.kazakhtelecom.proto.v203.CommandTypeEnum
import kz.kazakhtelecom.proto.v203.Request
import kz.kazakhtelecom.proto.v203.Response
import kz.mybrain.network.OfdEndpoint
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Правила токена и номера запроса, которыми БФД встречает пакеты кассы:
 * исправная касса нарушить их не может, поэтому пакеты собираются здесь вручную.
 */
class BfdLedgerRulesTest {
    private val bfd = FakeBfd()

    @Test
    fun `документ с выданным токеном и новым номером учитывается, и выдаётся новый токен`() {
        val (token, answer) = send(KASSA, FakeBfd.FIRST_TOKEN, 1, CommandTypeEnum.COMMAND_CLOSE_SHIFT)

        assertEquals(OK, answer.result.result_code)
        assertEquals(bfd.issuedToken(KASSA), token)
        assertNotEquals(FakeBfd.FIRST_TOKEN, token)
        assertEquals(1, bfd.countedDocuments.size)
    }

    @Test
    fun `повтор того же обмена возвращает прежний ответ и документ второй раз не учитывает`() {
        val first = send(KASSA, FakeBfd.FIRST_TOKEN, 1, CommandTypeEnum.COMMAND_CLOSE_SHIFT)

        val again = send(KASSA, FakeBfd.FIRST_TOKEN, 1, CommandTypeEnum.COMMAND_CLOSE_SHIFT)

        assertEquals(first, again)
        assertEquals(1, bfd.countedDocuments.size)
    }

    @Test
    fun `другая команда под номером прошлого обмена - отказ повтора`() {
        send(KASSA, FakeBfd.FIRST_TOKEN, 1, CommandTypeEnum.COMMAND_CLOSE_SHIFT)

        assertEquals(INVALID_RETRY, send(KASSA, FakeBfd.FIRST_TOKEN, 1, CommandTypeEnum.COMMAND_MONEY_PLACEMENT).second.result.result_code)
    }

    @Test
    fun `новый токен с номером прошлого обмена - отказ номера запроса`() {
        val (issued, _) = send(KASSA, FakeBfd.FIRST_TOKEN, 1, CommandTypeEnum.COMMAND_CLOSE_SHIFT)

        assertEquals(INVALID_REQUEST_NUMBER, send(KASSA, issued, 1, CommandTypeEnum.COMMAND_CLOSE_SHIFT).second.result.result_code)
    }

    @Test
    fun `чужой токен - отказ токена и для документа, и для служебной команды`() {
        val codes = listOf(CommandTypeEnum.COMMAND_TICKET, CommandTypeEnum.COMMAND_SYSTEM)
            .map { send(KASSA, STALE_TOKEN, 1, it).second.result.result_code }

        assertEquals(listOf(INVALID_TOKEN, INVALID_TOKEN), codes)
        assertEquals(0, bfd.countedDocuments.size)
    }

    @Test
    fun `служебная команда с выданным токеном отвечается и токен не меняет`() {
        val (token, answer) = send(KASSA, FakeBfd.FIRST_TOKEN, 1, CommandTypeEnum.COMMAND_INFO)

        assertEquals(FakeBfd.FIRST_TOKEN, token)
        assertEquals(bfd.kgdNumber(KASSA), answer.service?.reg_info?.kkm?.fns_kkm_id)
        assertEquals("123456789012", answer.service?.reg_info?.org?.inn)
    }

    @Test
    fun `у каждой кассы свой учёт`() {
        send(KASSA, FakeBfd.FIRST_TOKEN, 1, CommandTypeEnum.COMMAND_CLOSE_SHIFT)

        val (_, other) = send(OTHER_KASSA, FakeBfd.FIRST_TOKEN, 1, CommandTypeEnum.COMMAND_CLOSE_SHIFT)

        assertEquals(OK, other.result.result_code)
        assertEquals(listOf(KASSA, OTHER_KASSA), bfd.countedDocuments.map { it.kassa })
    }

    /** Токен из заголовка ответа и сам ответ. */
    private fun send(kassa: Long, token: Long, reqNum: Int, command: CommandTypeEnum): Pair<Long, Response> {
        val body = Request.ADAPTER.encode(Request(command = command))
        val header = ByteBuffer.allocate(HEADER).order(ByteOrder.LITTLE_ENDIAN)
            .putShort(APPCODE).putShort(VERSION).putInt(HEADER + body.size)
            .putInt(kassa.toInt()).putInt(token.toInt()).putShort(reqNum.toShort()).array()
        val reply = runBlocking { bfd.sendAndReceive(ENDPOINT, header + body) }.getOrThrow()
        val replyToken = ByteBuffer.wrap(reply, TOKEN_OFFSET, Int.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).int.toUInt().toLong()
        return replyToken to Response.ADAPTER.decode(reply.copyOfRange(HEADER, reply.size))
    }

    private companion object {
        const val KASSA = 100_500L
        const val OTHER_KASSA = 100_501L
        const val STALE_TOKEN = 42L
        const val HEADER = 18
        const val TOKEN_OFFSET = 12
        const val APPCODE: Short = 0x81A2.toShort()
        const val VERSION: Short = 203
        const val OK = 0
        const val INVALID_TOKEN = 2
        const val INVALID_REQUEST_NUMBER = 8
        const val INVALID_RETRY = 9
        val ENDPOINT = OfdEndpoint("bfd.test", 7777)
    }
}
