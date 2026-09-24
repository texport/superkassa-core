package io.github.texport.superkassa.testing.impl.bfd

import kz.kazakhtelecom.proto.v203.Request
import kz.kazakhtelecom.proto.v203.Response
import kz.bfd.proto.v204.Request as Request204
import kz.bfd.proto.v204.Response as Response204

/**
 * Пакет CPCR, как его видит БФД: заголовок и разобранное тело запроса.
 *
 * Заголовок: APPCODE 2, VERSION 2, SIZE 4, ID 4, TOKEN 4, REQNUM 2 байта, little-endian.
 *
 * Обслуживаются версии 2.0.3 и 2.0.4. Запрос на 2.0.4 разбирается по 2.0.4 —
 * негодный по ней пакет не принимается, — а учёт ведётся по общей с 2.0.3
 * части: провод 2.0.4 её не меняет, новые поля 2.0.4 остаются в теле
 * нетронутыми. Ответ уходит в версии запроса.
 *
 * @property version версия протокола из поля VERSION: 203 или 204.
 * @property kassa номер кассы в БФД из поля ID.
 */
internal class CpcrFrame private constructor(
    private val header: ByteArray,
    val version: Int,
    val kassa: Long,
    val token: Long,
    val reqNum: Int,
    val request: Request
) {
    /** Ответ с заголовком запроса, токеном БФД и общей длиной ответа. */
    fun reply(token: Long, answer: Response): ByteArray {
        val encoded = Response.ADAPTER.encode(answer)
        val payload = if (version == V204) Response204.ADAPTER.run { encode(decode(encoded)) } else encoded
        val replyHeader = header.copyOf()
        write(replyHeader, SIZE_OFFSET, SIZE_BYTES, (HEADER_SIZE + payload.size).toLong())
        write(replyHeader, TOKEN_OFFSET, TOKEN_BYTES, token)
        return replyHeader + payload
    }

    companion object {
        private const val V203 = 203
        private const val V204 = 204
        private const val VERSION_OFFSET = 2
        private const val VERSION_BYTES = 2
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

        /**
         * Разбирает пакет кассы.
         *
         * @throws IllegalArgumentException если версия протокола — не 2.0.3 и не 2.0.4:
         *   стенд её не обслуживает, и касса настроена не на него.
         */
        fun parse(bytes: ByteArray): CpcrFrame {
            val version = read(bytes, VERSION_OFFSET, VERSION_BYTES).toInt()
            require(version == V203 || version == V204) { "CPCR version $version is not served by the test BFD" }
            val body = bytes.copyOfRange(HEADER_SIZE, bytes.size)
            if (version == V204) Request204.ADAPTER.decode(body)
            return CpcrFrame(
                header = bytes.copyOf(HEADER_SIZE),
                version = version,
                kassa = read(bytes, ID_OFFSET, ID_BYTES),
                token = read(bytes, TOKEN_OFFSET, TOKEN_BYTES),
                reqNum = read(bytes, REQNUM_OFFSET, REQNUM_BYTES).toInt(),
                request = Request.ADAPTER.decode(body)
            )
        }

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
