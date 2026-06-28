package kz.mybrain.superkassa.core.domain.port

/**
 * Порт для генерации графических QR-кодов.
 * Используется для кодирования фискального признака и ссылки для проверки чека.
 */
@Suppress("unused")
interface QrCodeGeneratorPort {

    /**
     * Генерирует QR-код и возвращает его в формате PNG Data URI (Base64 строка).
     *
     * @param text текстовые данные, кодируемые в QR-код (например, URL-ссылка проверки чека).
     * @param sizePx размер стороны изображения QR-кода в пикселях.
     * @return строка в формате Data URI (например, "data:image/png;base64,...") или `null` в случае ошибки генерации.
     */
    fun generatePngDataUri(text: String, sizePx: Int = 180): String?
}
