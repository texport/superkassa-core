package io.github.texport.superkassa.receiptrenderer.impl.adapter

/**
 * Кодировщика QR на iOS пока нет: чек печатается со ссылкой текстом, без картинки.
 * Своим кодировщиком здесь станет CoreImage (`CIQRCodeGenerator`).
 */
internal actual fun qrModules(text: String): Array<BooleanArray>? = null
