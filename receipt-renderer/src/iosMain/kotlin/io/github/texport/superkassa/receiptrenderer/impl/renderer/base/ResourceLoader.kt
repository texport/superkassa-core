package io.github.texport.superkassa.receiptrenderer.impl.renderer.base

import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.NSUTF8StringEncoding

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
internal actual object ResourceLoader {
    actual fun readText(path: String): String? {
        val cleanPath = path.removePrefix("/")
        
        val directory = if (cleanPath.contains("/")) cleanPath.substringBeforeLast("/") else null
        val fileName = cleanPath.substringAfterLast("/")
        
        val extension = fileName.substringAfterLast('.', "")
        val name = fileName.substringBeforeLast('.')

        val bundle = NSBundle.mainBundle
        val filePath = bundle.pathForResource(name, extension, directory) ?: return null
        return NSString.stringWithContentsOfFile(filePath, NSUTF8StringEncoding, null)
    }
}
