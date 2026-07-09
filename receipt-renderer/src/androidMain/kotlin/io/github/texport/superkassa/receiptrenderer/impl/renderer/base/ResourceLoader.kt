package io.github.texport.superkassa.receiptrenderer.impl.renderer.base

internal actual object ResourceLoader {
    actual fun readText(path: String): String? {
        val stream = ResourceLoader::class.java.getResourceAsStream(path)
        if (stream == null) return null
        return stream.bufferedReader().use { it.readText() }
    }
}
