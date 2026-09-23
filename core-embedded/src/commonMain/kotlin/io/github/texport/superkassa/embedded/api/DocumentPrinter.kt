package io.github.texport.superkassa.embedded.api

/**
 * Принтер, установленный в системе.
 *
 * Там, где печати ещё нет, методы отказывают исключением
 * [UnsupportedOperationException], а не делают вид, что напечатали.
 */
interface DocumentPrinter {
    /** Имена принтеров системы. */
    fun printerNames(): List<String>

    /**
     * Печатает PDF, например полученный из `PrintApi.getPrintPdf`.
     *
     * @param pdf документ.
     * @param printerName имя принтера; `null` — принтер системы по умолчанию.
     * @throws IllegalStateException если такого принтера нет.
     */
    fun printPdf(pdf: ByteArray, printerName: String? = null)
}
