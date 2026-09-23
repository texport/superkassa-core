package io.github.texport.superkassa.receiptrenderer.impl.adapter

/**
 * Простейший PNG: серый, восемь бит на точку, deflate без сжатия.
 *
 * Для QR-кода чека этого хватает: картинка в несколько килобайт, а общий
 * код не тянет за собой библиотеку сжатия на каждую платформу.
 */
internal object PngWriter {
    private val SIGNATURE = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)
    private const val MAX_STORED_BLOCK = 65_535
    private const val BLOCK_HEADER = 5
    private const val ZLIB_METHOD: Byte = 0x78
    private const val ZLIB_FLAGS: Byte = 0x01
    private const val ZLIB_FRAME = 11
    private const val BIT_DEPTH = 8
    private const val GRAYSCALE = 0
    private const val ADLER_MOD = 65_521
    private const val CRC_POLY = 0xEDB88320.toInt()
    private const val BYTE = 0xFF

    private val crcTable = IntArray(256) { n ->
        var c = n
        repeat(8) { c = if (c and 1 != 0) CRC_POLY xor (c ushr 1) else c ushr 1 }
        c
    }

    fun grayscale(width: Int, height: Int, pixel: (Int, Int) -> Int): ByteArray {
        val raw = ByteArray(height * (width + 1))
        for (y in 0 until height) {
            val row = y * (width + 1)
            for (x in 0 until width) raw[row + 1 + x] = pixel(x, y).toByte()
        }
        val header = int(width) + int(height) + byteArrayOf(BIT_DEPTH.toByte(), GRAYSCALE.toByte(), 0, 0, 0)
        return SIGNATURE + chunk("IHDR", header) + chunk("IDAT", zlibStored(raw)) + chunk("IEND", ByteArray(0))
    }

    private fun zlibStored(data: ByteArray): ByteArray {
        val out = ArrayList<Byte>(data.size + data.size / MAX_STORED_BLOCK * BLOCK_HEADER + ZLIB_FRAME)
        out += ZLIB_METHOD
        out += ZLIB_FLAGS
        var offset = 0
        do {
            val length = minOf(MAX_STORED_BLOCK, data.size - offset)
            out += if (offset + length == data.size) 1 else 0
            out += (length and BYTE).toByte()
            out += (length ushr 8).toByte()
            out += (length.inv() and BYTE).toByte()
            out += ((length.inv() ushr 8) and BYTE).toByte()
            for (i in offset until offset + length) out += data[i]
            offset += length
        } while (offset < data.size)
        int(adler32(data)).forEach { out += it }
        return out.toByteArray()
    }

    private fun chunk(type: String, data: ByteArray): ByteArray {
        val typed = type.encodeToByteArray() + data
        return int(data.size) + typed + int(crc32(typed))
    }

    private fun crc32(bytes: ByteArray): Int {
        var c = -1
        for (b in bytes) c = crcTable[(c xor b.toInt()) and BYTE] xor (c ushr 8)
        return c.inv()
    }

    private fun adler32(bytes: ByteArray): Int {
        var a = 1
        var b = 0
        for (byte in bytes) {
            a = (a + (byte.toInt() and BYTE)) % ADLER_MOD
            b = (b + a) % ADLER_MOD
        }
        return (b shl 16) or a
    }

    private fun int(value: Int): ByteArray =
        byteArrayOf((value ushr 24).toByte(), (value ushr 16).toByte(), (value ushr 8).toByte(), value.toByte())
}
