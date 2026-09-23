package io.github.texport.superkassa.coredatabase.impl.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Приводит документы, записанные Room до версии 10, к записи узла.
 *
 * До версии 10 Room писал итог внесения, изъятия и чека целыми тенге,
 * тип чека — именем операции (`SELL`), а открытие смены — ждущим отправки.
 * Узел и нынешний Room пишут итог в тиынах, тип — `SALE`/`RETURN`,
 * открытие смены — внутренним. Без приведения пересчёт смены читал бы
 * старое внесение в сто раз меньшим, а изъятие упиралось бы в пустой ящик.
 *
 * Выполняется только на шаге 9 → 10: базы девятой версии записаны одним
 * Room, переноса из узла в них не было, и итоги в них — только тенге.
 * Итог чека берётся из сохранённого чека с тиынами; без него — тенге × 100.
 */
internal fun convertLegacyLedger(connection: SQLiteConnection) {
    connection.execSQL(
        "UPDATE fiscal_documents SET totalAmount = totalAmount * 100 " +
            "WHERE docType IN ('CASH_IN', 'CASH_OUT') AND totalAmount IS NOT NULL"
    )
    connection.execSQL(
        "UPDATE fiscal_documents SET totalAmount = CASE " +
            "WHEN json_valid(receiptPayloadJson) AND json_extract(receiptPayloadJson, '$.total.bills') IS NOT NULL " +
            "THEN json_extract(receiptPayloadJson, '$.total.bills') * 100 + " +
            "COALESCE(json_extract(receiptPayloadJson, '$.total.coins'), 0) " +
            "ELSE totalAmount * 100 END " +
            "WHERE docType IN ('SELL', 'SELL_RETURN', 'BUY', 'BUY_RETURN') AND totalAmount IS NOT NULL"
    )
    connection.execSQL("UPDATE fiscal_documents SET docType = 'SALE' WHERE docType = 'SELL'")
    connection.execSQL("UPDATE fiscal_documents SET docType = 'RETURN' WHERE docType = 'SELL_RETURN'")
    connection.execSQL(
        "UPDATE fiscal_documents SET ofdStatus = 'INTERNAL', totalAmount = 0 " +
            "WHERE docType = 'SHIFT_OPEN' AND ofdStatus = 'PENDING'"
    )
}
