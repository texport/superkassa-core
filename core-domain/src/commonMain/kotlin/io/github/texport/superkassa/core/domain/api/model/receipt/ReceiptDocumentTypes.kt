package io.github.texport.superkassa.core.domain.api.model.receipt

/**
 * Типы фискальных документов, за которыми стоит чек с сохранённым содержимым.
 *
 * Тип документа в журнале называет саму операцию: продажа, возврат продажи,
 * покупка, возврат покупки. Общий «CHECK» на все четыре в журнале уже не
 * пишется, но остаётся в записях, оформленных прежними версиями, и читать
 * их нужно наравне с новыми.
 *
 * Набор объявлен один на всех: и хранилище, которое ищет содержимое чека,
 * и пересчёт счётчиков смены должны понимать под чеком одно и то же.
 * Разошедшиеся списки означают документ, которого одна половина узла
 * не видит: чек не уходит в ОФД, а X-отчёт считает кассу пустой.
 */
object ReceiptDocumentTypes {

    /** Тип документа для операции чека. */
    fun of(operation: ReceiptOperationType): String = when (operation) {
        ReceiptOperationType.SELL -> SALE
        ReceiptOperationType.SELL_RETURN -> RETURN
        ReceiptOperationType.BUY -> BUY
        ReceiptOperationType.BUY_RETURN -> BUY_RETURN
    }

    /** Продажа. */
    const val SALE = "SALE"

    /** Возврат продажи. */
    const val RETURN = "RETURN"

    /** Покупка. */
    const val BUY = "BUY"

    /** Возврат покупки. */
    const val BUY_RETURN = "BUY_RETURN"

    /**
     * X-отчёт в журнале.
     *
     * Так его называет справочник узла `/dictionaries/document-types`.
     * Прежние версии писали «REPORT_X», и это имя осталось у протокольного
     * поля отчёта; путать их нельзя: одно видит кассир, другое уходит в ОФД.
     */
    const val X_REPORT = "X_REPORT"

    /** X-отчёт, как его писали прежние версии узла. */
    const val LEGACY_X_REPORT = "REPORT_X"

    /**
     * Имя типа, каким его знает справочник узла.
     *
     * Документы, записанные прежними версиями, лежат в журнале под старым
     * именем, и клиент показывал по ним голый код вместо названия. Приводим
     * имя на чтении: переписывать историю в базе ради подписи не нужно.
     */
    fun canonical(docType: String): String =
        if (docType == LEGACY_X_REPORT) X_REPORT else docType

    /**
     * Все типы документов-чеков, включая написанные прежними версиями узла.
     */
    val ALL: Set<String> = setOf(
        SALE,
        RETURN,
        BUY,
        BUY_RETURN,
        // Исторические названия, встречающиеся в уже накопленных журналах.
        "CHECK",
        "SELL",
        "SELL_RETURN",
        "TICKET",
        "RECEIPT"
    )
}
