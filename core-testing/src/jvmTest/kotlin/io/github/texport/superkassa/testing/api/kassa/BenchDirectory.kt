package io.github.texport.superkassa.testing.api.kassa

import io.github.texport.superkassa.embedded.api.SuperkassaPlatform
import io.github.texport.superkassa.testing.api.bfd.FakeBfd
import java.io.File
import kotlin.io.path.createTempDirectory

/** Каталог данных проверки: стенд на нём открывается и заново, а после проверки каталог удаляется. */
internal class BenchDirectory : AutoCloseable {
    val dir: File = createTempDirectory("bench-").toFile()
    private val platform = SuperkassaPlatform(dir.absolutePath)

    /** Стенд с новым БФД, как у потребителя, открывающего стенд впервые. */
    fun open(): TestBench = TestBench.open(platform)

    /** Стенд заново на том же каталоге и с тем же БФД, что до перезапуска. */
    fun reopen(bfd: FakeBfd): TestBench = TestBench.open(platform, bfd)

    override fun close() {
        dir.deleteRecursively()
    }

    companion object {
        const val ADMIN_PIN = "7391"
        const val CASHIER_PIN = "4826"

        val NOT_PAYER = KassaSetup(adminPin = ADMIN_PIN, cashierPin = CASHIER_PIN)
    }
}
