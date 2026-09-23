package io.github.texport.superkassa.coredatabase

import androidx.room.Room
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.receipt.PaymentType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptItem
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptPayment
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptStoredPayload
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.coredatabase.api.openRoomStorage
import io.github.texport.superkassa.coredatabase.impl.db.SuperkassaAppDatabase
import java.io.File
import kotlin.io.path.createTempDirectory

/** База кассы в файле временного каталога: открывается, закрывается и правится сырым SQL. */
internal class RoomFile {
    private val dir: File = createTempDirectory("room-parity-").toFile()
    val path: String = File(dir, "kassa.db").absolutePath

    fun builder() = Room.databaseBuilder<SuperkassaAppDatabase>(name = path)

    /** Открывает базу, как касса при запуске, и закрывает после [block] — как при остановке. */
    fun <T> session(block: (StoragePort) -> T): T {
        val storage = openRoomStorage(builder())
        try {
            return block(storage.storagePort)
        } finally {
            storage.close()
        }
    }

    fun sql(block: (SQLiteConnection) -> Unit) {
        val connection = BundledSQLiteDriver().open(path)
        try {
            block(connection)
        } finally {
            connection.close()
        }
    }

    fun delete() {
        dir.deleteRecursively()
    }

    companion object {
        /** Чек из одной позиции на [tiyn] с оплатой наличными — как его хранит узел. */
        fun receipt(kkmId: String, operation: ReceiptOperationType, tiyn: Long, key: String) = ReceiptStoredPayload(
            kkmId = kkmId,
            operation = operation,
            items = listOf(ReceiptItem("Нан", "001", 1000, Money.fromTiyn(tiyn), Money.fromTiyn(tiyn), measureUnitCode = "796")),
            payments = listOf(ReceiptPayment(PaymentType.CASH, Money.fromTiyn(tiyn))),
            total = Money.fromTiyn(tiyn),
            taken = Money.fromTiyn(tiyn),
            idempotencyKey = key,
            operatorName = "Айгерім"
        )
    }
}
