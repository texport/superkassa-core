package io.github.texport.superkassa.core.data.impl.util

object Crc32 {
    private const val CRC_POLY = -306674912 // 0xEDB88320 as signed 32-bit int
    private const val BYTE_MASK = 0xFF
    private const val CRC_MASK = 0xFFFFFFFFL
    private const val INITIAL_CRC = -1 // 0xFFFFFFFF as signed 32-bit int
    private const val SHIFT_8 = 8

    private val TABLE = IntArray(256) { i ->
        var entry = i
        repeat(8) {
            entry = if (entry and 1 != 0) {
                (entry ushr 1) xor CRC_POLY
            } else {
                entry ushr 1
            }
        }
        entry
    }

    fun calculate(bytes: ByteArray): Long {
        var crc = INITIAL_CRC
        for (b in bytes) {
            val index = (crc xor b.toInt()) and BYTE_MASK
            crc = (crc ushr SHIFT_8) xor TABLE[index]
        }
        return (crc.inv().toLong()) and CRC_MASK
    }
}
