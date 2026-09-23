package io.github.texport.superkassa.embedded

import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.domain.api.model.common.TimeValidationResult
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmMode
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmState
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.integration.TimeValidatorPort
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptItemRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptPaymentRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptSellRequest
import io.github.texport.superkassa.embedded.api.SuperkassaConfig
import io.github.texport.superkassa.embedded.api.SuperkassaPlatform
import io.github.texport.superkassa.embedded.impl.EmbeddedSuperkassa
import kz.mybrain.network.OfdEndpoint
import kz.mybrain.network.OfdNetworkClient
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.Base64

/** Касса для проверок: каталог, ОФД без связи и часы, которые не ходят в сеть. */
internal object TestCashRegister {
    const val KKM_ID = "kkm-embedded-1"
    const val CASHIER_PIN = "4321"
    const val ADMIN_PIN = "8765"
    private const val TOKEN = 123_456_789L

    /** ОФД, до которого нет связи: каждое обращение — отказ соединения. */
    class UnreachableOfd : OfdNetworkClient {
        var calls = 0
            private set

        override suspend fun sendAndReceive(endpoint: OfdEndpoint, request: ByteArray): Result<ByteArray> {
            calls++
            return Result.failure(IOException("Connection refused"))
        }
    }

    private val trustedClock = object : TimeValidatorPort {
        override fun validate(clock: ClockPort) = TimeValidationResult(ok = true)
    }

    fun open(dir: File, ofd: OfdNetworkClient = UnreachableOfd()): EmbeddedSuperkassa = EmbeddedSuperkassa.open(
        platform = SuperkassaPlatform(dir.absolutePath),
        config = SuperkassaConfig(ofdProviderId = "KAZAKHTELECOM", ofdProtocolVersion = "203"),
        ofdTransport = ofd,
        timeGuard = trustedClock
    )

    /** Касса, зарегистрированная раньше: её записи кладутся в базу, как их оставила бы регистрация. */
    fun registerKkm(kassa: EmbeddedSuperkassa) {
        val now = System.currentTimeMillis()
        kassa.storage.createKkm(
            KkmInfo(
                id = KKM_ID,
                createdAt = now,
                updatedAt = now,
                mode = KkmMode.REGISTRATION.name,
                state = KkmState.ACTIVE.name,
                ofdProvider = "KAZAKHTELECOM:TEST",
                registrationNumber = "010101012345",
                factoryNumber = "KZT0000001",
                systemId = "100500",
                tokenEncryptedBase64 = Base64.getEncoder().encodeToString(TOKEN.toString().encodeToByteArray()),
                tokenUpdatedAt = now,
                name = "Касса у окна"
            )
        )
        kassa.storage.createUser(KKM_ID, "admin-1", "Айгерим", UserRole.ADMIN, sha256(ADMIN_PIN), now)
        kassa.storage.createUser(KKM_ID, "cashier-1", "Нурлан", UserRole.CASHIER, sha256(CASHIER_PIN), now)
    }

    /** Продажа на 1 500 ₸ наличными. */
    fun sale(key: String) = ReceiptSellRequest(
        idempotencyKey = key,
        items = listOf(
            ReceiptItemRequest(name = "Хлеб бородинский", price = Decimal.parse("500.00"), quantity = Decimal.parse("3"))
        ),
        payments = listOf(ReceiptPaymentRequest(type = "CASH", sum = Decimal.parse("1500.00")))
    )

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.encodeToByteArray()).joinToString("") { "%02x".format(it) }
}
