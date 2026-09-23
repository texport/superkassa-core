package io.github.texport.superkassa.embedded.impl.delivery

/** Сетевой печати на iOS пока нет: отказ уходит каналу и становится отказом доставки. */
internal actual object RawSocket {
    actual fun send(host: String, port: Int, bytes: ByteArray): Unit =
        throw UnsupportedOperationException("Network printing is not implemented on iOS yet")
}
