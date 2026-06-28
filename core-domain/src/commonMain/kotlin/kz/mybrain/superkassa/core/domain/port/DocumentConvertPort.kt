package kz.mybrain.superkassa.core.domain.port

/**
 * Порт конвертации документов из формата HTML в различные бинарные и специализированные форматы.
 * Используется для подготовки чеков и отчётов к печати, отправке или сохранению.
 */
interface DocumentConvertPort {

    /**
     * Конвертирует HTML-разметку документа в документ формата PDF.
     *
     * @param html исходная строка с разметкой HTML.
     * @return массив байтов (ByteArray), представляющий сгенерированный PDF-документ.
     */
    fun htmlToPdf(html: String): ByteArray

    /**
     * Конвертирует HTML-разметку документа в растровое графическое изображение (PNG).
     *
     * @param html исходная строка с разметкой HTML.
     * @return массив байтов (ByteArray) с графическими данными в формате PNG.
     */
    fun htmlToImage(html: String): ByteArray

    /**
     * Конвертирует HTML-разметку документа в низкоуровневые ESC/POS команды для термопечати.
     *
     * @param html исходная строка с разметкой HTML.
     * @param paperWidthMm ширина чековой ленты в миллиметрах (например, 58 или 80).
     * @return массив байтов (ByteArray) команд принтера ESC/POS.
     */
    fun htmlToEscPos(html: String, paperWidthMm: Int): ByteArray
}
