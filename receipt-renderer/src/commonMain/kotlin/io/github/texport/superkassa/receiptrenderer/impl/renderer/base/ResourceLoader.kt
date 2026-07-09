package io.github.texport.superkassa.receiptrenderer.impl.renderer.base

internal expect object ResourceLoader {
    fun readText(path: String): String?
}
