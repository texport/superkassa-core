package io.github.texport.superkassa.embedded.impl.delivery

import java.net.InetSocketAddress
import java.net.Socket

internal actual object RawSocket {
    private const val TIMEOUT_MS = 5000

    actual fun send(host: String, port: Int, bytes: ByteArray) {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), TIMEOUT_MS)
            socket.soTimeout = TIMEOUT_MS
            socket.getOutputStream().apply {
                write(bytes)
                flush()
            }
        }
    }
}
