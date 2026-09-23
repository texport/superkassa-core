package io.github.texport.superkassa.coredatabase

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptBranding
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptStoredPayload
import io.github.texport.superkassa.coredatabase.RoomFile.Companion.receipt
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Базы прежних версий поднимаются до нынешней без потери и в записи узла:
 * документы девятой версии — в тиынах и с типами узла, касса десятой —
 * с оформлением по умолчанию, которое дальше хранится.
 */
class LedgerMigrationTest {
    private val room = RoomFile()

    @AfterTest
    fun cleanUp() = room.delete()

    @Test
    fun `документы девятой версии приводятся к записи узла`() {
        room.session { }
        room.sql { connection ->
            document(connection, "in", "CASH_IN", total = 1234, status = "SENT")
            document(connection, "sale", "SELL", total = 1500, status = "SENT", payload = payload(150_050))
            document(connection, "sale-bare", "SELL", total = 700, status = "SENT")
            document(connection, "open", "SHIFT_OPEN", total = null, status = "PENDING")
            downgrade(connection, version = 9)
        }

        val read = room.session { storage ->
            listOf("in", "sale", "sale-bare", "open").map { id ->
                storage.findFiscalDocumentById(id).let { Triple(it?.docType, it?.totalAmount, it?.ofdStatus) }
            }
        }

        val expected = listOf(
            Triple("CASH_IN", 123_400L, "SENT"),
            Triple("SALE", 150_050L, "SENT"),
            Triple("SALE", 70_000L, "SENT"),
            Triple("SHIFT_OPEN", 0L, "INTERNAL")
        )
        assertEquals(expected, read)
    }

    @Test
    fun `касса десятой версии получает оформление по умолчанию и дальше его хранит`() {
        room.session { }
        room.sql { connection ->
            connection.execSQL(
                "INSERT INTO kkms (id, state, mode, autoCloseShift, autoCashout, createdAt, updatedAt) " +
                    "VALUES ('kkm-old', 'ACTIVE', 'REGISTRATION', 0, 0, 1, 1)"
            )
            connection.execSQL("ALTER TABLE kkms DROP COLUMN brandingJson")
            connection.execSQL("DROP TABLE pin_attempts")
            connection.execSQL("PRAGMA user_version = 10")
        }

        assertEquals(ReceiptBranding(), room.session { it.findKkm("kkm-old")?.branding })
        room.session { storage ->
            val kkm = checkNotNull(storage.findKkm("kkm-old"))
            storage.updateKkm(kkm.copy(branding = ReceiptBranding(headerMsg = "Рахмет!")))
        }
        assertEquals("Рахмет!", room.session { it.findKkm("kkm-old")?.branding?.headerMsg })
    }

    /** Документ, как его писал Room девятой версии: итог целыми тенге, тип — имя операции. */
    private fun document(connection: SQLiteConnection, id: String, type: String, total: Long?, status: String, payload: String? = null) {
        val statement = connection.prepare(
            "INSERT INTO fiscal_documents (id, cashboxId, shiftId, docType, docNo, createdAt, totalAmount, currency, " +
                "isAutonomous, ofdStatus, receiptPayloadJson) VALUES (?, 'kkm-1', 's-1', ?, 1, 1, ?, 'KZT', 0, ?, ?)"
        )
        try {
            statement.bindText(1, id)
            statement.bindText(2, type)
            if (total == null) statement.bindNull(3) else statement.bindLong(3, total)
            statement.bindText(4, status)
            if (payload == null) statement.bindNull(5) else statement.bindText(5, payload)
            statement.step()
        } finally {
            statement.close()
        }
    }

    private fun payload(tiyn: Long): String =
        Json.encodeToString(ReceiptStoredPayload.serializer(), receipt("kkm-1", ReceiptOperationType.SELL, tiyn, "k-1"))

    /** Схема девятой версии: без ключей повтора, ссылки на чек, оформления и счёта пинов. */
    private fun downgrade(connection: SQLiteConnection, version: Int) {
        connection.execSQL("DROP TABLE idempotency_keys")
        connection.execSQL("ALTER TABLE fiscal_documents DROP COLUMN receiptUrl")
        connection.execSQL("ALTER TABLE kkms DROP COLUMN brandingJson")
        connection.execSQL("DROP TABLE pin_attempts")
        connection.execSQL("PRAGMA user_version = $version")
    }
}
