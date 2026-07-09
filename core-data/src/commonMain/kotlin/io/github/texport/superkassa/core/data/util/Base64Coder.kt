package io.github.texport.superkassa.core.data.util

object Base64Coder {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    private val DECODE_TABLE = IntArray(256) { -1 }.apply {
        for (i in ALPHABET.indices) {
            this[ALPHABET[i].code] = i
        }
    }

    fun encode(src: ByteArray): String {
        val out = StringBuilder((src.size * 4 + 2) / 3)
        var i = 0
        while (i < src.size) {
            val b0 = src[i++].toInt() and 0xFF
            if (i < src.size) {
                val b1 = src[i++].toInt() and 0xFF
                if (i < src.size) {
                    val b2 = src[i++].toInt() and 0xFF
                    out.append(ALPHABET[b0 shr 2])
                    out.append(ALPHABET[((b0 and 3) shl 4) or (b1 shr 4)])
                    out.append(ALPHABET[((b1 and 0xF) shl 2) or (b2 shr 6)])
                    out.append(ALPHABET[b2 and 0x3F])
                } else {
                    out.append(ALPHABET[b0 shr 2])
                    out.append(ALPHABET[((b0 and 3) shl 4) or (b1 shr 4)])
                    out.append(ALPHABET[(b1 and 0xF) shl 2])
                    out.append('=')
                }
            } else {
                out.append(ALPHABET[b0 shr 2])
                out.append(ALPHABET[(b0 and 3) shl 4])
                out.append("==")
            }
        }
        return out.toString()
    }

    fun decode(src: String): ByteArray {
        val clean = src.filter { it != '=' && !it.isWhitespace() }
        val len = clean.length
        val outLen = (len * 3) / 4
        val out = ByteArray(outLen)
        var i = 0
        var j = 0
        while (i < len) {
            val char0 = clean[i++]
            val c0 = DECODE_TABLE[char0.code]
            require(c0 >= 0) { "Invalid Base64 character: $char0" }

            val char1 = if (i < len) clean[i++] else null
            val c1 = char1?.let { DECODE_TABLE[it.code] } ?: 0
            if (char1 != null) {
                require(c1 >= 0) { "Invalid Base64 character: $char1" }
            }

            val char2 = if (i < len) clean[i++] else null
            val c2 = char2?.let { DECODE_TABLE[it.code] } ?: 0
            if (char2 != null) {
                require(c2 >= 0) { "Invalid Base64 character: $char2" }
            }

            val char3 = if (i < len) clean[i++] else null
            val c3 = char3?.let { DECODE_TABLE[it.code] } ?: 0
            if (char3 != null) {
                require(c3 >= 0) { "Invalid Base64 character: $char3" }
            }

            val triple = (c0 shl 18) or (c1 shl 12) or (c2 shl 6) or c3
            if (j < outLen) out[j++] = (triple shr 16).toByte()
            if (j < outLen) out[j++] = (triple shr 8).toByte()
            if (j < outLen) out[j++] = triple.toByte()
        }
        return out
    }
}
