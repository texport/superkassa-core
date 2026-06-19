package kz.mybrain.superkassa.core.domain.port

/**
 * Порт для генерации QR-кодов.
 */
interface QrCodeGeneratorPort {
    /**
     * Генерирует QR-код в формате PNG Data URI.
     */
    fun generatePngDataUri(text: String, sizePx: Int = 180): String?
}
