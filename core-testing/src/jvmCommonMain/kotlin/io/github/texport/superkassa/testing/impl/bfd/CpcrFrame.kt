package io.github.texport.superkassa.testing.impl.bfd

import kz.kazakhtelecom.proto.v203.Request
import kz.kazakhtelecom.proto.v203.Response

/**
 * Пакет CPCR, как его видит БФД: заголовок и разобранное тело запроса.
 *
 * Заголовок: APPCODE 2, VERSION 2, SIZE 4, ID 4, TOKEN 4, REQNUM 2 байта, little-endian.
 *
 * @property kassa номер кассы в БФД из поля ID.
 */
internal class CpcrFrame private constructor(
    private val header: ByteArray,
    val kassa: Long,
    val token: Long,
    val reqNum: Int,
    val request: Request
) {
    /** Ответ с заголовком запроса, токеном БФД и общей длиной ответа. */
    fun reply(token: Long, answer: Response): ByteArray {
        val payload = Response.ADAPTER.encode(answer)
        val replyHeader = header.copyOf()
        write(replyHeader, SIZE_OFFSET, SIZE_BYTES, (HEADER_SIZE + payload.size).toLong())
        write(replyHeader, TOKEN_OFFSET, TOKEN_BYTES, token)
        return replyHeader + payload
    }

    companion object {
        private const val HEADER_SIZE = 18
        private const val SIZE_OFFSET = 4
        private const val SIZE_BYTES = 4
        private const val ID_OFFSET = 8
        private const val ID_BYTES = 4
        private const val TOKEN_OFFSET = 12
        private const val TOKEN_BYTES = 4
        private const val REQNUM_OFFSET = 16
        private const val REQNUM_BYTES = 2
        private const val BYTE_BITS = 8
        private const val BYTE_MASK = 0xFFL

        /** Разбирает пакет кассы. */
        fun parse(bytes: ByteArray) = CpcrFrame(
            header = bytes.copyOf(HEADER_SIZE),
            kassa = read(bytes, ID_OFFSET, ID_BYTES),
            token = read(bytes, TOKEN_OFFSET, TOKEN_BYTES),
            reqNum = read(bytes, REQNUM_OFFSET, REQNUM_BYTES).toInt(),
            request = Request.ADAPTER.decode(bytes.copyOfRange(HEADER_SIZE, bytes.size))
        )

        private fun read(bytes: ByteArray, offset: Int, length: Int): Long {
            var value = 0L
            for (i in 0 until length) value = value or ((bytes[offset + i].toLong() and BYTE_MASK) shl (BYTE_BITS * i))
            return value
        }

        private fun write(bytes: ByteArray, offset: Int, length: Int, value: Long) {
            for (i in 0 until length) bytes[offset + i] = (value shr (BYTE_BITS * i)).toByte()
        }
    }
}
