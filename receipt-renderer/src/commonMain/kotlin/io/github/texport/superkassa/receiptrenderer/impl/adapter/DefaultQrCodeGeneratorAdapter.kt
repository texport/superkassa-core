package io.github.texport.superkassa.receiptrenderer.impl.adapter

import io.github.texport.superkassa.core.domain.api.port.integration.QrCodeGeneratorPort
import kotlin.io.encoding.Base64

/**
 * QR-код ссылки на чек в ОФД, картинкой PNG.
 *
 * Матрицу строит настоящий кодировщик QR (уровень коррекции M, UTF-8),
 * картинку — [PngWriter]. Прежде здесь стояла заглушка: три угловых
 * метки и шум от хеша ссылки. Такой «код» выглядел как QR, но ничем
 * не читался, и покупатель не мог проверить чек. PNG, а не SVG: его
 * рисует и браузер, и WebView, и движок PDF без браузера.
 *
 * Там, где кодировщика нет, возвращается `null` — чек печатается со ссылкой
 * текстом и без картинки, а не с кодом, который никуда не ведёт.
 */
class DefaultQrCodeGeneratorAdapter : QrCodeGeneratorPort {

    override fun generatePngDataUri(text: String, sizePx: Int): String? {
        if (text.isBlank()) return null
        val modules = qrModules(text) ?: return null
        val side = modules.size + 2 * QUIET_ZONE
        val scale = (sizePx / side).coerceAtLeast(1)
        val png = PngWriter.grayscale(side * scale, side * scale) { x, y ->
            val row = y / scale - QUIET_ZONE
            val column = x / scale - QUIET_ZONE
            val dark = row in modules.indices && column in modules.indices && modules[row][column]
            if (dark) BLACK else WHITE
        }
        return "data:image/png;base64," + Base64.encode(png)
    }

    private companion object {
        const val QUIET_ZONE = 2
        const val BLACK = 0
        const val WHITE = 255
    }
}

/**
 * Модули QR-кода: `true` — тёмный. Строки сверху вниз, модули слева направо.
 *
 * @return матрица либо `null`, если на платформе нет кодировщика.
 */
internal expect fun qrModules(text: String): Array<BooleanArray>?
